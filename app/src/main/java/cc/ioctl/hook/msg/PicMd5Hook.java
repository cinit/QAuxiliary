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
package cc.ioctl.hook.msg;

import static io.github.qauxv.util.Initiator.load;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.hicore.QApp.QAppUtils;
import cc.hicore.ReflectUtil.XField;
import cc.hicore.ReflectUtil.XMethod;
import cc.hicore.Utils.FunProtoData;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.Reflex;
import com.tencent.qphone.base.remote.FromServiceMsg;
import com.tencent.qqnt.kernel.nativeinterface.PicElement;
import com.xiaoniu.dispatcher.OnMenuBuilder;
import com.xiaoniu.util.ContextUtils;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import io.github.qauxv.R;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.ui.CustomDialog;
import io.github.qauxv.util.CustomMenu;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.LicenseStatus;
import io.github.qauxv.util.Toasts;
import io.github.qauxv.util.dexkit.AbstractQQCustomMenuItem;
import io.github.qauxv.util.dexkit.DexKitTarget;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import kotlin.Unit;
import org.json.JSONObject;
import xyz.nextalone.util.SystemServiceUtils;

@FunctionHookEntry
@UiItemAgentEntry
public class PicMd5Hook extends CommonSwitchFunctionHook implements OnMenuBuilder {

    public static final PicMd5Hook INSTANCE = new PicMd5Hook();

    private PicMd5Hook() {
        super(new DexKitTarget[]{AbstractQQCustomMenuItem.INSTANCE});
    }

    public static String rkey_group;

    public static String rkey_private;

    @NonNull
    @Override
    public String getName() {
        return "显示图片MD5";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "长按图片消息点击MD5, 可同时复制图片链接";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.MESSAGE_CATEGORY;
    }

    @Override
    public boolean initOnce() throws Exception {

        HookUtils.hookBeforeIfEnabled(this, XMethod.clz("mqq.app.msghandle.MsgRespHandler").name("dispatchRespMsg").ignoreParam().get(), param -> {
            FromServiceMsg fromServiceMsg = XField.obj(param.args[1]).name("fromServiceMsg").get();

            if ("OidbSvcTrpcTcp.0x9067_202".equals(fromServiceMsg.getServiceCmd())) {
                FunProtoData data = new FunProtoData();
                data.fromBytes(getUnpPackage(fromServiceMsg.getWupBuffer()));

                JSONObject obj = data.toJSON();
                rkey_group = obj.getJSONObject("4")
                        .getJSONObject("4")
                        .getJSONArray("1")
                        .getJSONObject(0).getString("1");

                rkey_private = obj.getJSONObject("4")
                        .getJSONObject("4")
                        .getJSONArray("1")
                        .getJSONObject(1).getString("1");
            }
        });

        if (QAppUtils.isQQnt()) {
            return true;
        }
        Class<?> cl_PicItemBuilder = Initiator._PicItemBuilder();
        Class<?> cl_BasePicItemBuilder = cl_PicItemBuilder.getSuperclass();
        try {
            XposedHelpers.findAndHookMethod(cl_PicItemBuilder, "a", int.class, Context.class,
                    load("com/tencent/mobileqq/data/ChatMessage"), new MenuItemClickCallback());
            XposedHelpers.findAndHookMethod(cl_BasePicItemBuilder, "a", int.class,
                    Context.class,
                    load("com/tencent/mobileqq/data/ChatMessage"), new MenuItemClickCallback());
        } catch (Exception e) {
        }
        for (Method m : cl_PicItemBuilder.getDeclaredMethods()) {
            if (!m.getReturnType().isArray()) {
                continue;
            }
            Class<?>[] ps = m.getParameterTypes();
            if (ps.length == 1 && ps[0].equals(View.class)) {
                XposedBridge.hookMethod(m, new GetMenuItemCallBack());
                break;
            }
        }
        for (Method m : cl_BasePicItemBuilder.getDeclaredMethods()) {
            if (!m.getReturnType().isArray()) {
                continue;
            }
            Class<?>[] ps = m.getParameterTypes();
            if (ps.length == 1 && ps[0].equals(View.class)) {
                XposedBridge.hookMethod(m, new GetMenuItemCallBack());
                break;
            }
        }

        return true;
    }

