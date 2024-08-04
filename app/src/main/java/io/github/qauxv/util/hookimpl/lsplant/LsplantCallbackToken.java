/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2024 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is an opensource software: you can redistribute it
 * and/or modify it under the terms of the General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the General Public License for more details.
 *
 * You should have received a copy of the General Public License
 * along with this software.
 * If not, see
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package io.github.qauxv.util.hookimpl.lsplant;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Objects;

public final class LsplantCallbackToken {

    // WARNING: This will only work for Android 7.0 and above.
    // Since SDK 24, Method.equals() and Method.hashCode() can correctly compare hooked methods.
    // Before SDK 24, equals() uses AbstractMethod which is not safe for hooked methods.
    // If you need to support lower versions, go and read cs.android.com.

    /*package*/ LsplantCallbackToken(@NonNull Member target) {
        mMember = target;
    }

    private Member mMember;
    // LSPlant backup is always a method, regardless of the target type being method or constructor
    private Method mBackup;

    /*package*/ void setBackupMember(@NonNull Method backup) {
        Objects.requireNonNull(backup);
        if (mBackup != null) {
            throw new IllegalStateException("Backup member already set");
        }
        mBackup = backup;
    }

    public Method getBackupMember() {
        return mBackup;
    }

    public Member getTargetMember() {
        return mMember;
    }

    // called from native
    @Keep
    public Object callback(Object[] args) throws Throwable {
        Member targetMethod = mMember;
        Method backupMethod = mBackup;
        if (targetMethod == null) {
            throw new AssertionError("targetMethod is null");
        }
        if (backupMethod == null) {
            throw new AssertionError("backupMethod is null");
        }
        return LsplantCallbackDispatcher.handleCallback(this, targetMethod, backupMethod, args);
    }

}
