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

package io.github.qauxv.fragment;

import android.content.Context;
import android.system.Os;
import android.system.StructUtsname;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.github.qauxv.config.ConfigManager;
import io.github.qauxv.util.hookstatus.AbiUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class CheckAbiVariantModel {

    private CheckAbiVariantModel() {
        throw new AssertionError("no instance");
    }

    public static final String[] HOST_PACKAGES = new String[]{
            "com.tencent.mobileqq",
            "com.tencent.tim",
            "com.tencent.qqlite",
            "com.tencent.minihd.qq",
            // "com.tencent.mobileqqi",
    };

    @NonNull
    public static AbiInfo collectAbiInfo(@NonNull Context context) {
        AbiInfo abiInfo = new AbiInfo();
        StructUtsname uts = Os.uname();
        String sysAbi = uts.machine;
        abiInfo.sysArchName = sysAbi;
        abiInfo.sysArch = AbiUtils.archStringToArchInt(sysAbi);

        HashSet<String> requestAbis = new HashSet<>();
        requestAbis.add(AbiUtils.archStringToLibDirName(sysAbi));
        for (String pkg : HOST_PACKAGES) {
            String activeAbi = AbiUtils.getApplicationActiveAbi(pkg);
            if (activeAbi == null) {
                continue;
            }
            String abi = AbiUtils.archStringToLibDirName(activeAbi);
            if (!isPackageIgnored(pkg)) {
                requestAbis.add(abi);
            }
            AbiInfo.Package pi = new AbiInfo.Package();
            pi.abi = AbiUtils.archStringToArchInt(activeAbi);
            pi.ignored = isPackageIgnored(pkg);
            pi.packageName = pkg;
            abiInfo.packages.put(pkg, pi);
        }
        String[] modulesAbis = AbiUtils.queryModuleAbiList();
        HashSet<String> missingAbis = new HashSet<>();
        // check if modulesAbis contains all requestAbis
        for (String abi : requestAbis) {
            if (!Arrays.asList(modulesAbis).contains(abi)) {
                missingAbis.add(abi);
            }
        }
        abiInfo.isAbiMatch = missingAbis.isEmpty();
        int abi = 0;
        for (String name : requestAbis) {
            abi |= AbiUtils.archStringToArchInt(name);
        }
        abiInfo.suggestedApkAbiVariant = AbiUtils.getSuggestedAbiVariant(abi);
        return abiInfo;
    }

    public static void setPackageIgnored(@NonNull String packageName, boolean ignored) {
        ConfigManager cfg = ConfigManager.getDefaultConfig();
        cfg.putBoolean("native_lib_abi_ignore." + packageName, ignored);
    }

    public static boolean isPackageIgnored(@NonNull String packageName) {
        ConfigManager cfg = ConfigManager.getDefaultConfig();
        return cfg.getBoolean("native_lib_abi_ignore." + packageName, false);
    }

    public static class AbiInfo {

        public static class Package {

            public String packageName;
            public int abi;
            public boolean ignored;
        }

        @NonNull
        public Map<String, Package> packages = new HashMap<>();
        public String sysArchName;
        public int sysArch;
        public boolean isAbiMatch;
        @Nullable
        public String suggestedApkAbiVariant;
    }

}
