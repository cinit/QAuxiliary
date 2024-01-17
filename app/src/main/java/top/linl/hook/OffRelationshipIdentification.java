package top.linl.hook;

import android.view.View;
import android.view.ViewParent;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import java.lang.reflect.Method;
import top.linl.util.reflect.FieldUtils;
import top.linl.util.reflect.MethodTool;


@FunctionHookEntry
@UiItemAgentEntry
public class OffRelationshipIdentification extends CommonSwitchFunctionHook {

    public static final OffRelationshipIdentification INSTANCE = new OffRelationshipIdentification();

    @Override
    protected boolean initOnce() throws Exception {
        Class<?> profileInStepComponentClass = Initiator.loadClass("com.tencent.mobileqq.profilecard.component.ProfileInStepComponent");

        Method method = profileInStepComponentClass.getDeclaredMethod(
                "onDataUpdate",
                Initiator.loadClass("com.tencent.mobileqq.profilecard.data.ProfileCardInfo"));
        HookUtils.hookAfterIfEnabled(this, method, param -> {
                    Object recyclerView = FieldUtils.getFirstField(param.thisObject,
                            Initiator.loadClass("com.tencent.biz.richframework.widget.listview.card.RFWCardListView"));
                    if (recyclerView == null) {
                        return;
                    }
                    LinearLayout parent = MethodTool.find(recyclerView.getClass()).name("getParent").returnType(ViewParent.class).call(recyclerView);
                    if (parent == null) {
                        throw new RuntimeException("ParentLayout == null");
                    }
                    for (int i = 0; i < parent.getChildCount(); i++) {
                        if (parent.getChildAt(i).getVisibility() != View.GONE) {
                            parent.getChildAt(i).setVisibility(View.GONE);
                        }
                    }
                    if (parent.getVisibility() != View.GONE) {
                        parent.setVisibility(View.GONE);
                    }
                }
        );
        return true;
    }


    /**
     * @return item 位置
     */
    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Simplify.UI_PROFILE;
    }

    /**
     * @return item name
     */
    @NonNull
    @Override
    public String getName() {
        return "精简陌生人资料卡你们的关系标识";
    }
}
