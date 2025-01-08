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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcelable;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import cc.hicore.QApp.QAppUtils;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import cc.ioctl.util.Reflex;
import cc.ioctl.util.ui.FaultyDialog;
import io.github.qauxv.util.xpcompat.XC_MethodHook;
import io.github.qauxv.util.xpcompat.XposedBridge;
import io.github.qauxv.R;
import io.github.qauxv.activity.ShadowShareFileAgentActivity;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.lifecycle.Parasitics;
import io.github.qauxv.ui.ResUtils;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.IoUtils;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.SyncUtils;
import io.github.qauxv.util.Toasts;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import kotlin.collections.ArraysKt;
import kotlin.jvm.functions.Function3;

@FunctionHookEntry
@UiItemAgentEntry
public class SharePicExtHook extends CommonSwitchFunctionHook {

    public static final SharePicExtHook INSTANCE = new SharePicExtHook();

    private boolean mAioPictureViewV2ListenerHooked = false;

    private SharePicExtHook() {
        super(SyncUtils.PROC_MAIN | SyncUtils.PROC_PEAK);
    }

    @NonNull
    @Override
    public String getName() {
        return "聊天图片分享其他应用";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "[QQ>=8.8.50]为聊天图片分享菜单添加分享至其他应用";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY;
    }

    @Nullable
    @Override
    public String[] getExtraSearchKeywords() {
        return new String[]{"跨应用分享图片"};
    }

    @Override
    public boolean isAvailable() {
        return !HostInfo.isTim();
    }

