package cc.microblock.hook

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.text.InputType
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import cc.ioctl.util.HookUtils
import com.github.kyuubiran.ezxhelper.utils.ArgTypes
import com.github.kyuubiran.ezxhelper.utils.Args
import com.github.kyuubiran.ezxhelper.utils.hookAllConstructorAfter
import com.github.kyuubiran.ezxhelper.utils.newInstance
import io.github.qauxv.R
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Log
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import xyz.nextalone.util.get
import xyz.nextalone.util.invoke
import xyz.nextalone.util.method
import xyz.nextalone.util.set
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.Executors


abstract class ExtraEmoticon {
//    abstract fun emoticonId(): String
//    abstract fun emoticonName(): String
    abstract fun QQEmoticonObject(): Any
}

abstract class ExtraEmoticonPanel {
    abstract fun emoticons(): List<ExtraEmoticon>
    abstract fun emoticonPanelIconURL(): String?
    abstract fun uniqueId(): String;
}

abstract class ExtraEmoticonProvider {
    abstract fun extraEmoticonList(): List<ExtraEmoticonPanel>
    abstract fun uniqueId(): String;
}

data class FileInfo(val name: String, val fullPath: String)

fun listDir(directoryPath: String): List<FileInfo> {
    return File(directoryPath).listFiles()?.map { FileInfo(it.name, it.absolutePath) } ?: listOf()
}

val executor = Executors.newFixedThreadPool(5)
val allowedExtensions = listOf(".png", ".jpg", ".jpeg", ".gif", ".webp");

class LocalDocumentEmoticonProvider : ExtraEmoticonProvider() {
    class Panel(val path: String, val id: String) : ExtraEmoticonPanel() {
        private var emoticons: List<ExtraEmoticon> = listOf();
        private var iconPath: String? = null;
        fun updateEmoticons() {
            var files = listDir(path);
            if(File(path + "/idList.txt.jpg").exists() && files.size == 0) {
                val reader = BufferedReader(InputStreamReader(File(path + "/idList.txt.jpg").inputStream()));
                val list = mutableListOf<FileInfo>();
                while(true) {
                    val line = reader.readLine() ?: break;
                    if(line.length == 0) continue;
                    var name =
                        if (File(path + "/" + line + "_high.webp").exists()) line + "_high.webp"
                        else if (File(path + "/" + line + "_high.webp").exists()) line + "_low.webp"
                        else if (File(path + "/" + line).exists()) line
                        else continue;
                    if (!list.any { it.name == name })
                        list.add(FileInfo(line, path + "/" + name));
                }
                files = list;
            }

            val emoticons = mutableListOf<ExtraEmoticon>();
            val FavoriteEmoticonInfo = Initiator.loadClass("com.tencent.mobileqq.emoticonview.FavoriteEmoticonInfo");
            for (file in files) {
                val filename = file.name;
                if(filename.startsWith("__cover__.")) {
                    iconPath = file.fullPath;
                    continue;
                }

                if(filename.endsWith(".nomedia")) continue;
                if(filename.endsWith(".txt.jpg")) continue;

                if(!allowedExtensions.contains(filename.substring(filename.lastIndexOf(".")))) continue;

                emoticons.add(object : ExtraEmoticon() {
                    val info = FavoriteEmoticonInfo.newInstance();
                    init {
                        info.set("path", file.fullPath);

                        // for recent use sorting
                        info.set("actionData", "${uniqueId()}:${file.fullPath}");
                    }
                    override fun QQEmoticonObject(): Any {
                        return info;
                    }
                })
            }
            this.emoticons = emoticons
            if (iconPath == null && files.size > 0) {
                iconPath = files[0].fullPath;
            }
        }
        init {
            executor.execute {
                updateEmoticons();
            }
        }
        var lastEmoticonUpdateTime = 0L;
        override fun emoticons(): List<ExtraEmoticon> {
            if(System.currentTimeMillis() - lastEmoticonUpdateTime > 1000 * 5) {
                lastEmoticonUpdateTime = System.currentTimeMillis();
                executor.execute {
                    updateEmoticons();
                }
            }
            return emoticons;
        }

        override fun emoticonPanelIconURL(): String? {
            return if(iconPath != null)  "file://$iconPath" else null;
        }

