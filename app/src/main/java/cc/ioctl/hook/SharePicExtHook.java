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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcelable;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.Reflex;
import cc.ioctl.util.ui.FaultyDialog;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import io.github.qauxv.R;
import io.github.qauxv.SyncUtils;
import io.github.qauxv.activity.ShadowShareFileAgentActivity;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.lifecycle.Parasitics;
import io.github.qauxv.ui.ResUtils;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.Toasts;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLConnection;
import java.util.List;

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
        return FunctionEntryRouter.Locations.Simplify.UI_CHAT_MSG;
    }

    @Override
    protected boolean initOnce() throws Exception {
        Class<?> kAIOPictureView = Initiator.loadClass("com.tencent.mobileqq.richmediabrowser.view.AIOPictureView");
        Class<?> kAIOBrowserBaseView = Initiator.loadClass("com.tencent.mobileqq.richmediabrowser.view.AIOBrowserBaseView");
        Field contextOfAIOBrowserBaseView = Reflex.findFirstDeclaredInstanceFieldByType(kAIOBrowserBaseView, Context.class);
        Class<?> kiShareActionSheet = Initiator.loadClass("com.tencent.mobileqq.widget.share.ShareActionSheet");
        Class<?> kShareActionSheetProxy = Initiator.loadClass("com.tencent.mobileqq.widget.share.ShareActionSheetProxy");
        Field fProxyImpl = Reflex.findFirstDeclaredInstanceFieldByType(kShareActionSheetProxy, kiShareActionSheet);
        Class<?> kActionSheetItem = Initiator.loadClass("com.tencent.mobileqq.utils.ShareActionSheetBuilder$ActionSheetItem");
        Constructor<?> ctorActionSheetItem = kActionSheetItem.getDeclaredConstructor();
        Class<?> kShareActionSheetV2 = Initiator.loadClass("com.tencent.mobileqq.widget.share.ShareActionSheetV2");
        Class<?> kShareActionSheetImplV2 = Initiator.loadClass("com.tencent.mobileqq.widget.share.ShareActionSheetImplV2");
        Field fImplV2Impl = Reflex.findFirstDeclaredInstanceFieldByType(kShareActionSheetImplV2, kShareActionSheetV2);
        Field fV2ItemListArray = Reflex.findFirstDeclaredInstanceFieldByType(kShareActionSheetV2, List[].class);
        fV2ItemListArray.setAccessible(true);
        // action sheet listener
        Class<?> kiv2OnItemClickListener = Initiator.loadClass("com.tencent.mobileqq.widget.share.ShareActionSheet$OnItemClickListener");
        // using kv3OnItemClickListenerV2 = com.tencent.mobileqq.widget.share.ShareActionSheet.OnItemClickListenerV2
        Method miv2OnItemClick = kiv2OnItemClickListener.getDeclaredMethod("onItemClick", kActionSheetItem, kiShareActionSheet);
        Method showActionSheetForPic = kAIOPictureView.getDeclaredMethod("a",
                Initiator.loadClass("com.tencent.mobileqq.richmediabrowser.model.AIOPictureData"),
                Initiator.loadClass("com.tencent.richmediabrowser.model.RichMediaBrowserInfo")
        );
        Field fieldV2Listener = Reflex.findFirstDeclaredInstanceFieldByType(kShareActionSheetV2, kiv2OnItemClickListener);
        Field fieldActionSheet = Reflex.findFirstDeclaredInstanceFieldByType(kAIOBrowserBaseView, kiShareActionSheet);
        // view model
        Class<?> kAIOPictureModel = Initiator.loadClass("com.tencent.mobileqq.richmediabrowser.model.AIOPictureModel");
        Class<?> kAIOPictureData = Initiator.loadClass("com.tencent.mobileqq.richmediabrowser.model.AIOPictureData");
        Constructor<?> ctorAIOPictureModel = kAIOPictureModel.getConstructor();
        Method getPictureFile = Reflex.findMethodByTypes_1(kAIOPictureModel, File.class, kAIOPictureData, int.class);
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
            if (actionSheet != null) {
                if (kShareActionSheetProxy.isInstance(actionSheet)) {
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
                Reflex.setInstanceObject(item, "argus", String.class, picFile.getAbsolutePath());
                row2.add(item);
            }
        });
        return true;
    }

    private final XC_MethodHook mItemClickHandler = HookUtils.afterIfEnabled(this, param -> {
        Object item = param.args[0];
        int id = (int) Reflex.getInstanceObject(item, "id", int.class);
        if (id == R.id.ShareActionSheet_sharePictureWithExtApp) {
            Class<?> kAIOPictureView = Initiator.loadClass("com.tencent.mobileqq.richmediabrowser.view.AIOPictureView");
            Class<?> kAIOBrowserBaseView = Initiator.loadClass("com.tencent.mobileqq.richmediabrowser.view.AIOBrowserBaseView");
            Field contextOfAIOBrowserBaseView = Reflex.findFirstDeclaredInstanceFieldByType(kAIOBrowserBaseView, Context.class);
            Context ctx = (Context) contextOfAIOBrowserBaseView.get(Reflex.getFirstByType(param.thisObject, kAIOPictureView));
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
                ShadowShareFileAgentActivity.startShareFileActivity(ctx, intent, file);
            } catch (ActivityNotFoundException e) {
                FaultyDialog.show(ctx, e);
            }
        }
    });

    @Nullable
    private static String guessMimeType(@NonNull File file) {
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            return URLConnection.guessContentTypeFromStream(in);
        } catch (IOException e) {
            return null;
        }
    }
}
