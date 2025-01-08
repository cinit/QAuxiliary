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

import static io.github.qauxv.util.HostInfo.requireMinQQVersion;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import cc.ioctl.util.Reflex;
import cc.ioctl.util.ui.FaultyDialog;
import io.github.qauxv.R;
import io.github.qauxv.activity.ShadowShareFileAgentActivity;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.lifecycle.Parasitics;
import io.github.qauxv.ui.ResUtils;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.QQVersion;
import io.github.qauxv.util.SyncUtils;
import io.github.qauxv.util.Toasts;
import io.github.qauxv.util.dexkit.DefaultFileModel;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import io.github.qauxv.util.dexkit.FileBrowserActivity_InnerClass_onItemClick;
import io.github.qauxv.util.xpcompat.XC_MethodHook;
import io.github.qauxv.util.xpcompat.XposedBridge;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import kotlin.collections.ArraysKt;

@FunctionHookEntry
@UiItemAgentEntry
public class FileShareExtHook extends CommonSwitchFunctionHook {

    public static final FileShareExtHook INSTANCE = new FileShareExtHook();

    private FileShareExtHook() {
        super(SyncUtils.PROC_MAIN, new DexKitTarget[]{
                FileBrowserActivity_InnerClass_onItemClick.INSTANCE,
                DefaultFileModel.INSTANCE
        });
    }

    @NonNull
    @Override
    public String getName() {
        return "聊天文件分享其他应用";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "[QQ>=8.8.80]为聊天文件菜单添加分享至其他应用";
    }

    @Nullable
    @Override
    public String[] getExtraSearchKeywords() {
        return new String[]{"跨应用分享文件"};
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY;
    }

    @Override
    public boolean isAvailable() {
        return !HostInfo.isTim();
    }

    private static Class<?> kFileBrowserManager = null;

