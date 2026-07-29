/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2026 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is an opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this software.  If not, see
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package cc.ioctl.hook.msg

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import cc.hicore.QApp.QAppUtils
import com.xiaoniu.dispatcher.OnMenuBuilder
import com.xiaoniu.util.ContextUtils
import io.github.qauxv.R
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.CustomMenu
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.dexkit.AbstractQQCustomMenuItem
import io.github.qauxv.util.xpcompat.XC_MethodHook
import xyz.nextalone.util.invoke
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.concurrent.thread

/**
 * 长按图片消息对图片进行"小番茄"解混淆。
 *
 * 小番茄图片混淆基于 Gilbert(广义 Hilbert)空间填充曲线对像素位置进行置换,
 * 并使用黄金比例偏移 offset = round((sqrt(5)-1)/2 * W * H) 做循环移位。
 * 混淆: dst[curve[(i+offset)%n]] = src[curve[i]]; 解混淆为其逆运算: dst[curve[i]] = src[curve[(i+offset)%n]]。
 *
 * 功能入口与配置位置参考 [PicMd5Hook]。
 */
@FunctionHookEntry
@UiItemAgentEntry
object FanqieDeobfuscateHook : CommonSwitchFunctionHook(
    targets = arrayOf(AbstractQQCustomMenuItem)
), OnMenuBuilder {

    override val name = "小番茄解混淆"
    override val description = "长按图片消息点击\"小番茄解混淆\", 对图片进行小番茄(Gilbert 曲线)解混淆并保存到相册"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY
    override val isAvailable = QAppUtils.isQQnt()

    /** 与网页版工具一致, 限制约 800 万像素以避免内存溢出。 */
    private const val MAX_PIXELS = 8_000_000

    /** 黄金比例共轭 (sqrt(5)-1)/2, 用作沿曲线的固定偏移系数。 */
    private val PHI: Double = (Math.sqrt(5.0) - 1.0) / 2.0

    override fun initOnce(): Boolean = true

    override val targetComponentTypes = arrayOf(
        "com.tencent.mobileqq.aio.msglist.holder.component.pic.AIOPicContentComponent"
    )

    override fun onGetMenuNt(msg: Any, componentType: String, param: XC_MethodHook.MethodHookParam) {
        if (!isEnabled) return
        val item = CustomMenu.createItemIconNt(msg, "解混淆", R.drawable.ic_item_tool_72dp, R.id.item_fanqie_deobfuscate) {
            val activity = ContextUtils.getCurrentActivity()
            val context = CommonContextWrapper.createAppCompatContext(activity)
            val file = File(getFilePathNt(msg))
            if (!file.exists()) {
                Toasts.info(context, "请先查看原图后重试")
                return@createItemIconNt
            }
            Toasts.info(context, "正在解混淆...")
            thread {
                try {
                    val result = deobfuscate(file)
                    SyncUtils.runOnUiThread { showResultDialog(context, result) }
                } catch (oom: OutOfMemoryError) {
                    SyncUtils.runOnUiThread { Toasts.error(context, "图片过大, 内存不足") }
                } catch (e: Exception) {
                    traceError(e)
                    SyncUtils.runOnUiThread {
                        Toasts.error(context, "解混淆失败: ${e.message ?: e.javaClass.simpleName}")
                    }
                }
            }
        }
        @Suppress("UNCHECKED_CAST")
        (param.result as MutableList<Any>).add(item)
    }

    private fun getFilePathNt(message: Any): String {
        val msgClass = Initiator.loadClass("com.tencent.mobileqq.aio.msg.AIOMsgItem")
        val clazz = Initiator.load("com.tencent.qqnt.aio.msg.api.impl.AIOMsgItemApiImpl")!!
        return clazz.newInstance().invoke("getLocalPath", message, msgClass) as String
    }

    // ---------------- 小番茄 (Gilbert 曲线 + 黄金比例偏移) 解混淆 ----------------

    private fun deobfuscate(file: File): Bitmap {
        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val src = BitmapFactory.decodeFile(file.absolutePath, options)
            ?: throw IOException("无法解码图片")
        try {
            val w = src.width
            val h = src.height
            val n = w * h
            if (n <= 0) throw IOException("图片尺寸异常")
            if (n > MAX_PIXELS) throw IOException("图片过大 ($w×$h), 请使用更小尺寸的图片")
            val pixels = IntArray(n)
            src.getPixels(pixels, 0, w, 0, 0, w, h)
            val curve = gilbertCurve(w, h) // 每个元素为像素索引 x + y * w
            val offset = Math.round(PHI * n).toInt()
            val out = IntArray(n)
            for (i in 0 until n) {
                // 解混淆: dst[curve[i]] = src[curve[(i + offset) % n]]
                out[curve[i]] = pixels[curve[(i + offset) % n]]
            }
            val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            result.setPixels(out, 0, w, 0, 0, w, h)
            return result
        } finally {
            src.recycle()
        }
    }

    /** 生成 Gilbert(广义 Hilbert)空间填充曲线, 返回每个位置对应的像素索引 (x + y * w)。 */
    private fun gilbertCurve(w: Int, h: Int): IntArray {
        val curve = IntArray(w * h)
        val idx = intArrayOf(0)
        if (w >= h) {
            gen(0, 0, w, 0, 0, h, w, curve, idx)
        } else {
            gen(0, 0, 0, h, w, 0, w, curve, idx)
        }
        return curve
    }

    private fun gen(
        x: Int, y: Int, ax: Int, ay: Int, bx: Int, by: Int,
        imgW: Int, curve: IntArray, idx: IntArray
    ) {
        val w = Math.abs(ax + ay)
        val h = Math.abs(bx + by)
        val dax = sgn(ax)
        val day = sgn(ay)
        val dbx = sgn(bx)
        val dby = sgn(by)
        if (h == 1) {
            var xx = x
            var yy = y
            repeat(w) {
                curve[idx[0]++] = xx + yy * imgW
                xx += dax
                yy += day
            }
            return
        }
        if (w == 1) {
            var xx = x
            var yy = y
            repeat(h) {
                curve[idx[0]++] = xx + yy * imgW
                xx += dbx
                yy += dby
            }
            return
        }
        var ax2 = Math.floorDiv(ax, 2)
        var ay2 = Math.floorDiv(ay, 2)
        var bx2 = Math.floorDiv(bx, 2)
        var by2 = Math.floorDiv(by, 2)
        val w2 = Math.abs(ax2 + ay2)
        val h2 = Math.abs(bx2 + by2)
        if (2 * w > 3 * h) {
            if (w2 % 2 != 0 && w > 2) {
                ax2 += dax
                ay2 += day
            }
            gen(x, y, ax2, ay2, bx, by, imgW, curve, idx)
            gen(x + ax2, y + ay2, ax - ax2, ay - ay2, bx, by, imgW, curve, idx)
        } else {
            if (h2 % 2 != 0 && h > 2) {
                bx2 += dbx
                by2 += dby
            }
            gen(x, y, bx2, by2, ax2, ay2, imgW, curve, idx)
            gen(x + bx2, y + by2, ax, ay, bx - bx2, by - by2, imgW, curve, idx)
            gen(
                x + (ax - dax) + (bx2 - dbx),
                y + (ay - day) + (by2 - dby),
                -bx2, -by2, -(ax - ax2), -(ay - ay2),
                imgW, curve, idx
            )
        }
    }

    private fun sgn(v: Int) = if (v > 0) 1 else if (v < 0) -1 else 0

    // ---------------- 结果展示与保存 ----------------

    private fun showResultDialog(context: Context, bitmap: Bitmap) {
        val dm = context.resources.displayMetrics
        val maxW = (dm.widthPixels * 0.85).toInt()
        val maxH = (dm.heightPixels * 0.70).toInt()
        val imageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            maxWidth = maxW
            maxHeight = maxH
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setImageBitmap(bitmap)
        }
        AlertDialog.Builder(context)
            .setTitle("解混淆结果")
            .setView(imageView)
            .setPositiveButton("保存到相册") { _, _ -> saveBitmapToGallery(context, bitmap) }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun saveBitmapToGallery(context: Context, bitmap: Bitmap) {
        val name = "fanqie_deobf_${System.currentTimeMillis()}.png"
        thread {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, name)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/FanqieDeobf")
                    }
                    val uri = context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                    ) ?: throw IOException("无法创建媒体文件")
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                            throw IOException("写入失败")
                        }
                    } ?: throw IOException("无法打开输出流")
                } else {
                    @Suppress("DEPRECATION")
                    val dir = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "FanqieDeobf"
                    )
                    if (!dir.exists() && !dir.mkdirs()) throw IOException("无法创建目录")
                    val file = File(dir, name)
                    FileOutputStream(file).use { out ->
                        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                            throw IOException("写入失败")
                        }
                    }
                    @Suppress("DEPRECATION")
                    android.media.MediaScannerConnection.scanFile(
                        context, arrayOf(file.absolutePath), arrayOf("image/png"), null
                    )
                }
                SyncUtils.runOnUiThread {
                    Toasts.success(context, "已保存到相册 Pictures/FanqieDeobf")
                }
            } catch (e: Exception) {
                traceError(e)
                SyncUtils.runOnUiThread {
                    Toasts.error(context, "保存失败: ${e.message ?: e.javaClass.simpleName}")
                }
            }
        }
    }
}
