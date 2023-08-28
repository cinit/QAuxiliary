/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the qwq233 Universal License
 * as published on https://github.com/qwq233/license; either
 * version 2 of the License, or any later version and our EULA as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the qwq233 Universal License for more details.
 *
 * See
 * <https://github.com/qwq233/license>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package cc.hicore.hook;


import static cc.ioctl.util.Reflex.getFirstNSFByType;
import static io.github.qauxv.util.Initiator._SessionInfo;
import static io.github.qauxv.util.Initiator.load;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.hicore.ReflectUtil.MField;
import cc.hicore.ReflectUtil.MMethod;
import cc.hicore.dialog.RepeaterPlusIconSettingDialog;
import cc.hicore.message.bridge.Nt_kernel_bridge;
import cc.hicore.message.chat.SessionHooker;
import cc.hicore.message.chat.SessionUtils;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import cc.ioctl.util.Reflex;
import com.tencent.qqnt.kernel.nativeinterface.Contact;
import com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService;
import com.tencent.qqnt.kernel.nativeinterface.MsgAttributeInfo;
import com.xiaoniu.util.ContextUtils;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import io.github.qauxv.R;
import io.github.qauxv.base.ISwitchCellAgent;
import io.github.qauxv.base.IUiItemAgent;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.bridge.AppRuntimeHelper;
import io.github.qauxv.bridge.ntapi.MsgServiceHelper;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.BaseFunctionHook;
import io.github.qauxv.util.CustomMenu;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.QQVersion;
import io.github.qauxv.util.dexkit.AbstractQQCustomMenuItem;
import io.github.qauxv.util.dexkit.DexKitTarget;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;
import kotlinx.coroutines.flow.MutableStateFlow;

@FunctionHookEntry
@UiItemAgentEntry
public class RepeaterPlus extends BaseFunctionHook implements SessionHooker.IAIOParamUpdate {

    public static final RepeaterPlus INSTANCE = new RepeaterPlus();

