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

import static android.widget.LinearLayout.LayoutParams.MATCH_PARENT;
import static android.widget.LinearLayout.LayoutParams.WRAP_CONTENT;
import static cc.ioctl.util.LayoutHelper.dip2px;
import static cc.ioctl.util.LayoutHelper.dip2sp;
import static cc.ioctl.util.LayoutHelper.newLinearLayoutParams;
import static cc.ioctl.util.Reflex.findField;
import static cc.ioctl.util.Reflex.getFirstByType;
import static io.github.qauxv.bridge.AppRuntimeHelper.getQQAppInterface;
import static io.github.qauxv.util.Initiator._PttItemBuilder;
import static io.github.qauxv.util.Initiator.load;
import static io.github.qauxv.util.xpcompat.XposedHelpers.findAndHookMethod;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import cc.hicore.QApp.QAppUtils;
import cc.ioctl.util.DebugUtils;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostStyledViewBuilder;
import cc.ioctl.util.Reflex;
import com.tencent.qqnt.kernel.nativeinterface.PttElement;
import com.xiaoniu.dispatcher.OnMenuBuilder;
import com.xiaoniu.util.ContextUtils;
import io.github.qauxv.R;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.bridge.ChatActivityFacade;
import io.github.qauxv.bridge.FaceImpl;
import io.github.qauxv.bridge.SessionInfoImpl;
import io.github.qauxv.config.ConfigManager;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.ui.CommonContextWrapper;
import io.github.qauxv.ui.CustomDialog;
import io.github.qauxv.ui.ResUtils;
import io.github.qauxv.util.CustomMenu;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.SyncUtils;
import io.github.qauxv.util.Toasts;
import io.github.qauxv.util.data.ContactDescriptor;
import io.github.qauxv.util.dexkit.AbstractQQCustomMenuItem;
import io.github.qauxv.util.dexkit.CDialogUtil;
import io.github.qauxv.util.dexkit.CFaceDe;
import io.github.qauxv.util.dexkit.DexKitTarget;
import io.github.qauxv.util.xpcompat.XC_MethodHook;
import io.github.qauxv.util.xpcompat.XposedBridge;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import kotlin.Unit;

@FunctionHookEntry
@UiItemAgentEntry
public class PttForwardHook extends CommonSwitchFunctionHook implements OnMenuBuilder {

    public static final String qn_cache_ptt_save_last_parent_dir = "qn_cache_ptt_save_last_parent_dir";
    public static final PttForwardHook INSTANCE = new PttForwardHook();

    @NonNull
    @Override
    public String getName() {
        return "语音转发及保存";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "长按语音消息可以转发或保存";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.MESSAGE_CATEGORY;
    }

    private PttForwardHook() {
        super(SyncUtils.PROC_MAIN, new DexKitTarget[]{CFaceDe.INSTANCE, CDialogUtil.INSTANCE, AbstractQQCustomMenuItem.INSTANCE});
    }

