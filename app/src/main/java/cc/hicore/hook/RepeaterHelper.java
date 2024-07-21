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
import static cc.hicore.hook.RepeaterPlus.INSTANCE;
import static io.github.qauxv.util.Initiator._SessionInfo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import cc.hicore.QApp.QAppUtils;
import cc.hicore.ReflectUtil.MField;
import cc.hicore.ReflectUtil.XField;
import cc.hicore.ReflectUtil.XMethod;
import cc.hicore.dialog.RepeaterPlusIconSettingDialog;
import cc.ioctl.util.LayoutHelper;
import io.github.qauxv.util.xpcompat.XC_MethodHook;
import io.github.qauxv.R;
import io.github.qauxv.util.CustomMenu;
import io.github.qauxv.util.LicenseStatus;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.Toasts;
import java.lang.reflect.Array;
import java.util.HashMap;

@SuppressLint("ResourceType")
public class RepeaterHelper {

    private static final HashMap<String, String> supportMessageTypes = new HashMap<>();

    static {
        supportMessageTypes.put("MessageForPic", "RelativeLayout");
        supportMessageTypes.put("MessageForText", "ETTextView");
        supportMessageTypes.put("MessageForLongTextMsg", "ETTextView");
        supportMessageTypes.put("MessageForFoldMsg", "ETTextView");
        supportMessageTypes.put("MessageForPtt", "BreathAnimationLayout");
        supportMessageTypes.put("MessageForMixedMsg", "MixedMsgLinearLayout");
        supportMessageTypes.put("MessageForReplyText", "SelectableLinearLayout");
        supportMessageTypes.put("MessageForScribble", "RelativeLayout");
        supportMessageTypes.put("MessageForMarketFace", "RelativeLayout");
        supportMessageTypes.put("MessageForTroopEffectPic", "RelativeLayout");
        supportMessageTypes.put("MessageForAniSticker", "FrameLayout");
        supportMessageTypes.put("MessageForArkFlashChat", "ArkAppRootLayout");
        supportMessageTypes.put("MessageForShortVideo", "RelativeLayout");
        supportMessageTypes.put("MessageForPokeEmo", "RelativeLayout");
        supportMessageTypes.put("MessageForStructing", "RelativeLayout");
    }

    private static volatile long click_time = 0;