    @Override
    protected boolean initOnce() throws Exception {
        Class<?> kAIOPictureView = Initiator.loadClass("com.tencent.mobileqq.richmediabrowser.view.AIOPictureView");
        Class<?> kAIOBrowserBaseView = Initiator.load("com.tencent.mobileqq.richmediabrowser.view.AIOBrowserBaseView");
        if (kAIOBrowserBaseView == null) {
            kAIOBrowserBaseView = kAIOPictureView.getSuperclass();
            assert kAIOBrowserBaseView != null;
        }
        Field contextOfAIOBrowserBaseView = Reflex.findFirstDeclaredInstanceFieldByType(kAIOBrowserBaseView, Context.class);
        Class<?> kiShareActionSheet = Initiator.loadClass("com.tencent.mobileqq.widget.share.ShareActionSheet");
        Class<?> kShareActionSheetProxy = Initiator.loadClassEither(
                "com.tencent.mobileqq.widget.share.ShareActionSheetProxy",
                // QQ 8.9.0(3060)
                "com.tencent.mobileqq.widget.share.c"
        );
        Field fProxyImpl = Reflex.findFirstDeclaredInstanceFieldByType(kShareActionSheetProxy, kiShareActionSheet);
        Class<?> kActionSheetItem = Initiator.loadClass("com.tencent.mobileqq.utils.ShareActionSheetBuilder$ActionSheetItem");
        Constructor<?> ctorActionSheetItem = kActionSheetItem.getDeclaredConstructor();
        Class<?> kShareActionSheetV2 = Initiator.loadClass("com.tencent.mobileqq.widget.share.ShareActionSheetV2");
        Class<?> kShareActionSheetImplV2 = Initiator.loadClassEither(
                "com.tencent.mobileqq.widget.share.ShareActionSheetImplV2",
                // QQ 8.9.0(3060)
                "com.tencent.mobileqq.widget.share.b"
        );
        Field fImplV2Impl = Reflex.findFirstDeclaredInstanceFieldByType(kShareActionSheetImplV2, kShareActionSheetV2);
        Field fV2ItemListArray = Reflex.findFirstDeclaredInstanceFieldByType(kShareActionSheetV2, List[].class);
        fV2ItemListArray.setAccessible(true);
        // action sheet listener
        Class<?> kiv2OnItemClickListener = Initiator.loadClass("com.tencent.mobileqq.widget.share.ShareActionSheet$OnItemClickListener");
        // using kv3OnItemClickListenerV2 = com.tencent.mobileqq.widget.share.ShareActionSheet.OnItemClickListenerV2
        Method miv2OnItemClick = kiv2OnItemClickListener.getDeclaredMethod("onItemClick", kActionSheetItem, kiShareActionSheet);
        Method showActionSheetForPic = Reflex.findSingleMethod(kAIOPictureView, void.class, false,
                Initiator.loadClass("com.tencent.mobileqq.richmediabrowser.model.AIOPictureData"),
                Initiator.loadClass("com.tencent.richmediabrowser.model.RichMediaBrowserInfo")
        );
        Field fieldV2Listener = Reflex.findFirstDeclaredInstanceFieldByType(kShareActionSheetV2, kiv2OnItemClickListener);
        Field fieldActionSheet = Reflex.findFirstDeclaredInstanceFieldByType(kAIOBrowserBaseView, kiShareActionSheet);
        // view model
        Class<?> kAIOPictureModel = Initiator.loadClassEither(
                "com.tencent.mobileqq.richmediabrowser.model.AIOPictureModel",
                // QQ 8.9.0(3060)
                "com.tencent.mobileqq.richmediabrowser.model.d"
        );
        Class<?> kAIOPictureData = Initiator.loadClass("com.tencent.mobileqq.richmediabrowser.model.AIOPictureData");
        Constructor<?> ctorAIOPictureModel = kAIOPictureModel.getConstructor();
        Method getPictureFile = Reflex.findMethodByTypes_1(kAIOPictureModel, File.class, kAIOPictureData, int.class);
        Function3<Object, Context, File, Void> fnInjectItemToShareSheet = (shareSheet, ctx, file) -> {
            try {
                Object actionSheet = shareSheet;
                if (actionSheet != null) {
                    if (kShareActionSheetProxy != null && kShareActionSheetProxy.isInstance(actionSheet)) {
                        actionSheet = fProxyImpl.get(actionSheet);
                    }
                    assert actionSheet != null;
                    if (kShareActionSheetImplV2.isInstance(actionSheet)) {
                        actionSheet = fImplV2Impl.get(actionSheet);
                    }
                    assert actionSheet != null;
                    // just make sure impl...
                    kShareActionSheetV2.cast(actionSheet);
                    if (!mAioPictureViewV2ListenerHooked) {
                        Object listener = fieldV2Listener.get(actionSheet);
                        assert listener != null;
                        Class<?> clazz = listener.getClass();
                        Method m = clazz.getDeclaredMethod(miv2OnItemClick.getName(), miv2OnItemClick.getParameterTypes());
                        XposedBridge.hookMethod(m, mItemClickHandler);
                        mAioPictureViewV2ListenerHooked = true;
                    }
                    List<Object>[] itemListArray = (List<Object>[]) fV2ItemListArray.get(actionSheet);
                    assert itemListArray != null;
                    List<Object> row2 = itemListArray[1];
                    assert row2 != null;
                    Object item = ctorActionSheetItem.newInstance();
                    Parasitics.injectModuleResources(ctx.getResources());
                    int drawableId = ResUtils.isInNightMode() ? R.drawable.ic_launch_28dp_night : R.drawable.ic_launch_28dp_light;
                    Reflex.setInstanceObject(item, "id", int.class, R.id.ShareActionSheet_sharePictureWithExtApp);
                    Reflex.setInstanceObject(item, "icon", int.class, drawableId);
                    Reflex.setInstanceObject(item, "label", String.class, "其他应用");
                    Reflex.setInstanceObject(item, "argus", String.class, file.getAbsolutePath());
                    row2.add(item);
                }
                return null;
            } catch (ReflectiveOperationException e) {
                IoUtils.unsafeThrow(e);
                return null;
            }
        };
        if (QAppUtils.isQQnt()) {
            Class<?> kNTShareActionManager = Initiator.loadClass("com.tencent.qqnt.aio.gallery.share.NTShareActionManager");
            Field itemsField = Reflex.findSingleField(kNTShareActionManager, ArrayList.class, false);
            itemsField.setAccessible(true);
            Method maybeShow = ArraysKt.single(kNTShareActionManager.getDeclaredMethods(), m -> {
                if (m.getReturnType() != void.class) {
                    return false;
                }
                Class<?>[] argt = m.getParameterTypes();
                if (argt.length != 1) {
                    return false;
                }
                Class<?> maybeNTShareContext = argt[0];
                return !maybeNTShareContext.isInterface() && Modifier.isFinal(maybeNTShareContext.getModifiers());
            });
            Class<?> kNTShareContext = maybeShow.getParameterTypes()[0];
            Class<?> kRFWLayerItemMediaInfo = Initiator.loadClass("com.tencent.richframework.gallery.bean.RFWLayerItemMediaInfo");
            Field layerItemInfoField = Reflex.findSingleField(kNTShareContext, kRFWLayerItemMediaInfo, false);
            layerItemInfoField.setAccessible(true);
            Field activityField = Reflex.findSingleField(kNTShareContext, Activity.class, false);
            activityField.setAccessible(true);
            Field actionSheetField = Reflex.findSingleField(kNTShareActionManager, kiShareActionSheet, false);
            actionSheetField.setAccessible(true);
            Method getExistSaveOrEditPath = kRFWLayerItemMediaInfo.getDeclaredMethod("getExistSaveOrEditPath");
            HookUtils.hookAfterIfEnabled(this, maybeShow, param -> {
                List<Object> secondLine = ((ArrayList<Object>) itemsField.get(param.thisObject));
                Object shareContext = param.args[0];
                Object layerItemInfo = layerItemInfoField.get(shareContext);
                Activity ctx = (Activity) activityField.get(shareContext);
                String path = (String) getExistSaveOrEditPath.invoke(layerItemInfo);
                if (TextUtils.isEmpty(path)) {
                    Toasts.error(ctx, "getExistSaveOrEditPath is empty");
                    return;
                }
                File file = new File(path);
                if (!file.exists()) {
                    Toasts.error(ctx, "file not exists");
                    return;
                }
                Object actionSheet = actionSheetField.get(param.thisObject);
                fnInjectItemToShareSheet.invoke(actionSheet, ctx, file);
            });
        }
        HookUtils.hookAfterIfEnabled(this, showActionSheetForPic, param -> {
            Object actionSheet = fieldActionSheet.get(param.thisObject);
            Context ctx = (Context) contextOfAIOBrowserBaseView.get(param.thisObject);
            Parcelable picData = (Parcelable) param.args[0];
            assert picData != null;
            File picFile = (File) getPictureFile.invoke(ctorAIOPictureModel.newInstance(), picData, 4);
            if (picFile == null) {
                picFile = (File) getPictureFile.invoke(ctorAIOPictureModel.newInstance(), picData, 2);
            }
            if (picFile == null || !picFile.exists()) {
                // unable to get picture file
                return;
            }
            assert ctx != null;
            fnInjectItemToShareSheet.invoke(actionSheet, ctx, picFile);
        });
        return true;
    }

