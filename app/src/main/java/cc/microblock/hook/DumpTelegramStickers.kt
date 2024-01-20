package cc.microblock.hook

import android.view.View
import android.widget.RelativeLayout
import cc.hicore.QApp.QAppUtils
import cc.ioctl.util.HookUtils
import cc.ioctl.util.Reflex
import com.github.kyuubiran.ezxhelper.utils.getObjectAs
import com.github.kyuubiran.ezxhelper.utils.hookAllConstructorAfter
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.hookReplace
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import xyz.nextalone.util.get
import xyz.nextalone.util.invoke
import xyz.nextalone.util.set

abstract class ExtraEmoticon {
//    abstract fun emoticonId(): String
//    abstract fun emoticonName(): String
    abstract fun QQEmoticonObject(): Any
}

abstract class ExtraEmoticonPanel {
    abstract fun emoticons(): List<ExtraEmoticon>
    abstract fun emoticonPanelIconURL(): String
    abstract fun uniqueId(): String;
}

abstract class ExtraEmoticonProvider {
    abstract fun extraEmoticonList(): List<ExtraEmoticonPanel>
    abstract fun uniqueId(): String;
}

fun listDir (dir: String): List<String> {
    val file = java.io.File(dir)
    val files = file.listFiles()
    val list = mutableListOf<String>()
    if (files != null) {
        for (f in files) {
            if (f.isDirectory) {
                list.addAll(listDir(f.absolutePath))
            } else {
                list.add(f.absolutePath)
            }
        }
    }
    return list
}

class LocalDocumentEmoticonProvider : ExtraEmoticonProvider() {
    class Panel(path: String) : ExtraEmoticonPanel() {
        var emoticons: List<ExtraEmoticon>;
        var iconPath: String? = null;
        init {
            val files = listDir(path);
            val emoticons = mutableListOf<ExtraEmoticon>();
            val FavoriteEmoticonInfo = Initiator.loadClass("com.tencent.mobileqq.emoticonview.FavoriteEmoticonInfo");
            for (file in files) {
                val filename = file.substring(file.lastIndexOf("/") + 1);
                if(filename.startsWith("__cover__.")) {
                    iconPath = file;
                    continue;
                }

                emoticons.add(object : ExtraEmoticon() {
                    override fun QQEmoticonObject(): Any {
                        val info = FavoriteEmoticonInfo.newInstance();
                        info.set("path", file);
                        return info;
                    }
                })
            }
            this.emoticons = emoticons
            if (iconPath == null) {
                iconPath = emoticons[0].QQEmoticonObject().get<String>("path")!!;
            }
        }
        override fun emoticons(): List<ExtraEmoticon> {
            return emoticons;
        }

        override fun emoticonPanelIconURL(): String {
            return "file://$iconPath";
        }

        override fun uniqueId(): String {
            return iconPath!!;
        }
    }
    override fun extraEmoticonList(): List<ExtraEmoticonPanel> {
        val files = listDir("/storage/emulated/0/Documents/TGStickersExported");
        val panels = mutableListOf<ExtraEmoticonPanel>()
        for (file in files) {
            val panel = Panel(file);
            panels.add(panel);
        }
        return panels;
    }
    override fun uniqueId(): String {
        return "LocalDocumentEmoticonProvider";
    }
}


@FunctionHookEntry
@UiItemAgentEntry
object DumpTelegramStickers : CommonSwitchFunctionHook() {
    override val name = "使用 Telegram Stickers 表情包集"
    override val description = "【还没做完】需使用 MicroBlock 的 Telegram 表情包同步插件";

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY;

