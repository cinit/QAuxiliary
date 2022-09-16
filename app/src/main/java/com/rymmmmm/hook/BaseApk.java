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
package com.rymmmmm.hook;

import static io.github.qauxv.util.QQVersion.QQ_8_6_0;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.dialog.RikkaBaseApkFormatDialog;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import io.github.qauxv.base.IUiItemAgent;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonConfigFunctionHook;
import io.github.qauxv.util.Initiator;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import kotlin.Unit;
import kotlin.jvm.functions.Function3;
import kotlinx.coroutines.flow.MutableStateFlow;
import io.github.qauxv.BuildConfig;

//重命名base.apk
@FunctionHookEntry
@UiItemAgentEntry
public class BaseApk extends CommonConfigFunctionHook {

    public static final BaseApk INSTANCE = new BaseApk();

    private BaseApk() {
        super();
    }

    @NonNull
    @Override
    public String getName() {
        return "重命名 base.apk";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.FILE_CATEGORY;
    }

    @Nullable
    @Override
    public MutableStateFlow<String> getValueState() {
        return null;
    }

    @NonNull
    @Override
    public Function3<IUiItemAgent, Activity, View, Unit> getOnUiItemClickListener() {
        return (agent, activity, view) -> {
            RikkaBaseApkFormatDialog dialog = new RikkaBaseApkFormatDialog();
            dialog.showDialog(activity);
            return Unit.INSTANCE;
        };
    }

    @Override
    public boolean initOnce() throws Exception {
        if (HostInfo.requireMinQQVersion(QQ_8_6_0)) {
            Class c = Initiator.load("com.tencent.mobileqq.utils.FileUtils");
            XposedHelpers.findAndHookMethod(c, "getFileName", String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        String fileName = (String) param.getResult();
                        String localFile = (String) param.args[0];
                        if (!localFile.contains("/")) {
                            // not a file path to upload, maybe someone has sent MessageForFile
                            return;
                        }
                        if (fileName.equals("base.apk")) {
                            PackageManager packageManager = HostInfo.getApplication().getPackageManager();
                            File file = new File(localFile);
                            if (!file.exists()) {
                                throw new FileNotFoundException("file not found: path='" + localFile + "', name='" + fileName + "'");
                            }
                            PackageInfo packageArchiveInfo = packageManager.getPackageArchiveInfo(localFile,
                                    PackageManager.GET_ACTIVITIES);
                            ApplicationInfo applicationInfo = packageArchiveInfo.applicationInfo;
                            applicationInfo.sourceDir = localFile;
                            applicationInfo.publicSourceDir = localFile;
                            String format = RikkaBaseApkFormatDialog.getCurrentBaseApkFormat();
                            if (format != null) {
                                String result = format
                                        .replace("%n", applicationInfo.loadLabel(packageManager).toString())
                                        .replace("%p", applicationInfo.packageName)
                                        .replace("%v", packageArchiveInfo.versionName)
                                        .replace("%c", String.valueOf(packageArchiveInfo.versionCode));
                                param.setResult(result);
                            }
                        }
                    } catch (Exception e) {
                        traceError(e);
                    }
                }
            });
            return true;
        }
        final Class<?> _ItemManagerClz = Initiator.load(
                "com.tencent.mobileqq.troop.utils.TroopFileTransferManager$Item");
        for (Method m : Initiator._TroopFileUploadMgr().getDeclaredMethods()) {
            if (Modifier.isPrivate(m.getModifiers()) && !Modifier.isStatic(m.getModifiers())
                    && m.getReturnType().equals(int.class)) {
                Class<?>[] argt = m.getParameterTypes();
                if (argt.length == 3 && argt[0] == long.class
                        && argt[1] == _ItemManagerClz && argt[2] == Bundle.class) {
                    HookUtils.hookBeforeIfEnabled(this, m, param -> {
                        Object item = param.args[1];
                        Field localFile = XposedHelpers.findField(_ItemManagerClz, "LocalFile");
                        Field fileName = XposedHelpers.findField(_ItemManagerClz, "FileName");
                        if (fileName.get(item).equals("base.apk")) {
                            PackageManager packageManager = HostInfo.getApplication().getPackageManager();
                            PackageInfo packageArchiveInfo = packageManager
                                    .getPackageArchiveInfo((String) localFile.get(item), PackageManager.GET_ACTIVITIES);
                            ApplicationInfo applicationInfo = packageArchiveInfo.applicationInfo;
                            applicationInfo.sourceDir = (String) localFile.get(item);
                            applicationInfo.publicSourceDir = (String) localFile.get(item);
                            String format = RikkaBaseApkFormatDialog.getCurrentBaseApkFormat();
                            if (format != null) {
                                String result = format
                                        .replace("%n", applicationInfo.loadLabel(packageManager).toString())
                                        .replace("%p", applicationInfo.packageName)
                                        .replace("%v", packageArchiveInfo.versionName)
                                        .replace("%c", String.valueOf(packageArchiveInfo.versionCode));
                                fileName.set(item, result);
                            }
                        }
                    });
                }
            }
        }
        return true;
    }

    @Override
    public boolean isEnabled() {
        return RikkaBaseApkFormatDialog.IsEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        // nop
    }
}