    @SuppressLint("SetTextI18n")
    private static void showSavePttFileDialog(Activity activity, final File ptt) {
        Context ctx = CommonContextWrapper.createAppCompatContext(activity);
        final EditText editText = new EditText(ctx);
        TextView tv = new TextView(ctx);
        tv.setText("格式为.slk/.amr 一般无法直接打开slk格式 而且大多数语音均为slk格式(转发语音可以看到格式) 请自行寻找软件进行转码");
        tv.setPadding(20, 10, 20, 10);
        String lastSaveDir = ConfigManager.getCache().getString(qn_cache_ptt_save_last_parent_dir);
        if (TextUtils.isEmpty(lastSaveDir)) {
            File f = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (f == null) {
                f = Environment.getExternalStorageDirectory();
            }
            lastSaveDir = f.getPath();
        }
        editText.setText(new File(lastSaveDir, DebugUtils.getPathTail(ptt)).getPath());
        editText.setTextSize(16);
        LinearLayout linearLayout = new LinearLayout(ctx);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(tv, MATCH_PARENT, WRAP_CONTENT);
        linearLayout.addView(editText, newLinearLayoutParams(MATCH_PARENT, WRAP_CONTENT));
        final AlertDialog alertDialog = new AlertDialog.Builder(ctx)
                .setTitle("输入保存路径(请自行转码)")
                .setView(linearLayout)
                .setPositiveButton("保存", null)
                .setNegativeButton("取消", null)
                .create();
        alertDialog.show();
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String path = editText.getText().toString();
                    if (path.equals("")) {
                        Toasts.error(ctx, "请输入路径");
                        return;
                    }
                    if (!path.startsWith("/")) {
                        Toasts.error(ctx, "请输入完整路径(以\"/\"开头)");
                        return;
                    }
                    File f = new File(path);
                    File dir = f.getParentFile();
                    if (dir == null || !dir.exists() || !dir.isDirectory()) {
                        Toasts.error(ctx, "文件夹不存在");
                        return;
                    }
                    if (!dir.canWrite()) {
                        Toasts.error(ctx, "文件夹无访问权限");
                        return;
                    }
                    FileOutputStream fout = null;
                    FileInputStream fin = null;
                    try {
                        if (!f.exists()) {
                            f.createNewFile();
                        }
                        fin = new FileInputStream(ptt);
                        fout = new FileOutputStream(f);
                        byte[] buf = new byte[1024];
                        int i;
                        while ((i = fin.read(buf)) > 0) {
                            fout.write(buf, 0, i);
                        }
                        fout.flush();
                        alertDialog.dismiss();
                        ConfigManager cache = ConfigManager.getCache();
                        String pdir = f.getParent();
                        if (pdir != null) {
                            cache.putString(qn_cache_ptt_save_last_parent_dir, pdir);
                            cache.save();
                        }
                    } catch (IOException e) {
                        Toasts.error(ctx, "失败:" + e.toString().replace("java.io.", ""));
                    } finally {
                        if (fin != null) {
                            try {
                                fin.close();
                            } catch (IOException ignored) {
                            }
                        }
                        if (fout != null) {
                            try {
                                fout.close();
                            } catch (IOException ignored) {
                            }
                        }
                    }
                });
    }

    @Override
    public boolean initOnce() throws Exception {
        Class<?> clz_ForwardBaseOption = load("com/tencent/mobileqq/forward/ForwardBaseOption");
        if (clz_ForwardBaseOption == null) {
            Class<?> clz_DirectForwardActivity = load(
                    "com/tencent/mobileqq/activity/DirectForwardActivity");
            for (Field f : clz_DirectForwardActivity.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                Class clz = f.getType();
                if (Modifier.isAbstract(clz.getModifiers()) && !clz.getName().contains("android")) {
                    clz_ForwardBaseOption = clz;
                    break;
                }
            }
        }
        Method buildConfirmDialog = null;
        for (Method m : clz_ForwardBaseOption.getDeclaredMethods()) {
            if (!m.getReturnType().equals(void.class)) {
                continue;
            }
            if (!Modifier.isFinal(m.getModifiers())) {
                continue;
            }
            if (m.getParameterTypes().length != 0) {
                continue;
            }
            buildConfirmDialog = m;
            break;
        }
        XposedBridge.hookMethod(buildConfirmDialog, new XC_MethodHook(51) {
            @SuppressLint("SetTextI18n")
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Field f;
                try {
                    f = findField(param.thisObject.getClass(), Bundle.class, "a");
                } catch (NoSuchFieldException ex) {
                    f = Reflex.getFirstNSFFieldByType(param.thisObject.getClass(), Bundle.class);
                }
                f.setAccessible(true);
                Bundle data = (Bundle) f.get(param.thisObject);
                if (!data.containsKey("ptt_forward_path")) {
                    return;
                }
                param.setResult(null);
                final String path = data.getString("ptt_forward_path");
                Activity ctx = getFirstByType(param.thisObject, Activity.class);
                if (path == null || !new File(path).exists()) {
                    Toasts.error(ctx, "Error: Invalid ptt file!");
                    return;
                }
                boolean multi;
                final ArrayList<ContactDescriptor> mTargets = new ArrayList<>();
                boolean unsupport = false;
                if (data.containsKey("forward_multi_target")) {
                    ArrayList targets = data.getParcelableArrayList("forward_multi_target");
                    if (targets.size() > 1) {
                        multi = true;
                        for (Object rr : targets) {
                            ContactDescriptor c = ContactDescriptor.parseResultRec(rr);
                            mTargets.add(c);
                        }
                    } else {
                        multi = false;
                        ContactDescriptor c = ContactDescriptor.parseResultRec(targets.get(0));
                        mTargets.add(c);
                    }
                } else {
                    multi = false;
                    ContactDescriptor cd = new ContactDescriptor();
                    cd.uin = data.getString("uin");
                    cd.uinType = data.getInt("uintype", -1);
                    cd.nick = data.getString("uinname");
                    if (cd.nick == null) {
                        cd.nick = data.getString("uin");
                    }
                    mTargets.add(cd);
                }
                if (unsupport) {
                    Toasts.info(ctx, "暂不支持我的设备/临时聊天/讨论组");
                }
                LinearLayout main = new LinearLayout(ctx);
                main.setOrientation(LinearLayout.VERTICAL);
                LinearLayout heads = new LinearLayout(ctx);
                heads.setGravity(Gravity.CENTER_VERTICAL);
                heads.setOrientation(LinearLayout.HORIZONTAL);
                View div = new View(ctx);
                div.setBackgroundColor(HostStyledViewBuilder.getColorSkinGray3());
                TextView tv = new TextView(ctx);
                tv.setText("[语音转发]" + path);
                tv.setTextColor(HostStyledViewBuilder.getColorSkinGray3());
                tv.setTextSize(dip2sp(ctx, 16));
                int pd = dip2px(ctx, 8);
                tv.setPadding(pd, pd, pd, pd);
                main.addView(heads, MATCH_PARENT, WRAP_CONTENT);
                main.addView(div, MATCH_PARENT, 1);
                main.addView(tv, MATCH_PARENT, WRAP_CONTENT);
                int w = dip2px(ctx, 40);
                LinearLayout.LayoutParams imglp = new LinearLayout.LayoutParams(w, w);
                imglp.setMargins(pd, pd, pd, pd);
                FaceImpl face = FaceImpl.getInstance();
                if (multi) {
                    if (mTargets != null) {
                        for (ContactDescriptor cd : mTargets) {
                            ImageView imgview = new ImageView(ctx);
                            Bitmap bm = face.getBitmapFromCache(
                                    cd.uinType == 1 ? FaceImpl.TYPE_TROOP : FaceImpl.TYPE_USER,
                                    cd.uin);
                            if (bm == null) {
                                imgview.setImageDrawable(ResUtils.loadDrawableFromAsset("face.png", ctx));
                                face.registerView(
                                        cd.uinType == 1 ? FaceImpl.TYPE_TROOP
                                                : FaceImpl.TYPE_USER,
                                        cd.uin, imgview);
                            } else {
                                imgview.setImageBitmap(bm);
                            }
                            heads.addView(imgview, imglp);
                        }
                    }
                } else {
                    ContactDescriptor cd = mTargets.get(0);
                    ImageView imgview = new ImageView(ctx);
                    Bitmap bm = face.getBitmapFromCache(cd.uinType == 1 ? FaceImpl.TYPE_TROOP
                            : FaceImpl.TYPE_USER, cd.uin);
                    if (bm == null) {
                        imgview.setImageDrawable(ResUtils.loadDrawableFromAsset("face.png", ctx));
                        face.registerView(
                                cd.uinType == 1 ? FaceImpl.TYPE_TROOP : FaceImpl.TYPE_USER,
                                cd.uin, imgview);
                    } else {
                        imgview.setImageBitmap(bm);
                    }
                    heads.setPadding(pd / 2, pd / 2, pd / 2, pd / 2);
                    TextView ni = new TextView(ctx);
                    ni.setText(cd.nick);
                    ni.setTextColor(HostStyledViewBuilder.getColorSkinBlack());
                    ni.setPadding(pd, 0, 0, 0);
                    ni.setTextSize(dip2sp(ctx, 17));
                    heads.addView(imgview, imglp);
                    heads.addView(ni);
                }
                CustomDialog dialog = CustomDialog.create(ctx);
                final Activity finalCtx = ctx;
                dialog.setPositiveButton("发送", (dialog1, which) -> {
                    try {
                        for (ContactDescriptor cd : mTargets) {
                            Parcelable sesssion = SessionInfoImpl.createSessionInfo(cd.uin, cd.uinType);
                            ChatActivityFacade.sendPttMessage(getQQAppInterface(), sesssion, path);
                        }
                        Toasts.success(finalCtx, "已发送");
                    } catch (Throwable e) {
                        Log.e(e);
                        try {
                            Toasts.error(finalCtx, "失败: " + e);
                        } catch (Throwable ignored) {
                            Toast.makeText(finalCtx, "失败: " + e, Toast.LENGTH_SHORT).show();
                        }
                    }
                    finalCtx.finish();
                });
                dialog.setNegativeButton("取消", null);
                dialog.setCancelable(false);
                dialog.setView(main);
                dialog.setTitle("发送给");
                dialog.show();
            }
        });

        if (QAppUtils.isQQnt()) {
            return true;
        }
        Class<?> kPttItemBuilder = _PttItemBuilder();
        findAndHookMethod(kPttItemBuilder, "a", int.class, Context.class,
                load("com/tencent/mobileqq/data/ChatMessage"), new XC_MethodHook(60) {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        int id = (int) param.args[0];
                        Activity context = (Activity) param.args[1];
                        Object chatMessage = param.args[2];
                        if (id == R.id.item_ptt_forward) {
                            param.setResult(null);
                            String url = (String) Reflex.invokeVirtual(chatMessage, "getLocalFilePath");
                            File file = new File(url);
                            if (!file.exists()) {
                                Toasts.error(context, "未找到语音文件");
                            } else {
                                sendForwardIntent(context, file);
                            }
                        } else if (id == R.id.item_ptt_save) {
                            param.setResult(null);
                            String url = (String) Reflex.invokeVirtual(chatMessage, "getLocalFilePath");
                            File file = new File(url);
                            if (!file.exists()) {
                                Toasts.error(context, "未找到语音文件");
                            } else {
                                showSavePttFileDialog(context, file);
                            }
                        }
                    }
                });
        for (Method m : kPttItemBuilder.getDeclaredMethods()) {
            if (!m.getReturnType().isArray()) {
                continue;
            }
            Class<?>[] ps = m.getParameterTypes();
            if (ps.length == 1 && ps[0].equals(View.class)) {
                HookUtils.hookAfterIfEnabled(INSTANCE, m, 60, param -> {
                    Object arr = param.getResult();
                    Class<?> clQQCustomMenuItem = arr.getClass().getComponentType();
                    Object ret;
                    Object item_forward = CustomMenu.createItem(clQQCustomMenuItem, R.id.item_ptt_forward, "转发");
                    Object item_save = CustomMenu.createItem(clQQCustomMenuItem, R.id.item_ptt_save, "保存");
                    ret = Array.newInstance(clQQCustomMenuItem, Array.getLength(arr) + 2);
                    Array.set(ret, 0, Array.get(arr, 0));
                    //noinspection SuspiciousSystemArraycopy
                    System.arraycopy(arr, 1, ret, 2, Array.getLength(arr) - 1);
                    Array.set(ret, 1, item_forward);
                    Array.set(ret, Array.getLength(ret) - 1, item_save);
                    param.setResult(ret);
                });
            }
        }
        return true;
    }

    private File getPttFileByMsgNt(Object msg) {
        try {
            Method getElement = null;
            for (Method m : msg.getClass().getDeclaredMethods()) {
                if (m.getReturnType() == PttElement.class) {
                    getElement = m;
                    break;
                }
            }
            PttElement element = (PttElement) getElement.invoke(msg);
            String filePath = element.getFilePath();
            return new File(filePath);
        } catch (Throwable e) {
            traceError(e);
            return new File("");
        }
    }

    private void sendForwardIntent(Context context, File file) {
        Intent intent = new Intent(context,
                load("com/tencent/mobileqq/activity/ForwardRecentActivity"));
        intent.putExtra("selection_mode", 0);
        intent.putExtra("direct_send_if_dataline_forward", false);
        intent.putExtra("forward_text", "null");
        intent.putExtra("ptt_forward_path", file.getPath());
        intent.putExtra("forward_type", -1);
        intent.putExtra("caller_name", "ChatActivity");
        intent.putExtra("k_smartdevice", false);
        intent.putExtra("k_dataline", false);
        intent.putExtra("k_forward_title", "语音转发");
        context.startActivity(intent);
    }

    @NonNull
    @Override
    public String[] getTargetComponentTypes() {
        return new String[]{
                "com.tencent.mobileqq.aio.msglist.holder.component.ptt.AIOPttContentComponent"
        };
    }

    @Override
    public void onGetMenuNt(@NonNull Object msg, @NonNull String componentType, @NonNull XC_MethodHook.MethodHookParam param) throws Exception {
        if (!isEnabled()) {
            return;
        }
        Activity context = ContextUtils.getCurrentActivity();
        Object item = CustomMenu.createItemIconNt(msg, "转发", R.drawable.ic_item_share_72dp, R.id.item_ptt_forward, () -> {
            File file = getPttFileByMsgNt(msg);
            if (!file.exists()) {
                Toasts.error(context, "未找到语音文件");
            } else {
                sendForwardIntent(context, file);
            }
            return Unit.INSTANCE;
        });
        Object item2 = CustomMenu.createItemIconNt(msg, "保存", R.drawable.ic_item_save_72dp, R.id.item_ptt_save, () -> {
            File file = getPttFileByMsgNt(msg);
            if (!file.exists()) {
                Toasts.error(context, "未找到语音文件");
            } else {
                showSavePttFileDialog(context, file);
            }
            return Unit.INSTANCE;
        });
        List list = (List) param.getResult();
        list.add(item);
        list.add(item2);
    }
}