    override fun initOnce(): Boolean {
        val EmoticonPanelController = Initiator.loadClass("com.tencent.mobileqq.emoticonview.EmoticonPanelController");
        val EmotionPanelViewPagerAdapter = Initiator.loadClass("com.tencent.mobileqq.emoticonview.EmotionPanelViewPagerAdapter");
        val EmoticonTabAdapter = Initiator.loadClass("com.tencent.mobileqq.emoticonview.EmoticonTabAdapter");
        val EmoticonPanelInfo = Initiator.loadClass("com.tencent.mobileqq.emoticonview.EmotionPanelInfo");
        val EmoticonPackage = Initiator.loadClass("com.tencent.mobileqq.data.EmoticonPackage");

        var providers: List<ExtraEmoticonProvider> = listOf(LocalDocumentEmoticonProvider());

        class QAEpId {
            public var providerId: String = "";
            public var panelId: String = "";
        }
        fun parseQAEpId(epId: String): QAEpId? {
            if (!epId.startsWith("qa:")) return null;
            val data = epId.substring(3);
            val providerId = data.substring(0, data.indexOf(":"));
            val panelId = data.substring(data.indexOf(":") + 1);
            return QAEpId().apply {
                this.providerId = providerId;
                this.panelId = panelId;
            }
        }

        var emoticonPanelViewAdapterInstance: Any? = null;

        EmotionPanelViewPagerAdapter.hookAllConstructorAfter {
            emoticonPanelViewAdapterInstance = it.thisObject;
        }

        // handleIPSite 只接受数字 epId，防崩
        HookUtils.hookBeforeIfEnabled(this,
        EmotionPanelViewPagerAdapter.getMethod("handleIPSite")) {
            val pack = it.args[0];
            if(pack != null && parseQAEpId(pack.get<String>("epId")!!) != null) {
                it.args[0] = null;
            }
        }

        // 显示图标
        HookUtils.hookBeforeIfEnabled(this, EmoticonTabAdapter.getMethod("generateTabUrl")) {
            val id = parseQAEpId(it.args[0] as String);

            if(id != null) {
                val provider = providers.find { it.uniqueId() == id.providerId };
                if(provider != null) {
                    val panel = provider.extraEmoticonList().find { it.uniqueId() == id.panelId };
                    if(panel != null) {
                        val url = panel.emoticonPanelIconURL();
                        it.result = url;
                    }
                }
            }
        }

        var lastPanelDataSize = -1;

        // 生成 Tab 面板
        HookUtils.hookAfterIfEnabled(this, EmoticonPanelController.getMethod("getPanelDataList")) {
            // 移除自带面板
            // TODO: 做成可选
            // 鸽子：当然不是我来做（

            val list = it.result as MutableList<Any>;
            val iterator = list.iterator();

            while(iterator.hasNext()) {
                val element = iterator.next();
                val epId = element.get<String>("epId")!!;

                val typeWhiteList = listOf(
//                    13, // 表情商城,
//                    18, // 搜索表情,
                    7, // Emoji 表情,
                    4, // 收藏表情,
//                    12, // GIF
//                    17, // QQ什么玩意专属表情
                );

                if(!typeWhiteList.contains(element.get<Int>("type")!!)) {
                    iterator.remove();
                }
            }

            // 添加自定义面板
            for(provider in providers) {
                for(panel in provider.extraEmoticonList()) {
                    val info = EmoticonPanelInfo.newInstance();

                    val pack = EmoticonPackage.newInstance();
                    pack.set("epId", "qa:${provider.uniqueId()}:${panel.uniqueId()}");
                    pack.set("name", "QAExtraSticker");

                    info.set("emotionPkg", pack);
                    info.set("type", 6);
                    list.add(info);
                }
            }

            if(lastPanelDataSize != list.size) {
                lastPanelDataSize = list.size;
                emoticonPanelViewAdapterInstance?.invoke("notifyDataSetChanged");
            }
        }

        // 面板数据
        HookUtils.hookBeforeIfEnabled(this, EmotionPanelViewPagerAdapter.getMethod("getEmotionPanelData")) {
            val pkg = it.args[0].get("emotionPkg") ?: return@hookBeforeIfEnabled;
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

        return false;
    }

    override val isAvailable = QAppUtils.isQQnt();
}
