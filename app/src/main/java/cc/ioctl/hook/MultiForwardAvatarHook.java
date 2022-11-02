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
package cc.ioctl.hook;

import static cc.ioctl.util.Reflex.getFirstNSFByType;
import static cc.ioctl.util.Reflex.getShortClassName;
import static io.github.qauxv.util.Initiator._ChatMessage;
import static io.github.qauxv.util.Initiator.load;
import static io.github.qauxv.util.Initiator.loadClass;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import cc.ioctl.util.HostStyledViewBuilder;
import cc.ioctl.util.LayoutHelper;
import cc.ioctl.util.Reflex;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import io.github.qauxv.R;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.ui.CustomDialog;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.UiThread;
import io.github.qauxv.util.dexkit.CAIOUtils;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import me.ketal.dispacher.BaseBubbleBuilderHook;
import me.ketal.dispacher.OnBubbleBuilder;
import me.singleneuron.data.MsgRecordData;

@FunctionHookEntry
@UiItemAgentEntry
public class MultiForwardAvatarHook extends CommonSwitchFunctionHook implements OnBubbleBuilder {

    public static final MultiForwardAvatarHook INSTANCE = new MultiForwardAvatarHook();
    private static Field mLeftCheckBoxVisible = null;

    private MultiForwardAvatarHook() {
        super(new DexKitTarget[]{CAIOUtils.INSTANCE});
    }

    @NonNull
    @Override
    public String getName() {
        return "转发消息点头像查看详细信息";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "仅限合并转发的消息";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.MESSAGE_CATEGORY;
    }

    /**
     * Target TIM or QQ<=7.6.0 Here we use DexKit!!!
     *
     * @param v the view in bubble
     * @return message or null
     */
    @Nullable
    //@Deprecated
    public static Object getChatMessageByView(View v) {
        Class<?> cl_AIOUtils = DexKit.loadClassFromCache(CAIOUtils.INSTANCE);
        if (cl_AIOUtils == null) {
            return null;
        }
        try {
            return Reflex.invokeStaticAny(cl_AIOUtils, v, View.class, _ChatMessage());
        } catch (NoSuchMethodException e) {
            return null;
        } catch (Exception e) {
            Log.e(e);
            return null;
        }
    }

    @UiThread
    private static void createAndShowDialogForTroop(final Context __ctx, final Object msg) {
        if (msg == null) {
            Log.e("createAndShowDialogForTroop/E msg == null");
            return;
        }
        CustomDialog dialog = CustomDialog.createFailsafe(__ctx).setTitle(getShortClassName(msg))
                .setPositiveButton("确认", null).setCancelable(true)
                .setNeutralButton("详情",
                        (dialog1, which) -> createAndShowDialogForDetail(__ctx, msg));
        Context ctx = dialog.getContext();
        LinearLayout ll = new LinearLayout(ctx);
        ll.setOrientation(LinearLayout.VERTICAL);
        int p = LayoutHelper.dip2px(ctx, 10);
        ll.setPadding(p, p / 3, p, p / 3);
        String senderuin = (String) Reflex.getInstanceObjectOrNull(msg, "senderuin");
        String frienduin = (String) Reflex.getInstanceObjectOrNull(msg, "frienduin");
        HostStyledViewBuilder.newDialogClickableItemClickToCopy(ctx, "群号", frienduin, ll, true,
                v -> OpenProfileCard.openTroopProfileActivity(__ctx, frienduin));
        HostStyledViewBuilder.newDialogClickableItemClickToCopy(ctx, "成员", senderuin, ll, true,
                v -> {
                    try {
                        long uin = Long.parseLong(senderuin);
                        if (uin > 10000) {
                            OpenProfileCard.openUserProfileCard(__ctx, uin);
                        }
                    } catch (Exception e) {
                        Log.e(e);
                    }
                });
        TextView tv = new TextView(ctx);
        tv.setText("(单击可打开，长按可复制)");
        ll.addView(tv);
        dialog.setView(ll);
        dialog.show();
    }

    @UiThread
    private static void createAndShowDialogForPrivateMsg(final Context __ctx, final Object msg) {
        if (msg == null) {
            Log.e("createAndShowDialogForPrivateMsg/E msg == null");
            return;
        }
        CustomDialog dialog = CustomDialog.createFailsafe(__ctx).setTitle(getShortClassName(msg))
                .setPositiveButton("确认", null).setCancelable(true)
                .setNeutralButton("详情",
                        (dialog1, which) -> createAndShowDialogForDetail(__ctx, msg));
        Context ctx = dialog.getContext();
        LinearLayout ll = new LinearLayout(ctx);
        int p = LayoutHelper.dip2px(ctx, 10);
        ll.setPadding(p, p / 3, p, p / 3);
        ll.setOrientation(LinearLayout.VERTICAL);
        String senderuin = (String) Reflex.getInstanceObjectOrNull(msg, "senderuin");
        HostStyledViewBuilder.newDialogClickableItemClickToCopy(ctx, "发送者", senderuin, ll, true,
                v -> {
                    try {
                        long uin = Long.parseLong(senderuin);
                        if (uin > 10000) {
                            OpenProfileCard.openUserProfileCard(__ctx, uin);
                        }
                    } catch (Exception e) {
                        Log.e(e);
                    }
                });
        TextView tv = new TextView(ctx);
        tv.setText("(单击可打开，长按可复制)");
        ll.addView(tv);
        dialog.setView(ll);
        dialog.show();
    }

