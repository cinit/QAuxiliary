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


import static cc.ioctl.util.HostInfo.requireMinQQVersion;
import static cc.ioctl.util.HostInfo.requireMinTimVersion;
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
import cc.hicore.QApp.QAppUtils;
import cc.hicore.ReflectUtil.XField;
import cc.hicore.ReflectUtil.XMethod;
import cc.hicore.dialog.RepeaterPlusIconSettingDialog;
import cc.hicore.message.chat.SessionHooker;
import cc.hicore.message.chat.SessionUtils;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.Reflex;
import com.tencent.qqnt.kernel.nativeinterface.MsgAttributeInfo;
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord;
import com.xiaoniu.dispatcher.OnMenuBuilder;
import com.xiaoniu.util.ContextUtils;
import io.github.qauxv.R;
import io.github.qauxv.base.IEntityAgent;
import io.github.qauxv.base.ISwitchCellAgent;
import io.github.qauxv.base.IUiItemAgent;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.bridge.AppRuntimeHelper;
import io.github.qauxv.bridge.kernelcompat.ContactCompat;
import io.github.qauxv.bridge.kernelcompat.KernelMsgServiceCompat;
import io.github.qauxv.bridge.ntapi.MsgConstants;
import io.github.qauxv.bridge.ntapi.MsgServiceHelper;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.BaseFunctionHook;
import io.github.qauxv.util.CustomMenu;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.QQVersion;
import io.github.qauxv.util.TIMVersion;
import io.github.qauxv.util.Toasts;
import io.github.qauxv.util.dexkit.AbstractQQCustomMenuItem;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import io.github.qauxv.util.dexkit.VasAttrBuilder;
import io.github.qauxv.util.xpcompat.XC_MethodHook;
import io.github.qauxv.util.xpcompat.XposedBridge;
import io.github.qauxv.util.xpcompat.XposedHelpers;
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
public class RepeaterPlus extends BaseFunctionHook implements SessionHooker.IAIOParamUpdate, OnMenuBuilder {

    public static final RepeaterPlus INSTANCE = new RepeaterPlus();

