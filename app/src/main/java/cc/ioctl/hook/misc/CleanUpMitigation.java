/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
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

package cc.ioctl.hook.misc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.system.Os;
import androidx.annotation.Nullable;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.hook.BasePersistBackgroundHook;
import io.github.qauxv.util.IoUtils;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import kotlin.collections.SetsKt;

@FunctionHookEntry
public class CleanUpMitigation extends BasePersistBackgroundHook {

    private CleanUpMitigation() {
    }

    public static final CleanUpMitigation INSTANCE = new CleanUpMitigation();

    private HashSet<String> mPathPrefixList;
    private static final HashSet<String> FILES_TO_HIDE = SetsKt.hashSetOf(
            "qa_mmkv", "qa_misc", "qa_dyn_lib", "qa_target_dpi", ".tool",
            "xa_mmkv", "xa_conf", "xa_lib", "xa_log", "xa_daemon", "TGStickersExported"
    );

    @SuppressLint("SdCardPath")
    @Override
    protected boolean initOnce() {
        Context ctx = HostInfo.getApplication();
        String packageName = ctx.getPackageName();
        // UserHandle.PER_USER_RANGE
        int userHandleIndex = Os.geteuid() / 100000;
        // /data/user/{user:d}/{pkg:s}/files
        // /data/data/{pkg:s}/files
        // /sdcard/Android/data/{pkg:s}/files
        // /storage/emulated/{user:d}/Android/data/{pkg:s}/files
        // /storage/self/primary/Android/data/{pkg:s}/files
        mPathPrefixList = SetsKt.hashSetOf(
                "/data/user/" + userHandleIndex + "/" + packageName + "/files",
                "/data/data/" + packageName + "/files",
                "/sdcard/Android/data/" + packageName + "/files",
                "/storage/emulated/" + userHandleIndex + "/Android/data/" + packageName + "/files",
                "/storage/self/primary/Android/data/" + packageName + "/files"
        );
        // hook File.{list,listFiles} to hide files
        Method listMethod;
        Method listFilesMethod;
        try {
            listMethod = File.class.getDeclaredMethod("list");
            listFilesMethod = File.class.getDeclaredMethod("listFiles");
        } catch (NoSuchMethodException e) {
            throw IoUtils.unsafeThrow(e);
        }
        HookUtils.hookAfterAlways(this, listMethod, p -> {
            File thiz = (File) p.thisObject;
            String path = thiz.getAbsolutePath();
            if (p.hasThrowable()) {
                return;
            }
            String[] result = (String[]) p.getResult();
            if (mPathPrefixList.contains(path)) {
                p.setResult(filterList(result));
            }
        });
        HookUtils.hookAfterAlways(this, listFilesMethod, p -> {
            File thiz = (File) p.thisObject;
            String path = thiz.getAbsolutePath();
            if (p.hasThrowable()) {
                return;
            }
            File[] result = (File[]) p.getResult();
            if (mPathPrefixList.contains(path)) {
                p.setResult(filterListFiles(result));
            }
        });
        return true;
    }

    @Nullable
    private static File[] filterListFiles(@Nullable File[] files) {
        if (files == null || files.length == 0) {
            return files;
        }
        ArrayList<File> fileList = new ArrayList<>(files.length);
        for (File file : files) {
            if (file == null) {
                continue;
            }
            boolean hide = FILES_TO_HIDE.contains(file.getName());
            if (!hide) {
                fileList.add(file);
            }
        }
        return fileList.toArray(new File[0]);
    }

    @Nullable
    private static String[] filterList(@Nullable String[] files) {
        if (files == null || files.length == 0) {
            return files;
        }
        ArrayList<String> fileList = new ArrayList<>(files.length);
        for (String file : files) {
            if (file == null) {
                continue;
            }
            boolean hide = FILES_TO_HIDE.contains(file);
            if (!hide) {
                fileList.add(file);
            }
        }
        return fileList.toArray(new String[0]);
    }

}