        override fun uniqueId(): String {
            return id;
        }
    }
    val panelsMap = mutableMapOf<String, Panel>();
    override fun extraEmoticonList(): List<ExtraEmoticonPanel> {
        val files = listDir("/storage/emulated/0/Documents/TGStickersExported/v1/");
        val panels = mutableListOf<ExtraEmoticonPanel>()
        for (fileInfo in files) {
            val file = fileInfo.fullPath;
            if (panelsMap.containsKey(file)) {
                panels.add(panelsMap[file]!!);
                continue;
            }
            if(file.endsWith(".nomedia")) continue;
            if(!File(file).isDirectory) continue;

            if (!panelsMap.containsKey(file)) {
                val panel = Panel(file, fileInfo.name);
                panelsMap[file] = panel;
            }

            panels.add(panelsMap[file]!!);
        }

        // restore last use sorting
        val db = ConfigManager.getDumpTG_LastUseEmoticonPackStore();
        panels.sortByDescending { db.getLongOrDefault(it.uniqueId(), 0) };

        return panels;
    }
    override fun uniqueId(): String {
        return "LocalDocumentEmoticonProvider";
    }
}


@FunctionHookEntry
@UiItemAgentEntry
object DumpTelegramStickers : CommonConfigFunctionHook() {
    override val name = "使用 Telegram Stickers 表情包集"
    override val description = "需配合 MicroBlock 的 Telegram 表情包同步插件使用";
    override val valueState: MutableStateFlow<String?> by lazy {
        MutableStateFlow(if (isEnabled) "已开启" else "已禁用")
    }

    var panelColumns: Number
        get() {
            val cfg = ConfigManager.getDefaultConfig()
            val summary = cfg.getString("dumpTGStickers.panelColumns")
            if(summary == null) return 4
            else return summary.toInt()
        }
        set(value) {
            val cfg = ConfigManager.getDefaultConfig()
            cfg.putString("dumpTGStickers.panelColumns", value.toString())
        }

    var removeQQEmoticons: Boolean
        get() {
            val cfg = ConfigManager.getDefaultConfig()
            return cfg.getString("dumpTGStickers.removeQQEmoticons") != "false"
        }
        set(value) {
            val cfg = ConfigManager.getDefaultConfig()
            cfg.putString("dumpTGStickers.removeQQEmoticons", if (value) "true" else "false")
        }

    var removeQQMisc: Boolean
        get() {
            val cfg = ConfigManager.getDefaultConfig()
            return cfg.getString("dumpTGStickers.removeQQMisc") != "false"
        }
        set(value) {
            val cfg = ConfigManager.getDefaultConfig()
            cfg.putString("dumpTGStickers.removeQQMisc", if (value) "true" else "false")
        }

    override val onUiItemClickListener: (IUiItemAgent, Activity, View) -> Unit = { _, ctx, _ ->
        val builder = AlertDialog.Builder(ctx)
        val wrapper = LinearLayout(ctx)
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )

        layoutParams.setMargins(30, 20, 30, 0)
        val root = LinearLayout(ctx)
        root.orientation = LinearLayout.VERTICAL

        wrapper.addView(root, layoutParams)

        val enable = CheckBox(ctx)
        enable.text = "启用"
        enable.isChecked = isEnabled

        val enable_removeQQEmoticons = CheckBox(ctx).apply {
            text = "移除 QQ 表情包"
            isChecked = removeQQEmoticons
        }

        val enable_removeQQMisc = CheckBox(ctx).apply {
            text = "移除 QQ 杂项"
            isChecked = removeQQMisc
        }

        val panelColumnsTextLabel = AppCompatTextView(ctx).apply {
            setText("表情包列数")
        }

        val panelColumnsTextEdit: EditText = AppCompatEditText(ctx).apply {
            setText(panelColumns.toString())
            inputType = InputType.TYPE_CLASS_NUMBER
            textSize = 16f
            setTextColor(ctx.resources.getColor(R.color.firstTextColor, ctx.theme))
            hint = "表情包列数"
        }

        // TODO: complete this
        val rangeTextLabel = AppCompatTextView(ctx).apply {
            setText("生效联系人列表（,分割）")
        }

        val rangeTextEdit: EditText = EditText(ctx).apply {
            setText("")
            textSize = 16f
            setTextColor(ctx.resources.getColor(R.color.firstTextColor, ctx.theme))
            hint = "114514, 1919810"
        }

