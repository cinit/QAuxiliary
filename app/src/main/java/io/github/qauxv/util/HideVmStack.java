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

import android.annotation.SuppressLint;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Method;
import java.util.ArrayList;

@SuppressLint("DiscouragedPrivateApi")
public class HideVmStack {

    private HideVmStack() {
        throw new AssertionError("No instance for you!");
    }

    private static volatile boolean inited = false;
    private static final StackTraceElement[] EMPTY_STACK_TRACE = new StackTraceElement[0];
    static boolean sHideEnabled = false;

    public static void setHideEnabled(boolean enabled) {
        HideVmStack.sHideEnabled = enabled;
        if (enabled) {
            init();
        }
    }

    public static boolean isHideEnabled() {
        return sHideEnabled;
    }

    private static void init() {
        if (inited) {
            return;
        }
        try {
            Method m = Throwable.class.getDeclaredMethod("getOurStackTrace");
            XposedBridge.hookMethod(m, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!sHideEnabled) {
                        return;
                    }
                    // don't call Throwable.getStackTrace() here, it will cause StackOverflow
                    StackTraceElement[] ste = (StackTraceElement[]) param.getResult();
                    if (ste != null) {
                        ArrayList<StackTraceElement> fakeSt = new ArrayList<>();
                        for (StackTraceElement e : ste) {
                            if (!e.getClassName().contains("io.github.qauxv.")) {
                                fakeSt.add(e);
                            }
                        }
                        param.setResult(fakeSt.toArray(EMPTY_STACK_TRACE));
                    }
                }
            });
        } catch (NoSuchMethodException e) {
            Log.e(e);
        }
        try {
            Method m = Class.forName("dalvik.system.VMStack")
                .getDeclaredMethod("getThreadStackTrace", Thread.class);
            XposedBridge.hookMethod(m, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!sHideEnabled) {
                        return;
                    }
                    // don't call Thread.getStackTrace() here, it will cause StackOverflow
                    StackTraceElement[] ste = (StackTraceElement[]) param.getResult();
                    if (ste != null) {
                        ArrayList<StackTraceElement> fakeSt = new ArrayList<>();
                        for (StackTraceElement e : ste) {
                            if (!e.getClassName().contains("io.github.qauxv.")) {
                                fakeSt.add(e);
                            }
                        }
                        param.setResult(fakeSt.toArray(EMPTY_STACK_TRACE));
                    }
                }
            });
        } catch (NoSuchMethodException | ClassNotFoundException e) {
            Log.e(e);
        }
        inited = true;
    }
}
