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
package cc.ioctl.hook.ui.chat;

import static cc.ioctl.util.LayoutHelper.dip2px;
import static cc.ioctl.util.Reflex.getFirstNSFByType;
import static cc.ioctl.util.Reflex.setInstanceObject;
import static io.github.qauxv.util.Initiator._MixedMsgItemBuilder;
import static io.github.qauxv.util.Initiator._PicItemBuilder;
import static io.github.qauxv.util.Initiator._PttItemBuilder;
import static io.github.qauxv.util.Initiator._QQAppInterface;
import static io.github.qauxv.util.Initiator._ReplyItemBuilder;
import static io.github.qauxv.util.Initiator._SessionInfo;
import static io.github.qauxv.util.Initiator._TextItemBuilder;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.dialog.RepeaterIconSettingDialog;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HookUtils.BeforeAndAfterHookedMethod;
import cc.ioctl.util.HostInfo;
import cc.ioctl.util.LayoutHelper;
import cc.ioctl.util.Reflex;
import io.github.qauxv.base.IEntityAgent;
import io.github.qauxv.base.RuntimeErrorTracer;
import io.github.qauxv.util.xpcompat.XC_MethodHook;
import io.github.qauxv.base.ISwitchCellAgent;
import io.github.qauxv.base.IUiItemAgent;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.bridge.AppRuntimeHelper;
import io.github.qauxv.bridge.ChatActivityFacade;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.BaseFunctionHook;
import io.github.qauxv.ui.widget.LinearLayoutDelegate;
import io.github.qauxv.util.QQVersion;
import io.github.qauxv.util.Toasts;
import io.github.qauxv.util.dexkit.CFaceDe;
import io.github.qauxv.util.dexkit.DexKitTarget;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;
import kotlinx.coroutines.flow.MutableStateFlow;
import mqq.app.AppRuntime;

@FunctionHookEntry
@UiItemAgentEntry
public class RepeaterHook extends BaseFunctionHook {

    public static final RepeaterHook INSTANCE = new RepeaterHook();

    private RepeaterHook() {
        super(null, false, new DexKitTarget[]{CFaceDe.INSTANCE});
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
                    return (agent, context) -> new String[]{"复读机", "+1"};
                }