    private RepeaterPlus() {
        super(null, false, new DexKitTarget[]{AbstractQQCustomMenuItem.INSTANCE});
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
                    return (agent, context) -> "点击设置自定义+1图标和显示位置";
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
        if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_9_63)) {
            if (!RepeaterPlusIconSettingDialog.getIsShowInMenu()) {
                XC_MethodHook callback = new XC_MethodHook() {
                    private ImageView img;
                    private volatile long click_time = 0;

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        ImageView imageView;
                        if (param.args.length == 0) {
                            Object result = param.getResult();
                            if (result instanceof ImageView) {
                                this.img = (ImageView) result;
                                this.img.setImageBitmap(RepeaterPlusIconSettingDialog.getRepeaterIcon());
                            }
                        } else if (param.args.length == 3 && (imageView = this.img) != null) {
                            if (img.getContext().getClass().getName().contains("MultiForwardActivity")) {
                                return;
                            }
                            img.setOnClickListener(v -> {
                                if (RepeaterPlusIconSettingDialog.getIsDoubleClick()) {
                                    try {
                                        if (System.currentTimeMillis() - 200 > click_time) {
                                            return;
                                        }
                                    } finally {
                                        click_time = System.currentTimeMillis();
                                    }
                                }
                                repeatByForwardNt(param.args[1]);
                            });
                            imageView.setVisibility(0);
                        }
                    }
                };
                Class clz = Initiator.loadClass("com.tencent.mobileqq.aio.msglist.holder.component.msgfollow.AIOMsgFollowComponent");
                for (Method method : clz.getDeclaredMethods()) {
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    boolean z = true;
                    boolean z2 = parameterTypes.length == 0 && method.getReturnType().equals(ImageView.class);
                    if (parameterTypes.length != 3 || !parameterTypes[0].equals(Integer.TYPE) || !parameterTypes[2].equals(List.class)) {
                        z = z2;
                    }
                    if (z) {
                        XposedBridge.hookMethod(method, callback);
                    }
                }

            } else {
                Class msgClass = Initiator.loadClass("com.tencent.mobileqq.aio.msg.AIOMsgItem");
                String[] component = new String[]{
                        "com.tencent.mobileqq.aio.msglist.holder.component.text.AIOTextContentComponent",
                        "com.tencent.mobileqq.aio.msglist.holder.component.pic.AIOPicContentComponent",
                        "com.tencent.mobileqq.aio.msglist.holder.component.reply.AIOReplyComponent",
                        "com.tencent.mobileqq.aio.msglist.holder.component.anisticker.AIOAniStickerContentComponent",
                        "com.tencent.mobileqq.aio.msglist.holder.component.video.AIOVideoContentComponent",
                        "com.tencent.mobileqq.aio.msglist.holder.component.multifoward.AIOMultifowardContentComponent",
                        "com.tencent.mobileqq.aio.msglist.holder.component.longmsg.AIOLongMsgContentComponent",
                        "com.tencent.mobileqq.aio.msglist.holder.component.mix.AIOMixContentComponent",
                        "com.tencent.mobileqq.aio.msglist.holder.component.ark.AIOArkContentComponent",
                        "com.tencent.mobileqq.aio.msglist.holder.component.file.AIOFileContentComponent",
                        "com.tencent.mobileqq.aio.msglist.holder.component.LocationShare.AIOLocationShareComponent"
                };
                Method getMsg = null;
                Method[] methods = Initiator.loadClass("com.tencent.mobileqq.aio.msglist.holder.component.BaseContentComponent").getDeclaredMethods();
                for (Method method : methods) {
                    if (method.getReturnType() == msgClass && method.getParameterTypes().length == 0) {
                        getMsg = method;
                        getMsg.setAccessible(true);
                        break;
                    }
                }
                for (String s : component) {
                    Class componentClazz = Initiator.loadClass(s);
                    Method listMethod = null;
                    methods = componentClazz.getDeclaredMethods();
                    for (Method method : methods) {
                        if (method.getReturnType() == List.class && method.getParameterTypes().length == 0) {
                            listMethod = method;
                            listMethod.setAccessible(true);
                            break;
                        }
                    }
                    Method finalGetMsg = getMsg;
                    HookUtils.hookAfterIfEnabled(this, listMethod, param -> {
                        if (ContextUtils.getCurrentActivity().getClass().getName().contains("MultiForwardActivity")) {
                            return;
                        }
                        Object msg = finalGetMsg.invoke(param.thisObject);
                        Object item = CustomMenu.createItemNt(msg, "+1", R.id.item_repeat, () -> {
                            repeatByForwardNt(msg);
                            return Unit.INSTANCE;
                        });
                        List list = (List) param.getResult();
                        List result = new ArrayList<>();
                        result.add(0, item);
                        result.addAll(list);
                        param.setResult(result);
                    });
                }

            }
            return true;
        }

        //Below Not QQNT

        else {
            Class<?> kChatAdapter1 = Initiator.load("com.tencent.mobileqq.activity.aio.ChatAdapter1");
            if (kChatAdapter1 == null) {
                Class<?> kGuildPieAdapter = Initiator.load("com.tencent.mobileqq.guild.chatpie.GuildPieAdapter");
                kChatAdapter1 = kGuildPieAdapter == null ? null : kGuildPieAdapter.getSuperclass();
            }
            Objects.requireNonNull(kChatAdapter1, "ChatAdapter1.class is null");
            if (!RepeaterPlusIconSettingDialog.getIsShowInMenu()) {
                HookUtils.hookAfterIfEnabled(this, MMethod.FindMethod(kChatAdapter1, "getView", View.class, new Class[]{
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
                    List<Object> MessageRecoreList = MField.GetFirstField(param.thisObject, List.class);
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
                HookUtils.hookBeforeIfEnabled(this, MMethod.FindMethod("com.tencent.mobileqq.data.ChatMessage", "isFollowMessage", boolean.class, new Class[0]),
                        param -> param.setResult(false));

            } else {
                List<Class<?>> list = Arrays.asList(
                        Initiator._TextItemBuilder(),
                        Initiator._PicItemBuilder(),
                        Initiator._PicItemBuilder().getSuperclass(),
                        Initiator._MixedMsgItemBuilder());
                list.forEach(item -> {
                    XposedHelpers.findAndHookMethod(item, "a", int.class, Context.class,
                            load("com/tencent/mobileqq/data/ChatMessage"), new RepeaterHelper.MenuItemClickCallback());
                    for (Method m : item.getDeclaredMethods()) {
                        if (!m.getReturnType().isArray()) {
                            continue;
                        }
                        Class<?>[] ps = m.getParameterTypes();
                        if (ps.length == 1 && ps[0].equals(View.class)) {
                            XposedBridge.hookMethod(m, new RepeaterHelper.GetMenuItemCallBack());
                            break;
                        }
                    }
                });
            }
        }

        return true;
    }

    @Override
    public boolean isAvailable() {
        return HostInfo.isQQ() && HostInfo.requireMinQQVersion(QQVersion.QQ_8_6_0);
    }

    private static Object AIOParam;

    @Override
    public void onAIOParamUpdate(Object AIOParam) {
        RepeaterPlus.AIOParam = AIOParam;
    }

    private void repeatByForwardNt(Object msg) {
        try {
            Contact contact = SessionUtils.AIOParam2Contact(AIOParam);
            long msgID = (long) Reflex.invokeVirtual(msg, "getMsgId");
            ArrayList<Contact> c = new ArrayList<>();
            c.add(contact);

            ArrayList<Long> l = new ArrayList<>();
            l.add(msgID);

            IKernelMsgService service = MsgServiceHelper.getKernelMsgService(AppRuntimeHelper.getAppRuntime());
            HashMap<Integer, MsgAttributeInfo> attrMap = new HashMap<>();
            MsgAttributeInfo info = Nt_kernel_bridge.getDefaultAttributeInfo();
            if (info != null) {
                attrMap.put(0, info);
                service.forwardMsg(l, contact, c, attrMap, (i, str, hashMap) -> {

                });
            }


        } catch (Exception e) {
            Log.e(e);
        }

    }
}
