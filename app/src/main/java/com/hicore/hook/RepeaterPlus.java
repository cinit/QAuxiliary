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

package com.hicore.hook;


import static cc.ioctl.util.Reflex.getFirstNSFByType;
import static io.github.qauxv.util.Initiator._SessionInfo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HookUtils.BeforeHookedMethod;
import cc.ioctl.util.HostInfo;
import com.hicore.ReflectUtil.MField;
import com.hicore.ReflectUtil.MMethod;
import com.hicore.dialog.RepeaterPlusIconSettingDialog;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import io.github.qauxv.base.ISwitchCellAgent;
import io.github.qauxv.base.IUiItemAgent;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.BaseFunctionHook;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.QQVersion;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;
import kotlinx.coroutines.flow.MutableStateFlow;

@FunctionHookEntry
@UiItemAgentEntry
public class RepeaterPlus extends BaseFunctionHook {

    public static final RepeaterPlus INSTANCE = new RepeaterPlus();

    private RepeaterPlus() {
        super();
    }

    private IUiItemAgent mUiAgent = null;

    @NonNull
    @Override
    public IUiItemAgent getUiItemAgent() {
        if (mUiAgent == null) {
            mUiAgent = new IUiItemAgent() {
                private final ISwitchCellAgent mSwitchCellAgent = new ISwitchCellAgent() {
                    @Override
                    public boolean isChecked() {
                        return isEnabled();
                    }

                    @Override
                    public void setChecked(boolean isChecked) {
                        setEnabled(isChecked);
                    }

                    @Override
                    public boolean isCheckable() {
                        return true;
                    }
                };

                @Override
                public Function2<IUiItemAgent, Context, String[]> getExtraSearchKeywordProvider() {
                    return (agent, context) -> new String[]{"复读机Plus", "+1"};
                }

                @Override
                public Function3<IUiItemAgent, Activity, View, Unit> getOnClickListener() {
                    return (agent, activity, view) -> {
                        RepeaterPlusIconSettingDialog dialog = new RepeaterPlusIconSettingDialog(activity);
                        dialog.show();
                        return Unit.INSTANCE;
                    };
                }

                @Override
                public ISwitchCellAgent getSwitchProvider() {
                    return mSwitchCellAgent;
                }

                @Nullable
                @Override
                public Function1<IUiItemAgent, Boolean> getValidator() {
                    return null;
                }

                @Nullable
                @Override
                public MutableStateFlow<String> getValueState() {
                    return null;
                }

                @Override
                public Function2<IUiItemAgent, Context, CharSequence> getSummaryProvider() {
                    return (agent, context) -> "点击设置自定义+1图标";
                }

                @NonNull
                @Override
                public Function1<IUiItemAgent, String> getTitleProvider() {
                    return agent -> "消息+1 Plus";
                }
            };
        }
        return mUiAgent;
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.MESSAGE_CATEGORY;
    }

    @Override
    @SuppressLint({"WrongConstant", "ResourceType"})
    public boolean initOnce() throws Exception {
        HookUtils.hookAfterIfEnabled(this, MMethod.FindMethod("com.tencent.mobileqq.activity.aio.ChatAdapter1", "getView", View.class, new Class[]{
                int.class,
                View.class,
                ViewGroup.class
        }), param -> {
            Object mGetView = param.getResult();
            RelativeLayout baseChatItem = null;
            if (mGetView instanceof RelativeLayout) {
                baseChatItem = (RelativeLayout) mGetView;
            } else {
                return;
            }
            Context context = baseChatItem.getContext();
            if (context.getClass().getName().contains("MultiForwardActivity")) {
                return;
            }
            List MessageRecoreList = MField.GetFirstField(param.thisObject, List.class);
            if (MessageRecoreList == null) {
                return;
            }
            Object ChatMsg = MessageRecoreList.get((int) param.args[0]);
            if (ChatMsg == null) {
                return;
            }
            Parcelable session = getFirstNSFByType(param.thisObject, _SessionInfo());
            AtomicReference<OnGlobalLayoutListener> listenerContainer = new AtomicReference<>();
            RelativeLayout finalBaseChatItem = baseChatItem;
            listenerContainer.set(() -> {
                try {
                    RepeaterHelper.createRepeatIcon(finalBaseChatItem, ChatMsg, session);
                    finalBaseChatItem.getViewTreeObserver().removeOnGlobalLayoutListener(listenerContainer.get());
                } catch (Exception e) {
                    Log.e(e);
                }
            });

            finalBaseChatItem.getViewTreeObserver().addOnGlobalLayoutListener(listenerContainer.get());

        });

        HookUtils.hookBeforeIfEnabled(this, MMethod.FindMethod("com.tencent.mobileqq.data.ChatMessage", "isFollowMessage", boolean.class, new Class[0]), param -> param.setResult(false));


        return true;
    }

    @Override
    public boolean isAvailable() {
        return HostInfo.isQQ() && HostInfo.requireMinQQVersion(QQVersion.QQ_8_6_0);
    }
}