    private RepeaterPlus() {
        super(null, false, new DexKitTarget[]{AbstractQQCustomMenuItem.INSTANCE, VasAttrBuilder.INSTANCE});
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
                public Function2<IEntityAgent, Context, CharSequence> getSummaryProvider() {
                    return (agent, context) -> "点击设置自定义+1图标和显示位置";
                }

                @NonNull
                @Override
                public Function1<IEntityAgent, String> getTitleProvider() {
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
        if (requireMinQQVersion(QQVersion.QQ_8_9_63_BETA_11345) || requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA)) {
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
                                Object msgObj = param.args[1];
                                if (isMessageRepeatable(msgObj)) {
                                    repeatByForwardNt(msgObj);
                                } else {
                                    Toasts.error(v.getContext(), "该消息不支持复读");
                                }
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
                HookUtils.hookAfterIfEnabled(this, XMethod.clz(kChatAdapter1).name("getView").ret(View.class).param(
                        int.class,
                        View.class,
                        ViewGroup.class
                ).get(), param -> {
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
                    List<Object> MessageRecoreList = XField.obj(param.thisObject).type(List.class).get();
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
                HookUtils.hookBeforeIfEnabled(this, XMethod.clz("com.tencent.mobileqq.data.ChatMessage").name("isFollowMessage").ret(boolean.class).get(),
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
        return requireMinQQVersion(QQVersion.QQ_8_6_0) || requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA);
    }

    private static Object AIOParam;

    @Override
    public void onAIOParamUpdate(Object AIOParam) {
        RepeaterPlus.AIOParam = AIOParam;
    }

    private void repeatByForwardNt(Object msg) {
        try {
            ContactCompat contact = SessionUtils.AIOParam2Contact(AIOParam);
            long msgID = (long) Reflex.invokeVirtual(msg, "getMsgId");
            ArrayList<ContactCompat> c = new ArrayList<>();
            c.add(contact);

            ArrayList<Long> l = new ArrayList<>();
            l.add(msgID);

            KernelMsgServiceCompat service = MsgServiceHelper.getKernelMsgService(AppRuntimeHelper.getAppRuntime());
            HashMap<Integer, MsgAttributeInfo> attrMap = new HashMap<>();
            Method builder = DexKit.loadMethodFromCache(VasAttrBuilder.INSTANCE);
            if (builder != null) {
                Object builderInstance = builder.getDeclaringClass().newInstance();
                builder.invoke(builderInstance, attrMap, contact.toKernelObject(), 4);
            }

            service.getMsgsByMsgId(contact, l, (i, str, list) -> {
                if (list.isEmpty()) {
                    Toasts.error(ContextUtils.getCurrentActivity(), "消息获取失败，请重试");
                    return;
                }
                if (list.get(0).getElements().get(0).getPicElement() != null
                        || list.get(0).getElements().get(0).getMarketFaceElement() != null
                        || list.get(0).getElements().get(0).getStructMsgElement() != null
                        || list.get(0).getElements().get(0).getArkElement() != null) {
                    service.forwardMsg(l, contact, c, attrMap, (i2, str2, hashMap) -> {
                    });
                } else {
                    long msgUniqueId;
                    if (requireMinQQVersion(QQVersion.QQ_9_0_30) || requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA)) {
                        msgUniqueId = service.generateMsgUniqueId(contact.getChatType(), QAppUtils.getServiceTime());
                    } else {
                        msgUniqueId = service.getMsgUniqueId(QAppUtils.getServiceTime());
                    }
                    service.sendMsg(msgUniqueId, contact, list.get(0).getElements(), attrMap, (i1, str1) -> {
                    });
                }
            });


        } catch (Exception e) {
            Log.e(e);
        }

    }

    private static Method sMethod_AIOMsgItem_getMsgRecord;

    private boolean isMessageRepeatable(Object msg) {
        if (msg == null) {
            return false;
        }
        try {
            if (sMethod_AIOMsgItem_getMsgRecord == null) {
                sMethod_AIOMsgItem_getMsgRecord = Initiator.loadClass("com.tencent.mobileqq.aio.msg.AIOMsgItem")
                        .getMethod("getMsgRecord");
            }
            MsgRecord msgRecord = (MsgRecord) sMethod_AIOMsgItem_getMsgRecord.invoke(msg);
            int msgType = msgRecord.getMsgType();
            int msgSubType = msgRecord.getSubMsgType();
            if (msgType == MsgConstants.MSG_TYPE_WALLET) {
                // red packet should not be repeated
                // receiver will see unsupported msg type or empty msg
                return false;
            }
            // add more msg type here
            return true;
        } catch (Exception | LinkageError e) {
            traceError(e);
            return false;
        }
    }

    @NonNull
    @Override
    public String[] getTargetComponentTypes() {
        return new String[]{
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
    }

    @Override
    public void onGetMenuNt(@NonNull Object msg, @NonNull String componentType, @NonNull XC_MethodHook.MethodHookParam param) throws Exception {
        if (!isEnabled() || !RepeaterPlusIconSettingDialog.getIsShowInMenu()) {
            return;
        }
        if (ContextUtils.getCurrentActivity().getClass().getName().contains("MultiForwardActivity")) {
            return;
        }
        Object item = CustomMenu.createItemIconNt(msg, "+1", R.drawable.ic_item_repeat_72dp, R.id.item_repeat, () -> {
            if (isMessageRepeatable(msg)) {
                repeatByForwardNt(msg);
            } else {
                Toasts.error(ContextUtils.getCurrentActivity(), "该消息不支持复读");
            }
            return Unit.INSTANCE;
        });
        List list = (List) param.getResult();
        List result = new ArrayList<>();
        result.add(0, item);
        result.addAll(list);
        param.setResult(result);
    }
}
