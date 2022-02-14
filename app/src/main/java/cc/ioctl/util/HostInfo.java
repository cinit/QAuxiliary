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
package cc.ioctl.util;

import android.app.Application;
import androidx.annotation.NonNull;

/**
 * Helper class for getting host information. Keep it as simple as possible.
 */
public class HostInfo {

    public static final String PACKAGE_NAME_QQ = "com.tencent.mobileqq";
    public static final String PACKAGE_NAME_QQ_INTERNATIONAL = "com.tencent.mobileqqi";
    public static final String PACKAGE_NAME_QQ_LITE = "com.tencent.qqlite";
    public static final String PACKAGE_NAME_TIM = "com.tencent.tim";
    public static final String PACKAGE_NAME_SELF = "io.github.qauxv";

    private HostInfo() {
        throw new AssertionError("No instance for you!");
    }

    @NonNull
    public static Application getApplication() {
        return io.github.qauxv.util.HostInfo.getHostInfo().getApplication();
    }

    @NonNull
    public static String getPackageName() {
        return io.github.qauxv.util.HostInfo.getHostInfo().getPackageName();
    }

    @NonNull
    public static String getAppName() {
        return io.github.qauxv.util.HostInfo.getHostInfo().getHostName();
    }

    @NonNull
    public static String getVersionName() {
        return io.github.qauxv.util.HostInfo.getHostInfo().getVersionName();
    }

    public static int getVersionCode32() {
        return io.github.qauxv.util.HostInfo.getHostInfo().getVersionCode32();
    }

    public static int getVersionCode() {
        return getVersionCode32();
    }

    public static long getLongVersionCode() {
        return io.github.qauxv.util.HostInfo.getHostInfo().getVersionCode();
    }

    public static boolean isInModuleProcess() {
        return io.github.qauxv.util.HostInfo.isInModuleProcess();
    }

    public static boolean isTim() {
        return io.github.qauxv.util.HostInfo.isTim();
    }

    public static boolean isQQLite() {
        return PACKAGE_NAME_QQ_LITE.equals(getPackageName());
    }

    public static boolean isPlayQQ() {
        return !io.github.qauxv.util.HostInfo.isPlayQQ();
    }

    public static boolean isQQ() {
        //Improve this method when supporting more clients.
        return !io.github.qauxv.util.HostInfo.isTim();
    }

    public static boolean requireMinQQVersion(long versionCode) {
        return isQQ() && getLongVersionCode() >= versionCode;
    }

    public static boolean requireMinPlayQQVersion(long versionCode) {
        return isPlayQQ() && getLongVersionCode() >= versionCode;
    }

    public static boolean requireMinTimVersion(long versionCode) {
        return isTim() && getLongVersionCode() >= versionCode;
    }
}
