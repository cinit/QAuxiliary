/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2026 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is an opensource software: you can redistribute it
 * and/or modify it under the terms of the General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the General Public License for more details.
 *
 * You should have received a copy of the General Public License
 * along with this software.
 * If not, see <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package io.github.nakixii.hook

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.SystemClock
import android.text.InputType
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.SwitchCompat
import cc.hicore.QApp.QAppUtils
import cc.ioctl.util.HostInfo
import cc.ioctl.util.LayoutHelper
import cc.ioctl.util.hookAfterIfEnabled
import cc.ioctl.util.hookBeforeIfEnabled
import com.tencent.qqnt.kernel.nativeinterface.MsgElement
import com.tencent.qqnt.kernel.nativeinterface.PicElement
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.core.HookInstaller
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.SafUtils
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.dexkit.BlockPicByMd5_EmotionPreviewDataV2
import io.github.qauxv.util.dexkit.BlockPicByMd5_LoadImagePathV2
import io.github.qauxv.util.dexkit.BlockPicByMd5_PicPathResolverV2
import io.github.qauxv.util.dexkit.DexKit
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.io.IOException
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@FunctionHookEntry
@UiItemAgentEntry
object BlockPicByMd5 : CommonConfigFunctionHook(
    hookKey = "nakixii.block_pic_by_md5",
    targets = arrayOf(
        BlockPicByMd5_LoadImagePathV2,
        BlockPicByMd5_PicPathResolverV2,
        BlockPicByMd5_EmotionPreviewDataV2,
    ),
    targetProc = SyncUtils.PROC_MAIN or SyncUtils.PROC_PEAK,
) {

    override val name = "屏蔽指定 MD5 图片"
    override val description = "接收指定 MD5 的图片时显示替换图"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY
    override val isAvailable: Boolean
        get() = QAppUtils.isQQnt()

    override val valueState = MutableStateFlow<String?>(null)
    override val onUiItemClickListener: (IUiItemAgent, Activity, View) -> Unit = { _, activity, _ ->
        if (HostInfo.isInModuleProcess()) {
            showHostOnlyDialog(activity)
        } else {
            reloadConfig()
            updateValueState()
            showRuleManager(activity)
        }
    }

    private const val CFG_KEY_RULES = "nakixii.block_pic_by_md5.rules"
    private const val CFG_KEY_FALLBACK_PATH = "nakixii.block_pic_by_md5.fallback_path"
    private const val CFG_KEY_RULE_PATH_PREFIX = "nakixii.block_pic_by_md5.rule."
    private const val CFG_KEY_REVISION = "nakixii.block_pic_by_md5.revision"
    private data class RuleConfig(
        val rules: Set<String>,
        val rulePaths: Map<String, String>,
        val fallbackPath: String?,
        val revision: Long,
    )

    @Volatile
    private var ruleConfig = RuleConfig(emptySet(), emptyMap(), null, Long.MIN_VALUE)

    @Volatile
    private var lastConfigCheckAt = 0L

    private val replacementPathByMd5 = ConcurrentHashMap<String, String>()
    private val messageMd5Fields = ConcurrentHashMap<Class<*>, Field>()

    init {
        reloadConfig()
        updateValueState()
    }

    override fun initOnce(): Boolean {
        reloadConfig()
        return listOf(::hookPicContentLoad, ::hookPicPathResolver, ::hookEmotionPreview)
            .map { runCatching(it).onFailure(::traceError).getOrDefault(false) }
            .any { it }
    }

    private fun hookPicContentLoad(): Boolean {
        val loadMethod = DexKit.requireMethodFromCache(BlockPicByMd5_LoadImagePathV2)
        val pathIndex = loadMethod.parameterTypes.indexOfFirst { it == String::class.java }
        val elementIndex = loadMethod.parameterTypes.indexOfFirst { it == MsgElement::class.java }
        if (pathIndex < 0 || elementIndex < 0) return false
        hookBeforeIfEnabled(loadMethod) { param ->
            maybeReloadConfig()
            val element = param.args.getOrNull(elementIndex) as? MsgElement ?: return@hookBeforeIfEnabled
            val replacement = element.picElement?.let(::getReplacementForPic) ?: return@hookBeforeIfEnabled
            param.args[pathIndex] = replacement
        }
        return true
    }

    private fun hookPicPathResolver(): Boolean {
        val originResolver = DexKit.requireMethodFromCache(BlockPicByMd5_PicPathResolverV2)
        val resolverMethods = originResolver.declaringClass.declaredMethods.filter { method ->
            Modifier.isStatic(method.modifiers) && method.returnType == String::class.java &&
                (method.parameterTypes.contentEquals(arrayOf(PicElement::class.java)) ||
                    method.parameterTypes.contentEquals(arrayOf(PicElement::class.java, Int::class.javaPrimitiveType)))
        }
        if (resolverMethods.isEmpty()) return false
        resolverMethods.forEach { method ->
            hookAfterIfEnabled(method) { param ->
                maybeReloadConfig()
                val picElement = param.args.firstOrNull() as? PicElement ?: return@hookAfterIfEnabled
                param.result = getReplacementForPic(picElement) ?: return@hookAfterIfEnabled
            }
        }
        return true
    }

    private fun hookEmotionPreview(): Boolean {
        val clazz = DexKit.requireClassFromCache(BlockPicByMd5_EmotionPreviewDataV2)
        val method = clazz.declaredMethods.firstOrNull {
            it.returnType.name == "android.graphics.drawable.Drawable" &&
                it.parameterTypes.contentEquals(arrayOf(Context::class.java))
        } ?: return false
        val picElementGetter = clazz.declaredMethods.firstOrNull {
            it.returnType == PicElement::class.java && it.parameterCount == 0
        }?.apply { isAccessible = true }
        val recordField = clazz.declaredFields.firstOrNull {
            it.type.name == "com.tencent.mobileqq.data.MessageRecord"
        }?.apply { isAccessible = true }
        hookAfterIfEnabled(method) { param ->
            maybeReloadConfig()
            val picElement = picElementGetter?.invoke(param.thisObject) as? PicElement
            val replacement = getReplacementForPic(picElement)
                ?: messageRecordMd5(recordField?.get(param.thisObject))?.let(::getReplacementPathByMd5)
                ?: return@hookAfterIfEnabled
            param.result = Drawable.createFromPath(replacement) ?: return@hookAfterIfEnabled
        }
        return true
    }

    private fun messageRecordMd5(record: Any?): String? {
        val value = record ?: return null
        val clazz = value.javaClass
        val field = messageMd5Fields[clazz] ?: generateSequence(clazz as Class<*>?) { it.superclass }
            .flatMap { it.declaredFields.asSequence() }
            .firstOrNull { it.name == "md5" && it.type == String::class.java }
            ?.apply { isAccessible = true }
            ?.also { messageMd5Fields.putIfAbsent(clazz, it) }
            ?: return null
        return normalizeMd5(field.get(value) as? String)
    }

    private fun getReplacementForPic(picElement: PicElement?): String? = picElement?.let {
        normalizeMd5(it.md5HexStr)?.let(::getReplacementPathByMd5)
            ?: normalizeMd5(it.originImageMd5)?.let(::getReplacementPathByMd5)
    }

    private fun getReplacementPathByMd5(md5: String): String? {
        val config = ruleConfig
        if (md5 !in config.rules) return null
        replacementPathByMd5[md5]?.let { return it.ifEmpty { null } }
        val replacement = config.rulePaths[md5]?.takeIf(::isUsableReplacementFile)
            ?: config.fallbackPath?.takeIf(::isUsableReplacementFile)
        replacementPathByMd5[md5] = replacement.orEmpty()
        return replacement
    }

    private fun isUsableReplacementFile(path: String): Boolean = File(path).let { it.isFile && it.canRead() }

    private fun reloadConfig(config: ConfigManager = ConfigManager.getDefaultConfig()) {
        val configuredRules = readRules(config)
        ruleConfig = RuleConfig(
            rules = configuredRules,
            rulePaths = configuredRules.mapNotNull { md5 ->
                config.getString(CFG_KEY_RULE_PATH_PREFIX + md5)?.takeIf(String::isNotEmpty)?.let { md5 to it }
            }.toMap(),
            fallbackPath = config.getString(CFG_KEY_FALLBACK_PATH)?.takeIf(String::isNotEmpty),
            revision = config.getLongOrDefault(CFG_KEY_REVISION, 0L),
        )
        lastConfigCheckAt = SystemClock.uptimeMillis()
        replacementPathByMd5.clear()
    }

    private fun readRules(config: ConfigManager): Set<String> =
        config.getStringSetOrDefault(CFG_KEY_RULES, emptySet()).mapNotNull(::normalizeMd5).toSet()

    private fun updateConfig(update: (ConfigManager) -> Unit) {
        val config = ConfigManager.getDefaultConfig()
        update(config)
        config.putLong(CFG_KEY_REVISION, config.getLongOrDefault(CFG_KEY_REVISION, 0L) + 1L)
        config.save()
        reloadConfig(config)
        updateValueState()
    }

    private fun maybeReloadConfig() {
        val now = SystemClock.uptimeMillis()
        if (now - lastConfigCheckAt < 1000L) return
        lastConfigCheckAt = now
        val config = ConfigManager.getDefaultConfig()
        if (config.getLongOrDefault(CFG_KEY_REVISION, 0L) != ruleConfig.revision) reloadConfig()
    }

    private fun updateValueState() {
        val ruleCount = ruleConfig.rules.size
        valueState.value = when {
            HostInfo.isInModuleProcess() -> "请在 QQ 内管理"
            !isEnabled -> "未启用"
            ruleCount == 0 -> "未添加规则"
            else -> "$ruleCount 条规则"
        }
    }

    private fun addRule(md5: String) = updateConfig {
        it.putStringSet(CFG_KEY_RULES, readRules(it) + md5)
    }

    private fun removeRule(md5: String) {
        updateConfig { config ->
            config.putStringSet(CFG_KEY_RULES, readRules(config) - md5)
            config.remove(CFG_KEY_RULE_PATH_PREFIX + md5)
        }
    }

    private fun setPath(key: String, path: String?) {
        updateConfig { config ->
            if (path == null) config.remove(key) else config.putString(key, path)
        }
    }

    private fun setBlockingEnabled(context: Context, enabled: Boolean) {
        isEnabled = enabled
        updateValueState()
        if (enabled && !isInitialized) {
            HookInstaller.initializeHookForeground(context, this)
        }
    }

    @JvmStatic
    fun addRuleAndShowConfig(context: Context, value: String) {
        if (!isAvailable) {
            Toasts.error(context, "仅支持 QQNT")
            return
        }
        val md5 = normalizeMd5(value)
        if (md5 == null) {
            Toasts.error(context, "获取图片 MD5 失败")
            return
        }
        addRule(md5)
        setBlockingEnabled(context, true)
        showRuleManager(context)
    }

    private fun showHostOnlyDialog(context: Context) {
        AlertDialog.Builder(CommonContextWrapper.createAppCompatContext(context))
            .setTitle(name)
            .setMessage("规则和替换图片由 QQ 进程保存。请在 QQ 内打开 QAuxiliary 设置，或在图片 MD5 菜单中选择“屏蔽图片”后管理规则。")
            .setPositiveButton("关闭", null)
            .show()
    }

    private fun showRuleManager(context: Context) {
        val (rules, rulePaths, fallbackPath) = ruleConfig
        val dialogContext = CommonContextWrapper.createAppCompatContext(context)
        val margin = LayoutHelper.dip2px(dialogContext, 16f)
        val itemMargin = LayoutHelper.dip2px(dialogContext, 4f)
        val itemParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = itemMargin }
        val content = LinearLayout(dialogContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(margin, margin, margin, margin)
        }
        content.addView(SwitchCompat(dialogContext).apply {
            text = "启用屏蔽"
            isChecked = this@BlockPicByMd5.isEnabled
            setOnCheckedChangeListener { _, enabled -> setBlockingEnabled(context, enabled) }
        }, itemParams)

        val fallbackConfigured = fallbackPath?.let(::isUsableReplacementFile) == true
        content.addView(AppCompatTextView(dialogContext).apply {
            text = if (fallbackConfigured) "默认替换图片：已设置" else "默认替换图片：未设置"
        }, itemParams)

        lateinit var dialog: AlertDialog
        content.addView(configButton(dialogContext, "选择默认替换图片") {
            dialog.dismiss()
            selectReplacementImage(context, CFG_KEY_FALLBACK_PATH)
        }, itemParams)
        if (fallbackPath != null) {
            content.addView(configButton(dialogContext, "清除默认替换图片") {
                setPath(CFG_KEY_FALLBACK_PATH, null)
                dialog.dismiss()
                showRuleManager(context)
            }, itemParams)
        }

        content.addView(configButton(dialogContext, "添加 MD5 规则") {
            dialog.dismiss()
            showAddRuleDialog(context)
        }, itemParams)
        content.addView(AppCompatTextView(dialogContext).apply {
            text = "已添加 ${rules.size} 条规则"
        }, itemParams)
        rules.sorted().forEach { md5 ->
            val rulePath = rulePaths[md5]
            val description = when {
                rulePath?.let(::isUsableReplacementFile) == true -> "专属替换图片"
                rulePath != null -> "专属替换图片不可用"
                fallbackConfigured -> "使用默认替换图片"
                else -> "未设置替换图片"
            }
            content.addView(configButton(dialogContext, "$md5\n$description") {
                dialog.dismiss()
                showRuleDialog(context, md5)
            }, itemParams)
        }

        dialog = AlertDialog.Builder(dialogContext)
            .setTitle("图片 MD5 屏蔽规则")
            .setView(ScrollView(dialogContext).apply { addView(content) })
            .setPositiveButton("关闭", null)
            .create()
        dialog.show()
    }

    private fun showAddRuleDialog(context: Context) {
        val dialogContext = CommonContextWrapper.createAppCompatContext(context)
        val margin = LayoutHelper.dip2px(dialogContext, 16f)
        val input = AppCompatEditText(dialogContext).apply {
            hint = "输入 32 位图片 MD5"
            inputType = InputType.TYPE_CLASS_TEXT
            isSingleLine = true
        }
        val wrapper = LinearLayout(dialogContext).apply {
            setPadding(margin, 0, margin, 0)
            addView(input, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
        val dialog = AlertDialog.Builder(dialogContext)
            .setTitle("添加 MD5 规则")
            .setView(wrapper)
            .setPositiveButton("添加", null)
            .setNegativeButton("取消", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val md5 = normalizeMd5(input.text.toString())
                if (md5 == null) {
                    input.error = "请输入 32 位十六进制 MD5"
                    return@setOnClickListener
                }
                addRule(md5)
                setBlockingEnabled(context, true)
                dialog.dismiss()
                showRuleManager(context)
            }
        }
        dialog.show()
    }

    private fun showRuleDialog(context: Context, md5: String) {
        val hasRulePath = ruleConfig.rulePaths[md5] != null
        val actions = if (hasRulePath) {
            arrayOf("选择专属替换图片", "恢复使用默认替换图片", "删除规则")
        } else {
            arrayOf("选择专属替换图片", "删除规则")
        }
        val dialogContext = CommonContextWrapper.createAppCompatContext(context)
        AlertDialog.Builder(dialogContext)
            .setTitle(md5)
            .setItems(actions) { _, which ->
                when {
                    which == 0 -> selectReplacementImage(context, CFG_KEY_RULE_PATH_PREFIX + md5)
                    hasRulePath && which == 1 -> {
                        setPath(CFG_KEY_RULE_PATH_PREFIX + md5, null)
                        showRuleManager(context)
                    }
                    else -> {
                        removeRule(md5)
                        showRuleManager(context)
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun selectReplacementImage(context: Context, configKey: String) {
        SafUtils.requestOpenFile(context)
            .setMimeType("image/*")
            .onResult { uri ->
                SyncUtils.async {
                    val path = runCatching { copyReplacementImage(context, uri) }
                    SyncUtils.runOnUiThread {
                        path.onSuccess {
                            setPath(configKey, it)
                            showRuleManager(context)
                            Toasts.success(context, "替换图片已设置")
                        }.onFailure {
                            traceError(it)
                            Toasts.error(context, "导入替换图片失败")
                        }
                    }
                }
            }
            .commit()
    }

    private fun copyReplacementImage(context: Context, uri: Uri): String {
        val directory = File(context.applicationContext.filesDir, "qa_misc/pic_md5_replace")
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("无法创建替换图片目录")
        }
        val fileName = "replace_${System.currentTimeMillis()}_${UUID.randomUUID()}"
        val temporary = File(directory, "$fileName.part")
        try {
            SafUtils.openInputStream(context, uri).use { input ->
                temporary.outputStream().use { output -> input.copyTo(output) }
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(temporary.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                throw IOException("无法解码替换图片")
            }
            val extension = when (bounds.outMimeType) {
                "image/png" -> "png"
                "image/jpeg" -> "jpg"
                "image/gif" -> "gif"
                "image/webp" -> "webp"
                else -> throw IOException("不支持的图片格式")
            }
            val target = File(directory, "$fileName.$extension")
            if (temporary.length() == 0L || !temporary.renameTo(target)) {
                throw IOException("无法保存替换图片")
            }
            return target.absolutePath
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }

    private fun configButton(context: Context, text: String, onClick: () -> Unit) =
        AppCompatButton(context).apply {
            this.text = text
            isAllCaps = false
            setOnClickListener { onClick() }
        }

    private fun normalizeMd5(value: String?): String? {
        val md5 = value?.trim()?.takeIf { it.length == 32 } ?: return null
        if (md5.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) return null
        return md5.uppercase(Locale.ROOT)
    }

}
