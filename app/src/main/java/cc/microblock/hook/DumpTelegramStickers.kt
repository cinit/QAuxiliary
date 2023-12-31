package cc.microblock.hook

import android.view.View
import android.widget.RelativeLayout
import cc.hicore.QApp.QAppUtils
import cc.ioctl.util.HookUtils
import cc.ioctl.util.Reflex
import com.github.kyuubiran.ezxhelper.utils.getObjectAs
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import xyz.nextalone.util.get
import xyz.nextalone.util.set

@FunctionHookEntry
@UiItemAgentEntry
object DumpTelegramStickers : CommonSwitchFunctionHook() {
    override val name = "读取 Telegram 中的 Sticker（实验性）"
    override val description = "还没做完";

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.EXPERIMENTAL_CATEGORY

    override fun initOnce(): Boolean {
        return false;
    }

    override val isAvailable = false ;
//    QAppUtils.isQQnt();
}
