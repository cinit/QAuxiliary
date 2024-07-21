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
import io.github.qauxv.poststartup.StartupInfo;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

    public static final int ABI_ARM32 = 1;
    public static final int ABI_ARM64 = 1 << 1;
    public static final int ABI_X86 = 1 << 2;
    public static final int ABI_X86_64 = 1 << 3;

    @Nullable
    private static String sCachedModuleAbiFlavor;

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
            return null;
        }
    }

    @NonNull
    public static String getModuleFlavorName() {
        if (sCachedModuleAbiFlavor != null) {
            return sCachedModuleAbiFlavor;
        }
        String apkPath;
        if (HostInfo.isInHostProcess()) {
            apkPath = StartupInfo.getModulePath();
        } else {
            // self process
            apkPath = HostInfo.getApplication().getPackageCodePath();
        }
        if (!new File(apkPath).exists()) {
            throw new IllegalStateException("getModuleFlavorName, apk not found: " + apkPath);
        }
        String[] abis;
        try {
            abis = getApkAbiList(apkPath);
        } catch (Exception e) {
            throw new RuntimeException("getModuleFlavorName, getApkAbiList failed: " + e.getMessage(), e);
        }
        int abiFlags = 0;
        for (String abi : abis) {
            switch (abi) {
                case "armeabi-v7a":
                    abiFlags |= ABI_ARM32;
                    break;
                case "arm64-v8a":
                    abiFlags |= ABI_ARM64;
                    break;
                case "x86":
                    abiFlags |= ABI_X86;
                    break;
                case "x86_64":
                    abiFlags |= ABI_X86_64;
                    break;
                default:
                    throw new IllegalStateException("getModuleFlavorName, unknown abi: " + abi);
            }
        }
        if ((abiFlags & (ABI_ARM32 | ABI_ARM64 | ABI_X86 | ABI_X86_64)) == (ABI_ARM32 | ABI_ARM64 | ABI_X86 | ABI_X86_64)) {
            sCachedModuleAbiFlavor = "universal";
        } else if ((abiFlags & (ABI_ARM32 | ABI_ARM64)) == (ABI_ARM32 | ABI_ARM64)) {
            sCachedModuleAbiFlavor = "armAll";
        } else if (abiFlags == ABI_ARM32) {
            sCachedModuleAbiFlavor = "arm32";
        } else if (abiFlags == ABI_ARM64) {
            sCachedModuleAbiFlavor = "arm64";
        } else {
            sCachedModuleAbiFlavor = "unknown";
        }
        return sCachedModuleAbiFlavor;
    }

    @NonNull
    public static String[] queryModuleAbiList() {
        switch (getModuleFlavorName()) {
            case "arm32": {
                return new String[]{"arm"};
            }
            case "arm64": {
                return new String[]{"arm64"};
            }
            case "armAll": {
                return new String[]{"arm", "arm64"};
            }
            case "universal": {
                return new String[]{"arm", "arm64", "x86", "x86_64"};
            }
            default: {
                return new String[]{};
            }
        }
    }

    public static int getModuleABI() {
        int abi;
        switch (getModuleFlavorName()) {
            case "arm32": {
                abi = AbiUtils.ABI_ARM32;
                break;
            }
            case "arm64": {
                abi = AbiUtils.ABI_ARM64;
                break;
            }
            case "armAll": {
                abi = AbiUtils.ABI_ARM32 | AbiUtils.ABI_ARM64;
                break;
            }
            case "universal": {
                abi = AbiUtils.ABI_ARM32 | AbiUtils.ABI_ARM64 | AbiUtils.ABI_X86 | AbiUtils.ABI_X86_64;
                break;
            }
            default: {
                abi = 0;
            }
        }
        return abi;
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
            case "armv7l":
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
            case "arm32":
            case "armeabi":
            case "armeabi-v7a":
            case "armv7l":
                // actually, armv7l is ARMv8 CPU in 32-bit compatibility mode,
                // I don't know if we should throw armv7l into ABI_ARM64
                return ABI_ARM32;
            case "arm64":
            case "arm64-v8a":
            case "aarch64":
                return ABI_ARM64;
            case "x86":
            case "i386":
            case "i486":
            case "i586":
            case "i686":
                return ABI_X86;
            case "x86_64":
            case "amd64":
                return ABI_X86_64;
            default:
                return 0;
        }
    }

    public static String archIntToNames(int abi) {
        ArrayList<String> results = new ArrayList<>(4);
        if ((abi & ABI_ARM32) != 0) {
            results.add("armeabi-v7a");
        }
        if ((abi & ABI_ARM64) != 0) {
            results.add("arm64-v8a");
        }
        if ((abi & ABI_X86) != 0) {
            results.add("x86");
        }
        if ((abi & ABI_X86_64) != 0) {
            results.add("x86_64");
        }
        if (results.isEmpty()) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        for (String s : results) {
            sb.append(s).append('|');
        }
        return sb.substring(0, sb.length() - 1);
    }

    public static String getSuggestedAbiVariant(int requestedAbi) {
        if (requestedAbi == ABI_ARM32) {
            return "arm32";
        }
        if (requestedAbi == ABI_ARM64) {
            return "arm64";
        }
        if ((requestedAbi | ABI_ARM32 | ABI_ARM64) == (ABI_ARM32 | ABI_ARM64)) {
            return "armAll";
        }
        return "universal";
    }
}
