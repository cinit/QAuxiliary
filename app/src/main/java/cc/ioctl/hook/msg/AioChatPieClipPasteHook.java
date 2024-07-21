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

import android.content.ClipData;
import android.content.ClipData.Item;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import cc.hicore.message.bridge.Chat_facade_bridge;
import cc.ioctl.util.SendCacheUtils;
import cc.ioctl.util.ui.FaultyDialog;
import io.github.qauxv.util.xpcompat.XC_MethodHook;
import io.github.qauxv.util.xpcompat.XposedBridge;
import io.github.qauxv.R;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.bridge.FaceImpl;
import io.github.qauxv.bridge.SessionInfoImpl;
import io.github.qauxv.databinding.DialogConfirmSendPictureBinding;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.router.decorator.IBaseChatPieInitDecorator;
import io.github.qauxv.router.dispacher.InputButtonHookDispatcher;
import io.github.qauxv.ui.CommonContextWrapper;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.SyncUtils;
import io.github.qauxv.util.dexkit.DexKitTarget;
import io.github.qauxv.util.dexkit.NBaseChatPie_init;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import kotlin.Pair;
import mqq.app.AppRuntime;

@FunctionHookEntry
@UiItemAgentEntry
public class AioChatPieClipPasteHook extends CommonSwitchFunctionHook implements IBaseChatPieInitDecorator {

    public static final AioChatPieClipPasteHook INSTANCE = new AioChatPieClipPasteHook();
    private static final String MIME_IMAGE = "image/*";

    public interface IOnContextMenuItemCallback {

        boolean onInterceptContextMenuItem(@NonNull EditText editText, int i);
    }

    private AioChatPieClipPasteHook() {
        super(SyncUtils.PROC_MAIN, new DexKitTarget[]{NBaseChatPie_init.INSTANCE});
    }

    @NonNull
    @Override
    public String getName() {
        return "支持聊天窗口粘贴图片";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY;
    }