    @UiThread
    public static void createAndShowDialogForDetail(final Context ctx, final Object msg) {
        if (msg == null) {
            Log.e("createAndShowDialogForDetail/E msg == null");
            return;
        }
        CustomDialog.createFailsafe(ctx).setTitle(Reflex.getShortClassName(msg))
                .setMessage(msg.toString())
                .setCancelable(true).setPositiveButton("确定", null).show();
    }

    public static boolean isLeftCheckBoxVisible() {
        Field a = null, b = null;
        try {
            if (mLeftCheckBoxVisible != null) {
                return mLeftCheckBoxVisible.getBoolean(null);
            } else {
                for (Field f : load("com/tencent/mobileqq/activity/aio/BaseChatItemLayout")
                        .getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers()) && Modifier.isPublic(f.getModifiers())
                            && f.getType().equals(boolean.class)) {
                        if ("a".equals(f.getName())) {
                            a = f;
                        }
                        if ("b".equals(f.getName())) {
                            b = f;
                        }
                    }
                }
                if (a != null) {
                    mLeftCheckBoxVisible = a;
                    return a.getBoolean(null);
                }
                if (b != null) {
                    mLeftCheckBoxVisible = b;
                    return b.getBoolean(null);
                }
                return false;
            }
        } catch (Exception e) {
            INSTANCE.traceError(e);
            return false;
        }
    }

    private int mChatItemHeadIconViewId = 0;

    @Override
    @SuppressLint("DiscouragedApi")
    public boolean initOnce() throws Exception {
        BaseBubbleBuilderHook.INSTANCE.initialize();
        Class<?> kBaseBubbleBuilder = loadClass("com/tencent/mobileqq/activity/aio/BaseBubbleBuilder");
        Method onClick = kBaseBubbleBuilder.getMethod("onClick", View.class);
        HookUtils.hookBeforeIfEnabled(this, onClick, 49, param -> {
            Context ctx = Reflex.getInstanceObjectOrNull(param.thisObject, "a", Context.class);
            if (ctx == null) {
                ctx = getFirstNSFByType(param.thisObject, Context.class);
            }
            View view = (View) param.args[0];
            if (ctx == null || isLeftCheckBoxVisible()) {
                return;
            }
            String activityName = ctx.getClass().getName();
            boolean needShow = activityName.equals("com.tencent.mobileqq.activity.MultiForwardActivity") &&
                    (view.getClass().getName().equals("com.tencent.mobileqq.vas.avatar.VasAvatar") ||
                            view.getClass().equals(ImageView.class) ||
                            view.getClass().equals(load("com.tencent.widget.CommonImageView")));
            if (!needShow) {
                return;
            }
            Object msg = getChatMessageByView(view);
            if (msg == null) {
                return;
            }
            int istroop = (int) Reflex.getInstanceObjectOrNull(msg, "istroop");
            if (istroop == 1 || istroop == 3000) {
                createAndShowDialogForTroop(ctx, msg);
            } else if (istroop == 0) {
                createAndShowDialogForPrivateMsg(ctx, msg);
            } else {
                createAndShowDialogForDetail(ctx, msg);
            }
        });
        mChatItemHeadIconViewId = HostInfo.getApplication().getResources()
                .getIdentifier("chat_item_head_icon", "id", HostInfo.getPackageName());
        if (mChatItemHeadIconViewId == 0) {
            throw new IllegalStateException("R.id.chat_item_head_icon not found");
        }
        return true;
    }

    @Override
    public void onGetView(@NonNull ViewGroup rootView, @NonNull MsgRecordData chatMessage, @NonNull MethodHookParam param) {
        // XXX: performance sensitive, peak frequency: ~68 invocations per second
        if (!isEnabled() || mChatItemHeadIconViewId == 0) {
            return;
        }
        // For versions >= x (x exists, where x <= 8.9.15), @[R.id.chat_item_head_icon].onCLickListener = null
        // register @[R.id.chat_item_head_icon] click event for versions >= x
        OnClickListener baseBubbleBuilderOnClick = (OnClickListener) param.thisObject;
        View headIconView = (View) rootView.getTag(R.id.tag_chat_item_head_icon);
        if (headIconView == null) {
            headIconView = rootView.findViewById(mChatItemHeadIconViewId);
            if (headIconView != null) {
                rootView.setTag(R.id.tag_chat_item_head_icon, headIconView);
            } else {
                // give up.
                return;
            }
        }
        headIconView.setOnClickListener(baseBubbleBuilderOnClick);
    }
}
