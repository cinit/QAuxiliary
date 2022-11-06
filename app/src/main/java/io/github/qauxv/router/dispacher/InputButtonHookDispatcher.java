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
package io.github.qauxv.router.dispacher;

import static cc.ioctl.util.Reflex.getFirstNSFByType;
import static io.github.qauxv.util.Initiator._SessionInfo;

import android.content.Context;
import android.os.Parcelable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import cc.ioctl.hook.msg.AioChatPieClipPasteHook;
import cc.ioctl.hook.experimental.CardMsgSender;
import cc.ioctl.util.HookUtils;
import com.hicore.hook.ReplyMsgWithImg;
import io.github.duzhaokun123.hook.SendTTSHook;
import io.github.qauxv.R;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.hook.BaseHookDispatcher;
import io.github.qauxv.router.decorator.IBaseChatPieDecorator;
import io.github.qauxv.router.decorator.IBaseChatPieInitDecorator;
import io.github.qauxv.router.decorator.IInputButtonDecorator;
import io.github.qauxv.ui.TouchEventToLongClickAdapter;
import io.github.qauxv.ui.widget.InterceptLayout;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.Toasts;
import io.github.qauxv.util.dexkit.CArkAppItemBubbleBuilder;
import io.github.qauxv.util.dexkit.CFaceDe;
import io.github.qauxv.util.dexkit.CTestStructMsg;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import io.github.qauxv.util.dexkit.NBaseChatPie_init;
import java.lang.reflect.Method;
import java.util.Objects;
import mqq.app.AppRuntime;

@FunctionHookEntry
public class InputButtonHookDispatcher extends BaseHookDispatcher<IBaseChatPieDecorator> {

    public static final InputButtonHookDispatcher INSTANCE = new InputButtonHookDispatcher();

    private InputButtonHookDispatcher() {
        super(new DexKitTarget[]{
                CArkAppItemBubbleBuilder.INSTANCE,
                CFaceDe.INSTANCE,
                NBaseChatPie_init.INSTANCE,
                CTestStructMsg.INSTANCE
        });
    }

    private static final IBaseChatPieDecorator[] DECORATORS = {
            CardMsgSender.INSTANCE,
            AioChatPieClipPasteHook.INSTANCE,
            ReplyMsgWithImg.INSTANCE,
            SendTTSHook.INSTANCE,
    };

    @NonNull
    @Override
    public IBaseChatPieDecorator[] getDecorators() {
        return DECORATORS;
    }

    @Override
    public boolean initOnce() throws Exception {
        //Begin: send btn
        HookUtils.hookAfterIfEnabled(this, DexKit.requireMethodFromCache(NBaseChatPie_init.INSTANCE), 40,
                param -> {
                    Object chatPie = param.thisObject;
                    //Class cl_PatchedButton = load("com/tencent/widget/PatchedButton");
                    ViewGroup __aioRootView = null;
                    for (Method m : Initiator._BaseChatPie().getDeclaredMethods()) {
                        if (m.getReturnType() == ViewGroup.class
                                && m.getParameterTypes().length == 0) {
                            __aioRootView = (ViewGroup) m.invoke(chatPie);
                            break;
                        }
                    }
                    if (__aioRootView == null) {
                        Log.w("AIO root view not found");
                        return;
                    }
                    ViewGroup aioRootView = __aioRootView;
                    Context ctx = aioRootView.getContext();
                    int funBtnId = ctx.getResources().getIdentifier("fun_btn", "id", ctx.getPackageName());
                    View sendBtn = aioRootView.findViewById(funBtnId);
                    final AppRuntime qqApp = getFirstNSFByType(param.thisObject, Initiator._QQAppInterface());
                    final Parcelable session = getSessionInfo(param.thisObject);
                    Objects.requireNonNull(qqApp, "QQAppInterface is null");
                    Objects.requireNonNull(session, "SessionInfo is null");
                    boolean isInterceptorInstalled = false;
                    {
                        ViewGroup parent = (ViewGroup) sendBtn.getParent();
                        if (parent instanceof InterceptLayout) {
                            isInterceptorInstalled = true;
                        } else {
                            ViewGroup p2 = (ViewGroup) parent.getParent();
                            if (p2 instanceof InterceptLayout) {
                                isInterceptorInstalled = true;
                            }
                        }
                    }
                    if (!isInterceptorInstalled) {
                        InterceptLayout layout = InterceptLayout.setupRudely(sendBtn);
                        layout.setId(R.id.inject_fun_btn_intercept);
                        layout.setTouchInterceptor(new TouchEventToLongClickAdapter() {
                            @Override
                            public boolean onTouch(View v, MotionEvent event) {
                                ViewGroup vg = (ViewGroup) v;
                                if (event.getAction() == MotionEvent.ACTION_DOWN &&
                                        vg.getChildCount() != 0 && vg.getChildAt(0).isEnabled()) {
                                    return false;
                                }
                                return super.onTouch(v, event);
                            }

                            @Override
                            public boolean onLongClick(View v) {
                                try {
                                    ViewGroup vg = (ViewGroup) v;
                                    Context ctx = v.getContext();
                                    if (vg.getChildCount() != 0 && !vg.getChildAt(0).isEnabled()) {
                                        EditText input = aioRootView.findViewById(
                                                ctx.getResources().getIdentifier("input", "id", ctx.getPackageName()));
                                        String text = input.getText().toString();
                                        if (text.length() == 0) {
                                            Toasts.error(ctx, "请先输入卡片代码");
                                        }
                                        return true;
                                    }
                                } catch (Exception e) {
                                    traceError(e);
                                }
                                return false;
                            }
                        }.setLongPressTimeoutFactor(1.5f));
                    }
                    sendBtn.setOnLongClickListener(v -> {
                        Context ctx1 = v.getContext();
                        EditText input = aioRootView.findViewById(ctx1.getResources()
                                .getIdentifier("input", "id", ctx1.getPackageName()));
                        String text = input.getText().toString();
                        if (((TextView) v).length() == 0) { //|| !CardMsgHook.INSTANCE.isEnabled()
                            return false;
                        }
                        for (IBaseChatPieDecorator decorator : DECORATORS) {
                            if (decorator instanceof IInputButtonDecorator) {
                                IInputButtonDecorator d = (IInputButtonDecorator) decorator;
                                try {
                                    if (d.isEnabled() && d.onFunBtnLongClick(text, session, input, sendBtn, ctx1, qqApp)) {
                                        return true;
                                    }
                                } catch (Throwable e) {
                                    decorator.traceError(e);
                                }
                            }
                        }
                        return true;
                    });
                    // call BaseChatPie init decorators
                    for (IBaseChatPieDecorator baseDecorator : DECORATORS) {
                        if (baseDecorator instanceof IBaseChatPieInitDecorator) {
                            IBaseChatPieInitDecorator decorator = (IBaseChatPieInitDecorator) baseDecorator;
                            try {
                                if (decorator.isEnabled()) {
                                    decorator.onInitBaseChatPie(chatPie, aioRootView, session, ctx, qqApp);
                                }
                            } catch (Throwable e) {
                                decorator.traceError(e);
                            }
                        }
                    }
                });
        //End: send btn
        return true;
    }

    @Override
    public boolean isEnabled() {
        for (IBaseChatPieDecorator decorator : DECORATORS) {
            if (decorator.isEnabled()) {
                return true;
            }
        }
        return false;
    }

    public static Parcelable getSessionInfo(@NonNull Object baseChatPie) {
        return getFirstNSFByType(baseChatPie, _SessionInfo());
    }
}
