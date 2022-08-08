package com.hicore.hook;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import cc.ioctl.util.LayoutHelper;
import com.hicore.ReflectUtil.MField;
import com.hicore.ReflectUtil.MMethod;
import com.hicore.dialog.RepeaterPlusIconSettingDialog;
import com.hicore.messageUtils.QQEnvUtils;
import de.robv.android.xposed.XposedBridge;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.Toasts;
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
        int istroop = MField.GetField(ChatMsg, "istroop", int.class);
        if (istroop == 1 || istroop == 0) {
            String UserUin = MField.GetField(ChatMsg, "senderuin", String.class);
            isSendFromLocal = UserUin.equals(QQEnvUtils.getCurrentUin());
        } else {
            isSendFromLocal = MMethod.CallMethodNoParam(ChatMsg, "isSendFromLocal", boolean.class);
        }

        Context context = baseChatItem.getContext();
        String clzName = ChatMsg.getClass().getSimpleName();

        if (supportMessageTypes.containsKey(clzName) && checkIsAvailStruct(ChatMsg)) {
            ImageButton imageButton = baseChatItem.findViewById(88486666);
            if (imageButton == null) {
                imageButton = new ImageButton(context);
                imageButton.setImageBitmap(RepeaterPlusIconSettingDialog.getRepeaterIcon());
                RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(
                        LayoutHelper.dip2px(context,  RepeaterPlusIconSettingDialog.getDpiSet()), LayoutHelper.dip2px(context,  RepeaterPlusIconSettingDialog.getDpiSet()));
                imageButton.setAdjustViewBounds(true);
                imageButton.getBackground().setAlpha(0);
                imageButton.setMaxHeight(LayoutHelper.dip2px(context, RepeaterPlusIconSettingDialog.getDpiSet()));
                imageButton.setMaxWidth(LayoutHelper.dip2px(context,  RepeaterPlusIconSettingDialog.getDpiSet()));
                imageButton.setId(88486666);
                imageButton.setTag(ChatMsg);
                imageButton.setOnClickListener(v -> {
                    if (RepeaterPlusIconSettingDialog.getIsDoubleClick()){
                        try {
                            if (System.currentTimeMillis() - 200 > click_time)return;
                        }finally {
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
                if (RepeaterPlusIconSettingDialog.getIsShowUpper()){
                    if (isSendFromLocal) {
                        param.removeRule(RelativeLayout.ALIGN_RIGHT);
                        param.removeRule(RelativeLayout.ALIGN_TOP);
                        param.removeRule(RelativeLayout.ALIGN_LEFT);
                        param.addRule(RelativeLayout.ALIGN_TOP, attachView.getId());
                        param.addRule(RelativeLayout.ALIGN_LEFT, attachView.getId());
                        param.leftMargin = -(LayoutHelper.dip2px(context, RepeaterPlusIconSettingDialog.getDpiSet()) / 4);
                        param.topMargin = -(LayoutHelper.dip2px(context,  RepeaterPlusIconSettingDialog.getDpiSet()) / 4);
                    } else {
                        param.removeRule(RelativeLayout.ALIGN_RIGHT);
                        param.removeRule(RelativeLayout.ALIGN_TOP);
                        param.removeRule(RelativeLayout.ALIGN_LEFT);
                        param.addRule(RelativeLayout.ALIGN_TOP, attachView.getId());
                        param.addRule(RelativeLayout.ALIGN_RIGHT, attachView.getId());
                        param.rightMargin = - (LayoutHelper.dip2px(context,  RepeaterPlusIconSettingDialog.getDpiSet()) / 4);
                        param.topMargin = - (LayoutHelper.dip2px(context,  RepeaterPlusIconSettingDialog.getDpiSet()) / 4);
                    }
                }else {
                    if (isSendFromLocal) {
                        param.removeRule(RelativeLayout.RIGHT_OF);
                        param.addRule(RelativeLayout.LEFT_OF, attachView.getId());
                        int AddedLength = attachView.getTop();
                        AddedLength += attachView.getHeight() / 2 - LayoutHelper.dip2px(context,  RepeaterPlusIconSettingDialog.getDpiSet()) / 2;
                        int OffsetV = LayoutHelper.dip2px(context,12);
                        param.leftMargin = -OffsetV;
                        param.topMargin = AddedLength;
                    } else {
                        param.removeRule(RelativeLayout.LEFT_OF);
                        param.addRule(RelativeLayout.RIGHT_OF, attachView.getId());
                        int AddedLength = attachView.getTop();
                        AddedLength += attachView.getHeight() / 2 - LayoutHelper.dip2px(context,  RepeaterPlusIconSettingDialog.getDpiSet()) / 2;
                        int OffsetV = LayoutHelper.dip2px(context,12);
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
        if (msg.getClass().getSimpleName().equals("MessageForStructing")){
            Object struct = MField.GetField(msg,"structingMsg");
            if (struct != null){
                int id = MField.GetField(struct,"mMsgServiceID");
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
}