    @Override
    protected boolean initOnce() throws Exception {
        // com.tencent.widget.XEditTextEx#onTextContextMenuItem(I)Z
        Method m = Initiator.loadClass("com.tencent.widget.XEditTextEx")
                .getDeclaredMethod("onTextContextMenuItem", int.class);
        XposedBridge.hookMethod(m, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                EditText editText = (EditText) param.thisObject;
                int id = (int) param.args[0];
                Object cb = editText.getTag(R.id.XEditTextEx_onTextContextMenuItemInterceptor);
                if (cb instanceof IOnContextMenuItemCallback) {
                    IOnContextMenuItemCallback callback = (IOnContextMenuItemCallback) cb;
                    boolean result = callback.onInterceptContextMenuItem(editText, id);
                    if (result) {
                        param.setResult(true);
                    }
                }
            }
        });
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            Method m2 = Initiator.loadClass("com.tencent.widget.XEditText")
                    .getDeclaredMethod("onCreateInputConnection", EditorInfo.class);
            XposedBridge.hookMethod(m2, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    EditText editText = (EditText) param.thisObject;
                    EditorInfo outAttrs = (EditorInfo) param.args[0];
                    InputConnection ic = (InputConnection) param.getResult();

                    // On SDK 30 and below, we manually configure the InputConnection here to use
                    // ViewCompat.performReceiveContent. On S and above, the platform's BaseInputConnection
                    // implementation calls View.performReceiveContent by default.
                    String[] mimeTypes = ViewCompat.getOnReceiveContentMimeTypes(editText);
                    if (mimeTypes != null) {
                        EditorInfoCompat.setContentMimeTypes(outAttrs, mimeTypes);
                        ic = InputConnectionCompat.createWrapper(editText, ic, outAttrs);
                    }
                    param.setResult(ic);
                }
            });
        }
        // init required dispatcher
        return InputButtonHookDispatcher.INSTANCE.initialize();
    }

    @Override
    public void onInitBaseChatPie(@NonNull Object baseChatPie, @NonNull ViewGroup aioRootView,
            @Nullable Parcelable session, @NonNull Context ctx, @NonNull AppRuntime rt) {
        int inputTextId = ctx.getResources().getIdentifier("input", "id", ctx.getPackageName());
        EditText input = aioRootView.findViewById(inputTextId);
        Objects.requireNonNull(input, "onInitBaseChatPie: findViewById R.id.input is null");
        ViewCompat.setOnReceiveContentListener(input, new String[]{MIME_IMAGE}, (view, payload) -> {
            ClipData clipData = payload.getClip();
            Pair<ClipDescription, Item> item = getClipDataItem0(clipData);
            if (item != null && item.getFirst().hasMimeType(MIME_IMAGE)) {
                Uri uri = item.getSecond().getUri();
                if (uri != null && "content".equals(uri.getScheme())) {
                    handleSendUriPicture(ctx, session == null ? InputButtonHookDispatcher.INSTANCE.getSessionByAIOParam() : session, uri, aioRootView, rt);
                    return null;
                }
            }
            return payload;
        });
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            input.setTag(R.id.XEditTextEx_onTextContextMenuItemInterceptor, (IOnContextMenuItemCallback) (editText, i) -> {
                if (i == android.R.id.paste) {
                    Pair<ClipDescription, Item> item = getClipDataItem0(getPrimaryClip(ctx));
                    if (item != null && item.getFirst().hasMimeType(MIME_IMAGE)) {
                        Uri uri = item.getSecond().getUri();
                        if (uri != null && "content".equals(uri.getScheme())) {
                            handleSendUriPicture(ctx, session == null ? InputButtonHookDispatcher.INSTANCE.getSessionByAIOParam() : session, uri, aioRootView, rt);
                            return true;
                        }
                    }
                }
                return false;
            });
        }
    }

    @Nullable
    private static ClipData getPrimaryClip(@NonNull Context ctx) {
        ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            return clipboard.getPrimaryClip();
        }
        return null;
    }

    @Nullable
    private static Pair<ClipDescription, Item> getClipDataItem0(@Nullable ClipData clip) {
        // only support 1 item
        if (clip != null && clip.getItemCount() == 1) {
            ClipDescription desc = clip.getDescription();
            Item item = clip.getItemAt(0);
            if (desc != null && item != null) {
                return new Pair<>(desc, item);
            }
        }
        return null;
    }

    private static void handleSendUriPicture(@NonNull Context ctx, @NonNull Parcelable session, @NonNull Uri uri,
            @NonNull ViewGroup aioRootView, @NonNull AppRuntime rt) {
        AtomicBoolean cpTimeout = new AtomicBoolean(true);
        // call ContentResolver#openInputStream(Uri) asynchronously to avoid blocking UI thread
        SyncUtils.async(() -> {
            byte[] data = null;
            try (InputStream is = ctx.getContentResolver().openInputStream(uri)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                data = baos.toByteArray();
                cpTimeout.set(false);
            } catch (Exception e) {
                FaultyDialog.show(ctx, e);
            }
            cpTimeout.set(false);
            if (data != null) {
                byte[] finalData = data;
                SyncUtils.runOnUiThread(() -> confirmSendMessage(ctx, session, finalData, aioRootView, rt));
            }
        });
        SyncUtils.postDelayed(() -> {
            if (cpTimeout.get()) {
                FaultyDialog.show(ctx, "ContentProvider 超时", uri.toString());
            }
        }, 3000);
    }

    private static void confirmSendMessage(@NonNull Context context, @NonNull Parcelable session, @NonNull byte[] data,
            @NonNull ViewGroup aioRootView, @NonNull AppRuntime rt) {
        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        } catch (Exception e) {
            FaultyDialog.show(context, e);
            return;
        }
        if (bitmap == null) {
            FaultyDialog.show(context, "解码图片失败", "BitmapFactory.decodeByteArray 返回 null");
            return;
        }
        int uinType = SessionInfoImpl.getUinType(session);
        String uin = SessionInfoImpl.getUin(session);
        if (uinType != 0 && uinType != 1 || TextUtils.isEmpty(uin)) {
            FaultyDialog.show(context, "不支持的目标", "uinType = " + uinType + ", uin = " + uin);
            return;
        }
        Context ctx = CommonContextWrapper.createAppCompatContext(context);
        DialogConfirmSendPictureBinding binding = DialogConfirmSendPictureBinding.inflate(LayoutInflater.from(ctx));
        binding.ivPicture.setImageBitmap(bitmap);
        binding.tvName.setText(uin);
        FaceImpl.getInstance().setImageOrRegister(uinType == 1 ? FaceImpl.TYPE_TROOP : FaceImpl.TYPE_USER, uin, binding.ivAvatar);
        new AlertDialog.Builder(ctx)
                .setTitle("发送给：")
                .setView(binding.getRoot())
                .setPositiveButton("发送", (dialog, which) -> executeSendMessage(context, session, data, aioRootView, rt))
                .setNegativeButton(android.R.string.cancel, null)
                .setCancelable(true)
                .show();
    }

    private static void executeSendMessage(@NonNull Context context, @NonNull Parcelable session, @NonNull byte[] data,
            @NonNull ViewGroup aioRootView, @NonNull AppRuntime rt) {
        try {
            File file = SendCacheUtils.saveAsCacheFile(context, data);
            Chat_facade_bridge.sendPic(session, file);
        } catch (IOException e) {
            FaultyDialog.show(context, e);
        }
    }
}