    public static void createRepeatIcon(RelativeLayout baseChatItem, Object ChatMsg, Object session) throws Exception {
        boolean isSendFromLocal;
        int istroop = XField.obj(ChatMsg).name("istroop").type(int.class).get();
        if (istroop == 1 || istroop == 0) {
            String UserUin = XField.obj(ChatMsg).name("senderuin").type(String.class).get();
            isSendFromLocal = UserUin.equals(QAppUtils.getCurrentUin());
        } else {
            isSendFromLocal = XMethod.obj(ChatMsg).name("isSendFromLocal").ret( boolean.class).invoke();
        }

        Context context = baseChatItem.getContext();
        String clzName = ChatMsg.getClass().getSimpleName();

        if (supportMessageTypes.containsKey(clzName) && checkIsAvailStruct(ChatMsg)) {
            ImageButton imageButton = baseChatItem.findViewById(88486666);
            if (imageButton == null) {
                imageButton = new ImageButton(context);
                imageButton.setImageBitmap(RepeaterPlusIconSettingDialog.getRepeaterIcon());
                RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(
                        LayoutHelper.dip2px(context, RepeaterPlusIconSettingDialog.getDpiSet()),
                        LayoutHelper.dip2px(context, RepeaterPlusIconSettingDialog.getDpiSet()));
                imageButton.setAdjustViewBounds(true);
                imageButton.getBackground().setAlpha(0);
                imageButton.setMaxHeight(LayoutHelper.dip2px(context, RepeaterPlusIconSettingDialog.getDpiSet()));
                imageButton.setMaxWidth(LayoutHelper.dip2px(context, RepeaterPlusIconSettingDialog.getDpiSet()));
                imageButton.setId(88486666);
                imageButton.setTag(ChatMsg);
                imageButton.setOnClickListener(v -> {
                    if (RepeaterPlusIconSettingDialog.getIsDoubleClick()) {
                        try {
                            if (System.currentTimeMillis() - 200 > click_time) {
                                return;
                            }
                        } finally {
                            click_time = System.currentTimeMillis();
                        }
                    }
                    try {
                        Repeater.Repeat(session, v.getTag());
                    } catch (Exception e) {
                        Toasts.error(null, e + "");
                        Log.e(e);
                    }
                });
                baseChatItem.addView(imageButton, param);
            } else {
                imageButton.setVisibility(View.VISIBLE);
                imageButton.setTag(ChatMsg);

            }
            RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams) imageButton.getLayoutParams();
            String attachName = supportMessageTypes.get(clzName);
            View attachView = findView(attachName, baseChatItem);
            if (attachView != null) {
                if (RepeaterPlusIconSettingDialog.getIsShowUpper()) {
                    param.removeRule(RelativeLayout.ALIGN_RIGHT);
                    param.removeRule(RelativeLayout.ALIGN_TOP);
                    param.removeRule(RelativeLayout.ALIGN_LEFT);
                    param.addRule(RelativeLayout.ALIGN_TOP, attachView.getId());
                    if (isSendFromLocal) {
                        param.addRule(RelativeLayout.ALIGN_LEFT, attachView.getId());
                        param.leftMargin = -(LayoutHelper.dip2px(context, RepeaterPlusIconSettingDialog.getDpiSet()) / 4);
                    } else {
                        param.addRule(RelativeLayout.ALIGN_RIGHT, attachView.getId());
                        param.rightMargin = -(LayoutHelper.dip2px(context, RepeaterPlusIconSettingDialog.getDpiSet()) / 4);
                    }
                    param.topMargin = -(LayoutHelper.dip2px(context, RepeaterPlusIconSettingDialog.getDpiSet()) / 4);
                } else {
                    if (isSendFromLocal) {
                        param.removeRule(RelativeLayout.RIGHT_OF);
                        param.addRule(RelativeLayout.LEFT_OF, attachView.getId());
                        int AddedLength = attachView.getTop();
                        AddedLength += attachView.getHeight() / 2 - LayoutHelper.dip2px(context, RepeaterPlusIconSettingDialog.getDpiSet()) / 2;
                        int OffsetV = LayoutHelper.dip2px(context, 12);
                        param.leftMargin = -OffsetV;
                        param.topMargin = AddedLength;
                    } else {
                        param.removeRule(RelativeLayout.LEFT_OF);
                        param.addRule(RelativeLayout.RIGHT_OF, attachView.getId());
                        int AddedLength = attachView.getTop();
                        AddedLength += attachView.getHeight() / 2 - LayoutHelper.dip2px(context, RepeaterPlusIconSettingDialog.getDpiSet()) / 2;
                        int OffsetV = LayoutHelper.dip2px(context, 12);
                        param.rightMargin = -OffsetV;
                        param.topMargin = AddedLength;
                    }
                }
                imageButton.setLayoutParams(param);
            }
        } else {
            ImageButton imageButton = baseChatItem.findViewById(88486666);
            if (imageButton != null) {
                imageButton.setVisibility(View.GONE);
            }
        }
    }
    private static boolean checkIsAvailStruct(Object msg) throws Exception {
        if (msg.getClass().getSimpleName().equals("MessageForStructing")) {
            Object struct = XField.obj(msg).name( "structingMsg").get();
            if (struct != null) {
                int id = XField.obj(struct).name("mMsgServiceID").get();
                return id == 5;
            }
            return false;
        }
        return true;
    }

    public static View findView(String Name, ViewGroup vg) {
        for (int i = 0; i < vg.getChildCount(); i++) {
            if (vg.getChildAt(i).getClass().getSimpleName().contains(Name)) {
                return vg.getChildAt(i);
            }
        }
        return null;
    }


    public static class GetMenuItemCallBack extends XC_MethodHook {

        public GetMenuItemCallBack() {
            super(60);
        }

        @Override
        protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
            if (LicenseStatus.sDisableCommonHooks) {
                return;
            }
            if (!INSTANCE.isEnabled()) {
                return;
            }
            try {
                Object arr = param.getResult();
                Class<?> clQQCustomMenuItem = arr.getClass().getComponentType();
                Object item_copy = CustomMenu.createItem(clQQCustomMenuItem, R.id.item_repeat, "+1");
                Object ret = Array.newInstance(clQQCustomMenuItem, Array.getLength(arr) + 1);
                //noinspection SuspiciousSystemArraycopy
                System.arraycopy(arr, 0, ret, 0, Array.getLength(arr));
                Array.set(ret, Array.getLength(arr), item_copy);
                param.setResult(ret);
            } catch (Throwable e) {
                INSTANCE.traceError(e);
                throw e;
            }
        }
    }

    public static class MenuItemClickCallback extends XC_MethodHook {

        public MenuItemClickCallback() {
            super(60);
        }

        @Override
        protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
            int id = (int) param.args[0];
            final Activity ctx = (Activity) param.args[1];
            final Object chatMessage = param.args[2];
            if (id == R.id.item_repeat) {
                param.setResult(null);
                try {
                    Parcelable session = getFirstNSFByType(param.thisObject, _SessionInfo());
                    Repeater.Repeat(session, chatMessage);
                } catch (Throwable e) {
                    INSTANCE.traceError(e);
                    Toasts.error(ctx, e.toString().replace("java.lang.", ""));
                }
            }
        }
    }

}
