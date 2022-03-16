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

package io.github.qauxv.util.hookstatus;

import android.content.Context;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.startup.HookEntry;
import io.github.qauxv.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This is intended to be used in module process only.
 */
public class AbiUtils {

    private AbiUtils() {
        throw new AssertionError("This class is not intended to be instantiated");
    }

    @Nullable
    public static String getApplicationActiveAbi(@NonNull String packageName) {
        Context ctx = HostInfo.getApplication();
        PackageManager pm = ctx.getPackageManager();
        try {
            // find apk path
            String libDir = pm.getApplicationInfo(packageName, 0).nativeLibraryDir;
            if (libDir == null) {
                return null;
            }
            // find abi
            HashSet<String> abiList = new HashSet<>(4);
            for (String abi : new String[]{"arm", "arm64", "x86", "x86_64"}) {
                if (new File(libDir, abi).exists()) {
                    abiList.add(abi);
                } else if (libDir.endsWith(abi)) {
                    abiList.add(abi);
                }
            }
            if (abiList.isEmpty()) {
                return null;
            }
            // TODO: 2022-03-14 handle multi arch
            return abiList.iterator().next();
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("PackageManager.NameNotFoundException: " + e.getMessage());
            return null;
        }
    }

    @NonNull
    public static String[] queryModuleAbiList() {
        Context ctx = HostInfo.getApplication();
        String apkPath;
        if (HostInfo.isInModuleProcess()) {
            apkPath = ctx.getPackageCodePath();
        } else {
            apkPath = HookEntry.getModulePath();
        }
        try {
            return getApkAbiList(apkPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    public static String[] getApkAbiList(@NonNull String apkPath) throws IOException {
        ZipFile zipFile = new ZipFile(apkPath);
        HashSet<String> abiList = new HashSet<>(4);
        Enumeration<? extends ZipEntry> it = zipFile.entries();
        while (it.hasMoreElements()) {
            ZipEntry entry = it.nextElement();
            if (entry.getName().startsWith("lib/")) {
                String abi = entry.getName().substring(4, entry.getName().indexOf('/', 4));
                abiList.add(abi);
            }
        }
        zipFile.close();
        return abiList.toArray(new String[0]);
    }

    @NonNull
    public static String archStringToLibDirName(@NonNull String arch) {
        switch (arch) {
            case "x86":
            case "i386":
            case "i486":
            case "i586":
            case "i686":
                return "x86";
            case "x86_64":
            case "amd64":
                return "x86_64";
            case "arm":
            case "armhf":
            case "armeabi":
            case "armeabi-v7a":
                return "arm";
            case "aarch64":
            case "arm64":
            case "arm64-v8a":
            case "armv8l":
                return "arm64";
            default:
                throw new IllegalArgumentException("unsupported arch: " + arch);
        }
    }

    public static int archStringToArchInt(@NonNull String arch) {
        switch (arch) {
            case "arm":
                return 1;
            case "arm64":
                return 1 << 1;
            case "x86":
                return 1 << 2;
            case "x86_64":
                return 1 << 3;
            default:
                return 0;
        }
    }

    public static String archIntToArchString(int arch) {
        switch (arch) {
            case 1:
                return "arm32, armAll, universal";
            case 1 << 1:
                return "arm64, armAll, universal";
            case 1 << 1 + 1:
                return "armAll, universal";
            default:
                return "universal";
        }
    }
}