        root.apply {
            addView(enable)
            addView(
                AppCompatTextView(ctx).apply {
                    setText("关于插件：需配合 MicroBlock 的 Telegram 表情包同步插件使用\n" +
                        "你也可以自行在 /storage/emulated/0/Documents/TGStickersExported/v1/ 下创建包含表情包图片文件的文件夹，然后在文件夹下idList.txt.jpg中写入表情包文件名（含拓展名，支持 png,jpg,webp,gif）列表，每行一个。")
                }
            )
            addView(
                AppCompatButton(ctx).apply {
                    setText("获取 Telegram 表情包同步插件")
                    setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/microblock_pub/159"));
                        ctx.startActivity(intent);
                    }
                }
            )
            addView(enable_removeQQEmoticons)
            addView(enable_removeQQMisc)
            addView(panelColumnsTextLabel)
            addView(panelColumnsTextEdit)
        }

        builder.setView(wrapper)
            .setTitle("Telegram Stickers 外部表情包设置")
            .setPositiveButton("确定") { dialog: DialogInterface?, which: Int ->
                this.isEnabled = enable.isChecked
                this.panelColumns = panelColumnsTextEdit.text.toString().toInt()
                this.removeQQEmoticons = enable_removeQQEmoticons.isChecked
                this.removeQQMisc = enable_removeQQMisc.isChecked

                valueState.update { if (isEnabled) "已开启" else "禁用" }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY;

    override fun initOnce(): Boolean {
        val EmoticonPanelController = Initiator.loadClass("com.tencent.mobileqq.emoticonview.EmoticonPanelController");
        val EmotionPanelViewPagerAdapter = Initiator.loadClass("com.tencent.mobileqq.emoticonview.EmotionPanelViewPagerAdapter");
        val EmoticonTabAdapter = Initiator.loadClass("com.tencent.mobileqq.emoticonview.EmoticonTabAdapter");
        val EmoticonPanelInfo = Initiator.loadClass("com.tencent.mobileqq.emoticonview.EmotionPanelInfo");
        val EmoticonPackage = Initiator.loadClass("com.tencent.mobileqq.data.EmoticonPackage");
        val FavoriteEmoticonInfo = Initiator.loadClass("com.tencent.mobileqq.emoticonview.FavoriteEmoticonInfo");

        // hook FavoriteEmoticonInfo.send for recent use sorting(local)
        HookUtils.hookBeforeIfEnabled(this, FavoriteEmoticonInfo.method("send")!!) {
            val actionData = it.thisObject.get<String>("actionData")!!;
            val panelId = actionData.substring(0, actionData.indexOf(":"));
            val fileId = actionData.substring(actionData.indexOf(":") + 1);
            Log.i("send $panelId $fileId")
            val panel = LocalDocumentEmoticonProvider().extraEmoticonList().find { it.uniqueId() == panelId };
            if(panel != null) {
                val db = ConfigManager.getDumpTG_LastUseEmoticonStore();
                db.set(fileId, System.currentTimeMillis());

                val db2 = ConfigManager.getDumpTG_LastUseEmoticonPackStore();
                db2.set(panelId, System.currentTimeMillis());
            }
        }

        var providers: List<ExtraEmoticonProvider> = listOf(LocalDocumentEmoticonProvider());

        if (!File("/storage/emulated/0/Download/QQ/TGStickersExported/.nomedia").exists()) {
            File("/storage/emulated/0/Download/QQ/TGStickersExported/").mkdirs();
            File("/storage/emulated/0/Download/QQ/TGStickersExported/.nomedia").createNewFile();
        }

        data class QAEpId (var providerId: String = "",var panelId: String);
        fun parseQAEpId(epId: String): QAEpId? {
            if (!epId.startsWith("qa:")) return null;
            val data = epId.substring(3);
            val providerId = data.substring(0, data.indexOf(":"));
            val panelId = data.substring(data.indexOf(":") + 1);
            return QAEpId(providerId, panelId)
        }

        var emoticonPanelViewAdapterInstance: Any? = null;

        EmotionPanelViewPagerAdapter.hookAllConstructorAfter {
            emoticonPanelViewAdapterInstance = it.thisObject;
        }

        // handleIPSite 只接受数字 epId，防崩
        HookUtils.hookBeforeIfEnabled(this,
        EmotionPanelViewPagerAdapter.method("handleIPSite")!!) {
            val pack = it.args[0];
            if(pack != null && parseQAEpId(pack.get<String>("epId")!!) != null) {
                it.args[0] = null;
            }
        }

        // 显示图标
        HookUtils.hookBeforeIfEnabled(this, EmoticonTabAdapter.method("generateTabUrl")!!) {
            val id = parseQAEpId(it.args[0] as String);

            if(id != null) {
                val provider = providers.find { it.uniqueId() == id.providerId };
                if(provider != null) {
                    val panel = provider.extraEmoticonList().find { it.uniqueId() == id.panelId };
                    if(panel != null) {
                        val url = panel.emoticonPanelIconURL();
                        if (url != null)
                            it.result = java.net.URL(url);
                        else
                            it.result = null;
                    }
                }
            }
        }

        var lastPanelDataSize = -1;
        // 生成 Tab 面板
        HookUtils.hookAfterIfEnabled(this, EmoticonPanelController.method("getPanelDataList")!!) {
            // 移除自带面板
            if(it.result == null) return@hookAfterIfEnabled;
            val list = it.result as MutableList<Any>;
            val iterator = list.iterator();

            var typeWhiteList = setOf(
//                    13, // 表情商城,
                18, // 搜索表情,
                7, // Emoji 表情,
                4, // 收藏表情,
//                    6, // 商店表情
//                    12, // GIF
//                    17, // QQ什么玩意专属表情
            );

            if(!removeQQMisc) {
                typeWhiteList += 13;
                typeWhiteList += 12;
                typeWhiteList += 17;
            }

            val existingIds = mutableSetOf<String>();

            while(iterator.hasNext()) {
                val element = iterator.next();

                if(!typeWhiteList.contains(element.get<Int>("type")!!)) {
                    if (element.get<Int>("type")!! == 6) {
                        val id = element.get("emotionPkg")!!.get("epId") as String;
                        if(!id.startsWith("qa:") && removeQQEmoticons) iterator.remove();
                        existingIds.add(id);
                    } else {
                        iterator.remove();
                    }
                }
                else {
                    if (element.get<Int>("type")!! == 6)
                        existingIds.add(element.get("emotionPkg")!!.get("epId") as String);
                }
            }

            var i = 3;
            // 添加自定义面板
            for(provider in providers) {
                for(panel in provider.extraEmoticonList()) {
                    i++;
                    val epid = "qa:${provider.uniqueId()}:${panel.uniqueId()}";
                    if(existingIds.contains(epid)) continue;
                    val pack = EmoticonPackage.newInstance();
                    pack.set("epId", epid);
                    pack.set("name", "QAExtraSticker");
                    pack.set("type", 3);
                    pack.set("ipJumpUrl", "https://github.com/cinit/QAuxiliary/");
                    pack.set("ipDetail", "QA");
                    pack.set("valid", true);
                    pack.set("status", 2);
                    pack.set("latestVersion", 1488377358);
                    pack.set("aio", true);

                    val info = EmoticonPanelInfo.newInstance(
                        Args(
                            arrayOf(
                                6, // type,
                                panelColumns, // columnNum,
                                pack // emotionPkg
                            )
                        ),
                        ArgTypes(
                            arrayOf(
                                Int::class.javaPrimitiveType!!,
                                Int::class.javaPrimitiveType!!,
                                EmoticonPackage
                            )
                        )
                    );

                    list.add(info!!);
                }
            }

            if(lastPanelDataSize != list.size) {
                lastPanelDataSize = list.size;
                emoticonPanelViewAdapterInstance?.invoke("notifyDataSetChanged");
            }

            it.result = list;
        }
        // 面板数据
        HookUtils.hookBeforeIfEnabled(this, EmotionPanelViewPagerAdapter.method("getEmotionPanelData")?:EmotionPanelViewPagerAdapter.method("queryEmoticonsByPackageIdFromDB")!!) {
            val pkg = it.args[2].get("emotionPkg") ?: return@hookBeforeIfEnabled;
            val epid = pkg.get("epId")?: return@hookBeforeIfEnabled;
            val id = parseQAEpId(epid as String);
            if(id != null) {
                val provider = providers.find { it.uniqueId() == id.providerId };
                if(provider != null) {
                    val panel = provider.extraEmoticonList().find { it.uniqueId() == id.panelId };
                    if(panel != null) {
                        val emoticons = panel.emoticons();
                        val list = mutableListOf<Any>();
                        for(emoticon in emoticons) {
                            list.add(emoticon.QQEmoticonObject());
                        }
                        it.result = list;
                    }
                }
            }
        }

        return true;
    }

    override val isAvailable: Boolean get() = requireMinQQVersion(QQVersion.QQ_9_0_8)
}