                @Override
                public Function3<IUiItemAgent, Activity, View, Unit> getOnClickListener() {
                    return (agent, activity, view) -> {
                        RepeaterIconSettingDialog dialog = new RepeaterIconSettingDialog(activity);
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
                public Function2<IEntityAgent, Context, CharSequence> getSummaryProvider() {
                    return (agent, context) -> "此功能不支持较新的版本，推荐使用消息+1 Plus。点击设置自定义+1图标";
                }

                @NonNull
                @Override
                public Function1<IEntityAgent, String> getTitleProvider() {
                    return agent -> "消息+1";
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
        Method getView = null;
        Class<?> listener2 = null;
        Class<?> itemHolder = null;
        Class<?> BaseChatItemLayout = null;
        Class<?> ChatMessage = null;
        //begin: pic
        for (Method m : _PicItemBuilder().getDeclaredMethods()) {
            Class<?>[] argt = m.getParameterTypes();
            if (m.getReturnType() == View.class && m.getName().equalsIgnoreCase(HostInfo.requireMinQQVersion(QQVersion.QQ_8_8_93) ? "F" : "a")) {
                if (argt.length > 4 && argt[2] == View.class) {
                    getView = m;
                    listener2 = argt[4];
                    itemHolder = argt[1];
                    ChatMessage = argt[0];
                    BaseChatItemLayout = argt[3];
                }
            }
        }
        HookUtils.hookAfterIfEnabled(this, getView, param -> {
            ViewGroup relativeLayout = (ViewGroup) param.getResult();
            Context ctx = relativeLayout.getContext();
            if (ctx.getClass().getName().contains("ChatHistoryActivity") ||
                    ctx.getClass().getName().contains("MultiForwardActivity")) {
                return;
            }
            final AppRuntime app = getFirstNSFByType(param.thisObject, _QQAppInterface());
            final Parcelable session = getFirstNSFByType(param.thisObject, _SessionInfo());
            String uin = "" + AppRuntimeHelper.getLongAccountUin();
            if (relativeLayout.findViewById(101) == null) {
                View childAt = relativeLayout.getChildAt(0);
                ViewGroup viewGroup = (ViewGroup) childAt.getParent();
                viewGroup.removeView(childAt);
                int __id = childAt.getId();
                LinearLayout linearLayout = new LinearLayout(ctx);
                if (__id != -1) {
                    linearLayout.setId(__id);
                }
                linearLayout.setOrientation(0);
                linearLayout.setGravity(17);
                ImageView imageView = new ImageView(ctx);
                imageView.setId(101);
                imageView.setImageBitmap(RepeaterIconSettingDialog.getRepeaterIcon());
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
                layoutParams.rightMargin = dip2px(ctx, (float) 10);
                linearLayout.addView(imageView, layoutParams);
                linearLayout.addView(childAt, childAt.getLayoutParams());
                ImageView imageView2 = new ImageView(ctx);
                imageView2.setId(102);
                imageView2.setImageBitmap(RepeaterIconSettingDialog.getRepeaterIcon());
                LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-2, -2);
                layoutParams2.leftMargin = dip2px(ctx, (float) 10);
                linearLayout.addView(imageView2, layoutParams2);
                viewGroup.addView(linearLayout, -2, -2);
            }
            ImageView imageView3 = relativeLayout.findViewById(101);
            ImageView imageView4 = relativeLayout.findViewById(102);
            if (Reflex.getInstanceObject(param.args[0], "senderuin", String.class).equals(uin)) {
                imageView3.setVisibility(0);
                imageView4.setVisibility(8);
            } else {
                imageView3.setVisibility(8);
                imageView4.setVisibility(0);
            }
            View.OnClickListener r0 = view -> {
                try {
                    ChatActivityFacade.repeatMessage(app, session, param.args[0]);
                } catch (Throwable e) {
                    traceError(e);
                    Toasts.error(HostInfo.getApplication(), e.toString());
                }
            };
            imageView3.setOnClickListener(r0);
            imageView4.setOnClickListener(r0);
        });
        //end: pic
        //begin: text
        if (HostInfo.isTim()) {
            // TODO: 2020/5/17 Add MsgForText +1 for TIM
            Method m = _TextItemBuilder().getDeclaredMethod("a",
                    ChatMessage, itemHolder, View.class, BaseChatItemLayout, listener2);
            HookUtils.hookAfterIfEnabled(this, m, param -> {
                View view;
                View resultView = (View) param.getResult();
                Context ctx = resultView.getContext();
                if (ctx.getClass().getName().contains("ChatHistoryActivity")
                        || ctx.getClass().getName().contains("MultiForwardActivity")) {
                    return;
                }
                final AppRuntime app = getFirstNSFByType(param.thisObject, _QQAppInterface());
                final Parcelable session = getFirstNSFByType(param.thisObject, _SessionInfo());
                String uin = "" + AppRuntimeHelper.getLongAccountUin();
                if (resultView.findViewById(101) == null) {
                    LinearLayoutDelegate linearLayout = new LinearLayoutDelegate(ctx);
                    linearLayout.setOrientation(0);
                    linearLayout.setGravity(17);
                    ImageView imageView = new ImageView(ctx);
                    imageView.setId(101);
                    imageView.setImageBitmap(RepeaterIconSettingDialog.getRepeaterIcon());
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
                    layoutParams.rightMargin = dip2px(ctx, (float) 5);
                    linearLayout.addView(imageView, layoutParams);
                    ViewGroup p = (ViewGroup) resultView.getParent();
                    if (p != null) {
                        p.removeView(resultView);
                    }
                    ViewGroup.LayoutParams currlp = resultView.getLayoutParams();
                    linearLayout.addView(resultView, -2, -2);
                    linearLayout.setDelegate(resultView);
                    ImageView imageView2 = new ImageView(ctx);
                    imageView2.setId(102);
                    imageView2.setImageBitmap(RepeaterIconSettingDialog.getRepeaterIcon());
                    LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-2, -2);
                    layoutParams2.leftMargin = dip2px(ctx, (float) 5);
                    linearLayout.addView(imageView2, layoutParams2);
                    linearLayout.setPadding(0, 0, 0, 0);
                    param.setResult(linearLayout);
                    view = linearLayout;
                } else {
                    view = resultView.findViewById(101);
                }
                ImageView imageView3 = view.findViewById(101);
                @SuppressLint("ResourceType") ImageView imageView4 = view.findViewById(102);
                if (Reflex.getInstanceObject(param.args[0], "senderuin", String.class).equals(uin)) {
                    imageView3.setVisibility(0);
                    imageView4.setVisibility(8);
                } else {
                    imageView3.setVisibility(8);
                    imageView4.setVisibility(0);
                }
                View.OnClickListener r0 = view1 -> {
                    try {
                        ChatActivityFacade.repeatMessage(app, session, param.args[0]);
                    } catch (Throwable e) {
                        traceError(e);
                        Toasts.error(HostInfo.getApplication(), e.toString());
                    }
                };
                imageView3.setOnClickListener(r0);
                imageView4.setOnClickListener(r0);
            });
        } else {
            Method m = _TextItemBuilder().getDeclaredMethod(HostInfo.requireMinQQVersion(QQVersion.QQ_8_8_93) ? "F" : "a",
                    ChatMessage, itemHolder, View.class, BaseChatItemLayout, listener2);
            HookUtils.hookBeforeAndAfterIfEnabled(this, m, 50, new BeforeAndAfterHookedMethod() {
                @Override
                public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    View v = (View) param.args[2];
                    if (v != null && (v.getContext().getClass().getName().contains("ChatHistoryActivity")
                            || v.getContext().getClass().getName().contains("MultiForwardActivity"))) {
                        return;
                    }
                    Reflex.setInstanceObject(param.args[0], "isFlowMessage", true);
                    if (((int) Reflex.getInstanceObjectOrNull(param.args[0], "extraflag")) == 32768) {
                        setInstanceObject(param.args[0], "extraflag", 0);
                    }
                }

                @Override
                public void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    RelativeLayout baseChatItemLayout = (RelativeLayout) param.args[3];
                    ImageView imageView = baseChatItemLayout.findViewById(
                            baseChatItemLayout.getResources().getIdentifier("cfx", "id",
                                    HostInfo.getPackageName()));
                    ImageView imageView2 = baseChatItemLayout.findViewById(
                            baseChatItemLayout.getResources().getIdentifier("cfw", "id",
                                    HostInfo.getPackageName()));
                    Bitmap repeat = RepeaterIconSettingDialog.getRepeaterIcon();
                    imageView.setImageBitmap(repeat);
                    imageView2.setImageBitmap(repeat);
                    final AppRuntime app = getFirstNSFByType(param.thisObject, _QQAppInterface());
                    final Parcelable session = getFirstNSFByType(param.thisObject, _SessionInfo());
                    final Object msg = param.args[0];
                    View.OnClickListener r0 = v -> {
                        try {
                            ChatActivityFacade.repeatMessage(app, session, msg);
                        } catch (Throwable e) {
                            traceError(e);
                            Toasts.error(HostInfo.getApplication(), e.toString());
                        }
                    };
                    imageView.setOnClickListener(r0);
                    imageView2.setOnClickListener(r0);
                }
            });
        }
        //end: text
        //begin: ptt
        Method pttMethod = _PttItemBuilder().getDeclaredMethod(HostInfo.requireMinQQVersion(QQVersion.QQ_8_8_93) ? "B0" : "a",
                ChatMessage, itemHolder, View.class, BaseChatItemLayout, listener2);
        HookUtils.hookAfterIfEnabled(this, pttMethod, 51, param -> {
            ViewGroup convertView = (ViewGroup) param.getResult();
            Context ctx = convertView.getContext();
            if (ctx.getClass().getName().contains("ChatHistoryActivity")
                    || ctx.getClass().getName().contains("MultiForwardActivity")) {
                return;
            }
            final AppRuntime app = getFirstNSFByType(param.thisObject, _QQAppInterface());
            final Parcelable session = getFirstNSFByType(param.thisObject, _SessionInfo());
            String uin = "" + AppRuntimeHelper.getLongAccountUin();
            if (convertView.findViewById(101) == null) {
                LinearLayoutDelegate wrapperLayout = new LinearLayoutDelegate(ctx);
                wrapperLayout.setDelegate(convertView);
                //wrapperLayout.setId(Integer.parseInt((String) Hook.config.get("PttItem_id"), 16));
                wrapperLayout.setOrientation(0);
                wrapperLayout.setGravity(17);
                ImageView leftIcon = new ImageView(ctx);
                leftIcon.setId(101);
                leftIcon.setImageBitmap(RepeaterIconSettingDialog.getRepeaterIcon());
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
                layoutParams.rightMargin = dip2px(ctx, (float) 5);
                wrapperLayout.addView(leftIcon, layoutParams);
                wrapperLayout.addView(convertView, -2, -2);
                ImageView rightIcon = new ImageView(ctx);
                rightIcon.setId(102);
                rightIcon.setImageBitmap(RepeaterIconSettingDialog.getRepeaterIcon());
                LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-2, -2);
                layoutParams2.leftMargin = dip2px(ctx, (float) 5);
                wrapperLayout.addView(rightIcon, layoutParams2);
                param.setResult(wrapperLayout);
                convertView = wrapperLayout;
            }
            ImageView leftIcon = convertView.findViewById(101);
            ImageView rightIcon = convertView.findViewById(102);
            if (Reflex.getInstanceObject(param.args[0], "senderuin", String.class).equals(uin)) {
                leftIcon.setVisibility(0);
                rightIcon.setVisibility(8);
            } else {
                leftIcon.setVisibility(8);
                rightIcon.setVisibility(0);
            }
            View.OnClickListener l = view -> {
                try {
                    ChatActivityFacade.repeatMessage(app, session, param.args[0]);
                } catch (Throwable e) {
                    Toasts.error(HostInfo.getApplication(), e.toString());
                    traceError(e);
                }
            };
            leftIcon.setOnClickListener(l);
            rightIcon.setOnClickListener(l);
        });
        //end: ptt
        if(HostInfo.isQQ() && HostInfo.requireMinQQVersion(QQVersion.QQ_8_5_0)){
            //start reply
            Method replyMethod = _ReplyItemBuilder().getDeclaredMethod(HostInfo.requireMinQQVersion(QQVersion.QQ_8_8_93) ? "F" : "a",
                    ChatMessage, itemHolder, View.class, BaseChatItemLayout, listener2);
            HookUtils.hookAfterIfEnabled(this, replyMethod, 51, param -> {
                ViewGroup relativeLayout = (ViewGroup) param.getResult();
                if (relativeLayout == null) {
                    return;
                }
                relativeLayout = (ViewGroup) relativeLayout.getParent();
                Context ctx = relativeLayout.getContext();
                if (ctx.getClass().getName().contains("ChatHistoryActivity") ||
                        ctx.getClass().getName().contains("MultiForwardActivity")) {
                    return;
                }
                int defSize = 45;
                final AppRuntime app = getFirstNSFByType(param.thisObject, _QQAppInterface());
                final Parcelable session = getFirstNSFByType(param.thisObject, _SessionInfo());
                boolean isSendFromLocal = (boolean) Reflex.invokeVirtual(param.args[0],"isSendFromLocal",boolean.class); //是否是自己发的消息
                ImageButton imageButton =relativeLayout.findViewById(101);
                if(imageButton==null) {
                    //不存在则创建
                    imageButton = new ImageButton(ctx);
                    imageButton.setImageBitmap(RepeaterIconSettingDialog.getRepeaterIcon());
                    imageButton.setBackgroundColor(Color.TRANSPARENT);

                    imageButton.setAdjustViewBounds(true);
                    imageButton.getBackground().setAlpha(100);
                    imageButton.setMaxHeight(LayoutHelper.dip2px(ctx, defSize));
                    imageButton.setMaxWidth(LayoutHelper.dip2px(ctx, defSize));
                    imageButton.setId(101);
                    relativeLayout.addView(imageButton);
                }else {
                    imageButton.setVisibility(View.VISIBLE);
                }
                View alignParent = findView("SelectableLinearLayout",relativeLayout);
                RelativeLayout.LayoutParams llparam = new RelativeLayout.LayoutParams(LayoutHelper.dip2px(ctx, defSize), LayoutHelper.dip2px(ctx, defSize));
                if (isSendFromLocal){
                    llparam.removeRule(RelativeLayout.ALIGN_RIGHT);
                    llparam.removeRule(RelativeLayout.ALIGN_TOP);
                    llparam.removeRule(RelativeLayout.ALIGN_LEFT);

                    llparam.addRule(RelativeLayout.ALIGN_LEFT,alignParent.getId());
                    int width = View.MeasureSpec.makeMeasureSpec(0,
                            View.MeasureSpec.UNSPECIFIED);
                    int height = View.MeasureSpec.makeMeasureSpec(0,
                            View.MeasureSpec.UNSPECIFIED);
                    alignParent.measure(width,height);

                    int AddedLength = alignParent.getTop();
                    AddedLength += alignParent.getHeight()/2- LayoutHelper.dip2px(ctx,defSize)/2;

                    int OffsetV = LayoutHelper.dip2px(ctx,35);

                    ViewGroup.MarginLayoutParams mLParam = llparam;
                    mLParam.leftMargin=-OffsetV;
                    mLParam.topMargin =AddedLength;
                }else {
                    llparam.removeRule(RelativeLayout.ALIGN_RIGHT);
                    llparam.removeRule(RelativeLayout.ALIGN_TOP);
                    llparam.removeRule(RelativeLayout.ALIGN_LEFT);

                    llparam.addRule(RelativeLayout.ALIGN_RIGHT,alignParent.getId());
                    int width = View.MeasureSpec.makeMeasureSpec(0,
                            View.MeasureSpec.UNSPECIFIED);
                    int height = View.MeasureSpec.makeMeasureSpec(0,
                            View.MeasureSpec.UNSPECIFIED);
                    alignParent.measure(width,height);

                    int AddedLength = alignParent.getTop();
                    AddedLength += alignParent.getHeight()/2- LayoutHelper.dip2px(ctx,defSize)/2;

                    int OffsetV = LayoutHelper.dip2px(ctx,35);
                    ViewGroup.MarginLayoutParams mLParam = llparam;
                    mLParam.rightMargin=-OffsetV;
                    mLParam.topMargin =AddedLength;
                }
                imageButton.setLayoutParams(llparam);
                View.OnClickListener r0 = view -> {
                    try {
                        ChatActivityFacade.repeatMessage(app, session, param.args[0]);
                    } catch (Throwable e) {
                        traceError(e);
                        Toasts.error(HostInfo.getApplication(), e.toString());
                    }
                };
                imageButton.setOnClickListener(r0);
            });
            //end: reply
            //start: mixedMsg
            Method mixedMethod = _MixedMsgItemBuilder().getDeclaredMethod(HostInfo.requireMinQQVersion(QQVersion.QQ_8_8_93) ? "F" : "a",
                    ChatMessage, itemHolder, View.class, BaseChatItemLayout, listener2);
            HookUtils.hookAfterIfEnabled(this, mixedMethod, 51, param -> {
                ViewGroup relativeLayout = (ViewGroup) param.getResult();
                if (relativeLayout == null) {
                    return;
                }
                relativeLayout = (ViewGroup) relativeLayout.getParent();
                Context ctx = relativeLayout.getContext();
                if (ctx.getClass().getName().contains("ChatHistoryActivity") ||
                        ctx.getClass().getName().contains("MultiForwardActivity")) {
                    return;
                }
                int defSize = 45;
                final AppRuntime app = getFirstNSFByType(param.thisObject, _QQAppInterface());
                final Parcelable session = getFirstNSFByType(param.thisObject, _SessionInfo());
                String uin = "" + AppRuntimeHelper.getLongAccountUin();
                boolean isSendFromLocal = Reflex.getInstanceObject(param.args[0], "senderuin", String.class).equals(uin); //是否是自己发的消息
                ImageButton imageButton =relativeLayout.findViewById(101);
                if(imageButton==null) {
                    //不存在则创建
                    imageButton = new ImageButton(ctx);
                    imageButton.setImageBitmap(RepeaterIconSettingDialog.getRepeaterIcon());
                    imageButton.setBackgroundColor(Color.TRANSPARENT);

                    imageButton.setAdjustViewBounds(true);
                    imageButton.getBackground().setAlpha(100);
                    imageButton.setMaxHeight(LayoutHelper.dip2px(ctx, defSize));
                    imageButton.setMaxWidth(LayoutHelper.dip2px(ctx, defSize));
                    imageButton.setId(101);
                    relativeLayout.addView(imageButton);
                }else {
                    imageButton.setVisibility(View.VISIBLE);
                }
                View alignParent = findView("MixedMsgLinearLayout",relativeLayout);
                RelativeLayout.LayoutParams llparam = new RelativeLayout.LayoutParams(LayoutHelper.dip2px(ctx, defSize), LayoutHelper.dip2px(ctx, defSize));
                if (isSendFromLocal){
                    llparam.removeRule(RelativeLayout.ALIGN_RIGHT);
                    llparam.removeRule(RelativeLayout.ALIGN_TOP);
                    llparam.removeRule(RelativeLayout.ALIGN_LEFT);

                    llparam.addRule(RelativeLayout.ALIGN_LEFT,alignParent.getId());
                    int width = View.MeasureSpec.makeMeasureSpec(0,
                            View.MeasureSpec.UNSPECIFIED);
                    int height = View.MeasureSpec.makeMeasureSpec(0,
                            View.MeasureSpec.UNSPECIFIED);
                    alignParent.measure(width,height);

                    int AddedLength = alignParent.getTop();
                    AddedLength += alignParent.getHeight()/2- LayoutHelper.dip2px(ctx,defSize)/2;

                    int OffsetV = LayoutHelper.dip2px(ctx,35);

                    ViewGroup.MarginLayoutParams mLParam = llparam;
                    mLParam.leftMargin=-OffsetV;
                    mLParam.topMargin =AddedLength;
                }else {
                    llparam.removeRule(RelativeLayout.ALIGN_RIGHT);
                    llparam.removeRule(RelativeLayout.ALIGN_TOP);
                    llparam.removeRule(RelativeLayout.ALIGN_LEFT);

                    llparam.addRule(RelativeLayout.ALIGN_RIGHT,alignParent.getId());
                    int width = View.MeasureSpec.makeMeasureSpec(0,
                            View.MeasureSpec.UNSPECIFIED);
                    int height = View.MeasureSpec.makeMeasureSpec(0,
                            View.MeasureSpec.UNSPECIFIED);
                    alignParent.measure(width,height);

                    int AddedLength = alignParent.getTop();
                    AddedLength += alignParent.getHeight()/2- LayoutHelper.dip2px(ctx,defSize)/2;

                    int OffsetV = LayoutHelper.dip2px(ctx,35);
                    ViewGroup.MarginLayoutParams mLParam = llparam;
                    mLParam.rightMargin=-OffsetV;
                    mLParam.topMargin =AddedLength;
                }
                imageButton.setLayoutParams(llparam);
                View.OnClickListener r0 = view -> {
                    try {
                        ChatActivityFacade.repeatMessage(app, session, param.args[0]);
                    } catch (Throwable e) {
                        traceError(e);
                        Toasts.error(HostInfo.getApplication(), e.toString());
                    }
                };
                imageButton.setOnClickListener(r0);
            });
            //end MixedMsg
        }
        return true;
    }
    public static View findView(String Name, ViewGroup vg)
    {
        for(int i=0;i<vg.getChildCount();i++)
        {
            if(vg.getChildAt(i).getClass().getSimpleName().contains(Name))
            {
                return vg.getChildAt(i);
            }
        }
        return null;
    }

    @Nullable
    @Override
    public List<RuntimeErrorTracer> getRuntimeErrorDependentComponents() {
        return null;
    }
}
