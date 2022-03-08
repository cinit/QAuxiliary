/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package me.ketal.hook

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.EditText
import androidx.core.content.FileProvider
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.invokeAs
import com.github.kyuubiran.ezxhelper.utils.tryOrFalse
import io.github.qauxv.R
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.CustomMenu
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Log
import xyz.nextalone.util.invoke
import xyz.nextalone.util.method
import java.io.File

@FunctionHookEntry
@UiItemAgentEntry
object PicCopyToClipboard : CommonSwitchFunctionHook() {
    override val name: String = "复制图片到剪贴板"

    override val description: String = "复制图片到剪贴板，可以在聊天窗口中粘贴使用"

    override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY

    override fun initOnce() = tryOrFalse {
        val clsPicItemBuilder = Initiator._PicItemBuilder()
        val clsBasePicItemBuilder = clsPicItemBuilder.superclass
        val clazz = arrayOf(clsPicItemBuilder, clsBasePicItemBuilder)
        // copy pic
        clazz.forEach {
            it.method { m ->
                m.name == "a"
                    && m.parameterTypes.contentEquals(arrayOf(Int::class.java, Context::class.java, Initiator._ChatMessage()))
            }?.hookBefore { m ->
                val id = m.args[0]
                val context = m.args[1] as Context
                val message = m.args[2]
                if (id != R.id.item_copyToClipboard) return@hookBefore
                m.result = null
                val path = arrayOf("chatraw", "chatimg", "chatthumb").map { str ->
                    message.invoke("getFilePath", str, String::class.java) as String
                }.first { path ->
                    // chosen the first exist file
                    File(path).exists()
                }
                // An error occurs when the host does not have a fileprovider
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(path))
                val item = ClipData.Item(uri)
                val clipData = ClipData("label", arrayOf("image/*"), item)
                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboardManager.setPrimaryClip(clipData)
            }
            it.method { m ->
                m.returnType.isArray
                    && m.parameterTypes.contentEquals(arrayOf(View::class.java))
            }?.hookAfter { param ->
                param.result = param.result.run {
                    this as Array<Any>
                    val clQQCustomMenuItem = javaClass.componentType
                    val itemCopy = CustomMenu.createItem(clQQCustomMenuItem, R.id.item_copyToClipboard, "复制")
                    plus(itemCopy)
                }
            }
        }

        // todo: paste pic
        /*
        val clsXEditText = loadClass("com.tencent.widget.XEditText")
        clsXEditText.method { m ->
            m.name == "onCreateInputConnection"
        }?.hookBefore {
            val editorInfo = it.args[0] as EditorInfo
            val editetxt = it.thisObject as EditText
            val ic: InputConnection = editetxt.onCreateInputConnection(editorInfo)
            EditorInfoCompat.setContentMimeTypes(editorInfo, arrayOf("image/*"))

            val callback =
                InputConnectionCompat.OnCommitContentListener { inputContentInfo, flags, opts ->
                    val lacksPermission = (flags and
                        InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0
                    // read and display inputContentInfo asynchronously
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && lacksPermission) {
                        try {
                            inputContentInfo.requestPermission()
                        } catch (e: Exception) {
                            return@OnCommitContentListener false // return false if failed
                        }
                    }
                    val uri = inputContentInfo.contentUri
                    Log.i("PicCopyToClipboard -> uri: $uri")
                    true  // return true if succeeded
                }
            it.result = InputConnectionCompat.createWrapper(ic, editorInfo, callback)
        }
        */
         */
    }
}