    private final XC_MethodHook mItemClickHandler = HookUtils.afterIfEnabled(this, param -> {
        Object item = param.args[0];
        int id = (int) Reflex.getInstanceObject(item, "id", int.class);
        if (id == R.id.ShareActionSheet_sharePictureWithExtApp) {
            Context ctx = null;
            Class<?> kNTAIOLayerMorePart = Initiator.load("com.tencent.qqnt.aio.gallery.part.NTAIOLayerMorePart");
            if (kNTAIOLayerMorePart != null && kNTAIOLayerMorePart.isInstance(param.thisObject)) {
                // NT
                Field activityField = Reflex.findSingleField(kNTAIOLayerMorePart, Activity.class, true);
                activityField.setAccessible(true);
                ctx = (Context) activityField.get(param.thisObject);
            } else {
                // older
                Class<?> kAIOPictureView = Initiator.loadClass("com.tencent.mobileqq.richmediabrowser.view.AIOPictureView");
                Class<?> kAIOBrowserBaseView = Initiator.load("com.tencent.mobileqq.richmediabrowser.view.AIOBrowserBaseView");
                if (kAIOBrowserBaseView == null) {
                    kAIOBrowserBaseView = kAIOPictureView.getSuperclass();
                    assert kAIOBrowserBaseView != null;
                }
                Field contextOfAIOBrowserBaseView = Reflex.findFirstDeclaredInstanceFieldByType(kAIOBrowserBaseView, Context.class);
                ctx = (Context) contextOfAIOBrowserBaseView.get(Reflex.getFirstByType(param.thisObject, kAIOPictureView));
            }
            if (ctx == null) {
                Toasts.error(null, "unable to get activity");
                return;
            }
            assert ctx != null;
            String picPath = (String) Reflex.getInstanceObject(item, "argus", String.class);
            if (TextUtils.isEmpty(picPath)) {
                Toasts.error(ctx, "图片参数为空");
                return;
            }
            File file = new File(picPath);
            if (!file.exists()) {
                Toasts.error(ctx, "图片不存在");
                return;
            }
            Uri uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            String type = guessMimeType(file);
            if (type == null) {
                type = "application/octet-stream";
            }
            Log.d("type=" + type + ", uri=" + uri);
            intent.setType(type);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                ShadowShareFileAgentActivity.startShareFileActivity(ctx, intent, file, true);
            } catch (ActivityNotFoundException e) {
                FaultyDialog.show(ctx, e);
            }
        }
    });

    @Nullable
    public static String guessMimeType(@NonNull File file) {
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            return URLConnection.guessContentTypeFromStream(in);
        } catch (IOException e) {
            return null;
        }
    }
}