    @NonNull
    @Override
    public String[] getTargetComponentTypes() {
        return new String[]{
                "com.tencent.mobileqq.aio.msglist.holder.component.pic.AIOPicContentComponent"
        };
    }

    @Override
    public void onGetMenuNt(@NonNull Object msg, @NonNull String componentType, @NonNull XC_MethodHook.MethodHookParam param) throws Exception {
        if (!isEnabled()) {
            return;
        }
        Object item = CustomMenu.createItemIconNt(msg, "MD5", R.drawable.ic_item_md5_72dp, R.id.item_showPicMd5, () -> {
            try {
                Method getElement = null;
                for (Method m : msg.getClass().getDeclaredMethods()) {
                    if (m.getReturnType() == PicElement.class) {
                        getElement = m;
                        break;
                    }
                }
                PicElement element = (PicElement) getElement.invoke(msg);
                String md5 = element.getMd5HexStr().toUpperCase();
                showMd5Dialog(ContextUtils.getCurrentActivity(), md5, element);
            } catch (Throwable e) {
                traceError(e);
            }
            return Unit.INSTANCE;
        });
        List list = (List) param.getResult();
        list.add(item);
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
                Object item_copy = CustomMenu.createItem(clQQCustomMenuItem, R.id.item_showPicMd5, "MD5");
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
            if (id == R.id.item_showPicMd5) {
                param.setResult(null);
                try {
                    final String md5;
                    if (chatMessage == null
                            || (md5 = (String) Reflex.getInstanceObjectOrNull(chatMessage, "md5")) == null
                            || md5.isEmpty()) {
                        Toasts.error(ctx, "获取图片MD5失败");
                        return;
                    }
                    showMd5Dialog(ctx, md5, null);
                } catch (Throwable e) {
                    INSTANCE.traceError(e);
                    Toasts.error(ctx, e.toString().replace("java.lang.", ""));
                }
            }
        }
    }

    private static void showMd5Dialog(Context ctx, String md5, PicElement element) {
        CustomDialog.createFailsafe(ctx).setTitle("MD5").setCancelable(true)
                .setMessage(md5).setPositiveButton("复制",
                        (dialog, which) -> SystemServiceUtils.copyToClipboard(ctx, md5))
                .setNeutralButton("复制图片链接",
                        (dialog, which) -> SystemServiceUtils.copyToClipboard(ctx, getPicturePath(md5, element)))
                .setNegativeButton("关闭", null).show();
    }

    private static String getPicturePath(@NonNull String md5, PicElement element) {
        if (element == null) {
            // legacy
            return "https://gchat.qpic.cn/gchatpic_new/0/0-0-" + md5 + "/0";
        }
        // java/cc/hicore/hook/stickerPanel/Hooker/StickerPanelEntryHooker.java #190
        String url;
        String originUrl = element.getOriginImageUrl();
        if (TextUtils.isEmpty(originUrl)) {
            url = "https://gchat.qpic.cn/gchatpic_new/0/0-0-" + md5 + "/0";
        } else {
            if (originUrl.startsWith("/download")) {
                if (originUrl.contains("appid=1406")) {
                    url = "https://multimedia.nt.qq.com.cn" + originUrl + rkey_group;
                } else {
                    url = "https://multimedia.nt.qq.com.cn" + originUrl + rkey_private;
                }
            } else {
                url = "https://gchat.qpic.cn" + element.getOriginImageUrl();
            }
        }
        return url;
    }


    private static byte[] getUnpPackage(byte[] b) {
        if (b == null) {
            return null;
        }
        if (b.length < 4) {
            return b;
        }
        if (b[0] == 0) {
            return Arrays.copyOfRange(b, 4, b.length);
        } else {
            return b;
        }
    }
}