    @Override
    protected boolean initOnce() throws Exception {
        // share sheet stuff
        Class<?> kActionSheetItem = Initiator.loadClass("com.tencent.mobileqq.utils.ShareActionSheetBuilder$ActionSheetItem");
        Constructor<?> ctorActionSheetItem = kActionSheetItem.getDeclaredConstructor();
        Class<?> kiShareActionSheet = Initiator.loadClass("com.tencent.mobileqq.widget.share.ShareActionSheet");
        // args
        Class<?> kFileBrowserModelBase = Initiator.loadClass("com.tencent.mobileqq.filemanager.fileviewer.model.FileBrowserModelBase");
        Class<?> kDefaultFileModel = DexKit.requireClassFromCache(DefaultFileModel.INSTANCE);
        String fileViewerAdapterClassName;
        if (requireMinQQVersion(QQVersion.QQ_9_1_5_BETA_20015)) {
            fileViewerAdapterClassName = "com.tencent.mobileqq.filemanager.fileviewer.i";
        } else if (requireMinQQVersion(QQVersion.QQ_9_0_80)) {
            fileViewerAdapterClassName = "com.tencent.mobileqq.filemanager.fileviewer.h";
        } else if (requireMinQQVersion(QQVersion.QQ_9_0_15)) {
            fileViewerAdapterClassName = "com.tencent.mobileqq.filemanager.fileviewer.g";
        } else if (requireMinQQVersion(QQVersion.QQ_8_9_0)) {
            fileViewerAdapterClassName = "com.tencent.mobileqq.filemanager.fileviewer.h";
        } else {
            fileViewerAdapterClassName = "com.tencent.mobileqq.filemanager.fileviewer.IFileViewerAdapter";
        }
        Class<?> kIFileViewerAdapter = Initiator.loadClass(fileViewerAdapterClassName);
        kFileBrowserManager = Initiator.loadClassEither(
                "com.tencent.mobileqq.filemanager.fileviewer.FileBrowserManager",
                // QQ 8.9.0(3060)
                "com.tencent.mobileqq.filemanager.fileviewer.a"
        );
        Field fieldFileBrowserManager_FileBrowserModelBase = Reflex.findFirstDeclaredInstanceFieldByType(kFileBrowserManager, kFileBrowserModelBase);
        Field fieldDefaultFileModel_IFileViewerAdapter = Reflex.findFirstDeclaredInstanceFieldByType(kDefaultFileModel, kIFileViewerAdapter);
        fieldDefaultFileModel_IFileViewerAdapter.setAccessible(true);
        fieldFileBrowserManager_FileBrowserModelBase.setAccessible(true);
        Method getFilePath = kIFileViewerAdapter.getDeclaredMethod("getFilePath");
        Method getShareSheetItemLists1 = Reflex.findSingleMethod(kFileBrowserManager, ArrayList[].class, false);
        List<Method> getShareSheetItemLists2List = ArraysKt.filter(kDefaultFileModel.getDeclaredMethods(), it -> {
            return it.getParameterTypes().length == 0 && it.getReturnType() == ArrayList[].class;
        });
        String fileBrowserManagerItemClassName;
        if (requireMinQQVersion(QQVersion.QQ_9_0_15)) {
            fileBrowserManagerItemClassName = "com.tencent.mobileqq.filemanager.fileviewer.FileBrowserManager$c";
        } else if (requireMinQQVersion(QQVersion.QQ_8_9_0)) {
            fileBrowserManagerItemClassName = "com.tencent.mobileqq.filemanager.fileviewer.a$b";
        } else {
            fileBrowserManagerItemClassName = "com.tencent.mobileqq.filemanager.fileviewer.FileBrowserManager$2";
        }
        Method onItemClick = Initiator.loadClass(fileBrowserManagerItemClassName).getDeclaredMethod("onItemClick", kActionSheetItem, kiShareActionSheet);
        XposedBridge.hookMethod(onItemClick, mItemClickHandler);
        Method mFileBrowserActivity_InnerClass_onItemClick = DexKit.loadMethodFromCache(FileBrowserActivity_InnerClass_onItemClick.INSTANCE);
        if (mFileBrowserActivity_InnerClass_onItemClick != null) {
            XposedBridge.hookMethod(mFileBrowserActivity_InnerClass_onItemClick, mItemClickHandler);
        }
        XC_MethodHook getShareSheetItemListsHook = HookUtils.afterIfEnabled(this, param -> {
            ArrayList<Object>[] results = (ArrayList<Object>[]) param.getResult();
            if (results == null || results.length != 2) {
                Log.e("FileShareExtHook: getShareSheetItemLists result is null or length is not 2");
                return;
            }
            Object fileBrowserModel;
            if (kFileBrowserManager.isInstance(param.thisObject)) {
                fileBrowserModel = fieldFileBrowserManager_FileBrowserModelBase.get(param.thisObject);
            } else {
                fileBrowserModel = kFileBrowserModelBase.cast(param.thisObject);
            }
            if (fileBrowserModel == null) {
                return;
            }
            Object fileViewAdapter = fieldDefaultFileModel_IFileViewerAdapter.get(fileBrowserModel);
            if (fileViewAdapter == null) {
                return;
            }
            String filePath = (String) getFilePath.invoke(fileViewAdapter);
            if (TextUtils.isEmpty(filePath) || !new File(filePath).exists()) {
                return;
            }
            ArrayList<Object> row2 = results[1];
            // check if already added
            if (!row2.isEmpty()) {
                Object lastItem = row2.get(row2.size() - 1);
                int id = Reflex.getInstanceObject(lastItem, "id", int.class);
                if (id == R.id.ShareActionSheet_shareFileWithExtApp) {
                    // already added
                    return;
                }
            }
            Object item = ctorActionSheetItem.newInstance();
            Context ctx = Reflex.getFirstByType(param.thisObject, Activity.class);
            Parasitics.injectModuleResources(ctx.getResources());
            int drawableId = ResUtils.isInNightMode() ? R.drawable.ic_launch_28dp_night : R.drawable.ic_launch_28dp_light;
            Reflex.setInstanceObject(item, "id", int.class, R.id.ShareActionSheet_shareFileWithExtApp);
            Reflex.setInstanceObject(item, "icon", int.class, drawableId);
            Reflex.setInstanceObject(item, "label", String.class, "其他应用");
            Reflex.setInstanceObject(item, "argus", String.class, filePath);
            row2.add(item);
        });
        XposedBridge.hookMethod(getShareSheetItemLists1, getShareSheetItemListsHook);
        for (Method method : getShareSheetItemLists2List) {
            XposedBridge.hookMethod(method, getShareSheetItemListsHook);
        }
        return true;
    }

    private final XC_MethodHook mItemClickHandler = HookUtils.afterIfEnabled(this, param -> {
        Object item = param.args[0];
        int id = (int) Reflex.getInstanceObject(item, "id", int.class);
        if (id == R.id.ShareActionSheet_shareFileWithExtApp) {
            Context ctx = null;
            // case 1
            Object fileBrowserManager = Reflex.getFirstByTypeOrNull(param.thisObject, kFileBrowserManager);
            if (fileBrowserManager != null) {
                ctx = Reflex.getFirstByType(fileBrowserManager, Activity.class);
            }
            // case 2
            Class<?> kFileBrowserActivity = Initiator.load("com.tencent.mobileqq.filebrowser.FileBrowserActivity");
            if (kFileBrowserActivity != null) {
                Activity activity = (Activity) Reflex.getFirstByTypeOrNull(param.thisObject, kFileBrowserActivity);
                if (activity != null) {
                    ctx = activity;
                }
            }
            if (ctx == null) {
                Toasts.error(null, "unable to get activity");
                return;
            }
            assert ctx != null;
            String picPath = (String) Reflex.getInstanceObject(item, "argus", String.class);
            if (TextUtils.isEmpty(picPath)) {
                Toasts.error(ctx, "文件参数为空");
                return;
            }
            File file = new File(picPath);
            if (!file.exists()) {
                Toasts.error(ctx, "文件不存在");
                return;
            }
            Uri uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            String type = SharePicExtHook.guessMimeType(file);
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
}
