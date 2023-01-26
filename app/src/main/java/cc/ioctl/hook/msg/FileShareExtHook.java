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
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import cc.ioctl.util.Reflex;
import cc.ioctl.util.ui.FaultyDialog;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
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
import io.github.qauxv.util.SyncUtils;
import io.github.qauxv.util.Toasts;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

@FunctionHookEntry
@UiItemAgentEntry
public class FileShareExtHook extends CommonSwitchFunctionHook {

    public static final FileShareExtHook INSTANCE = new FileShareExtHook();

    private FileShareExtHook() {
        super(SyncUtils.PROC_MAIN);
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
        Class<?> kDefaultFileModel = Initiator.loadClassEither(
                "com.tencent.mobileqq.filemanager.fileviewer.model.DefaultFileModel",
                // QQ 8.9.0(3060)
                "com.tencent.mobileqq.filemanager.fileviewer.model.b"
        );
        Class<?> kIFileViewerAdapter = Initiator.loadClassEither(
                "com.tencent.mobileqq.filemanager.fileviewer.IFileViewerAdapter",
                // QQ 8.9.0(3060)
                "com.tencent.mobileqq.filemanager.fileviewer.h"
        );
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
        Method getShareSheetItemLists = Reflex.findSingleMethod(kFileBrowserManager, ArrayList[].class, false);
        Method onItemClick = Initiator.loadClassEither(
                "com.tencent.mobileqq.filemanager.fileviewer.FileBrowserManager$2",
                // QQ 8.9.0(3060)
                "com.tencent.mobileqq.filemanager.fileviewer.a$b"
        ).getDeclaredMethod("onItemClick", kActionSheetItem, kiShareActionSheet);
        XposedBridge.hookMethod(onItemClick, mItemClickHandler);
        HookUtils.hookAfterIfEnabled(this, getShareSheetItemLists, param -> {
            ArrayList<Object>[] results = (ArrayList<Object>[]) param.getResult();
            if (results == null || results.length != 2) {
                Log.e("FileShareExtHook: getShareSheetItemLists result is null or length is not 2");
                return;
            }
            Object fileBrowserModel = fieldFileBrowserManager_FileBrowserModelBase.get(param.thisObject);
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
        return true;
    }

    private final XC_MethodHook mItemClickHandler = HookUtils.afterIfEnabled(this, param -> {
        Object item = param.args[0];
        int id = (int) Reflex.getInstanceObject(item, "id", int.class);
        if (id == R.id.ShareActionSheet_shareFileWithExtApp) {
            Object fileBrowserManager = Reflex.getFirstByType(param.thisObject, kFileBrowserManager);
            Context ctx = Reflex.getFirstByType(fileBrowserManager, Activity.class);
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
