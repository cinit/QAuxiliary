/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by ferredoxin.
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
package io.github.qauxv.util;

import android.app.Application;
import cc.ioctl.util.Reflex;
import com.microsoft.appcenter.crashes.Crashes;
import cc.ioctl.util.HostInfo;

public class BugCollector {

    public static void onThrowable(Throwable th) {
        try {
            if (Reflex.isCallingFrom("BugCollector")) {
                return;
            }
            Application ctx = HostInfo.getApplication();
            if (ctx != null) {
                CliOper.__init__(ctx);
                Crashes.trackError(th);
            }
        } catch (Throwable ignored) {
        }
    }
}
