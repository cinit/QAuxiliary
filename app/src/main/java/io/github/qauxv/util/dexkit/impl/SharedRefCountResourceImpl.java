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

package io.github.qauxv.util.dexkit.impl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class SharedRefCountResourceImpl<R> {

    private R mResources = null;
    private final ReadWriteLock mReleaseLock = new ReentrantReadWriteLock();
    private final Object mLock = new Object();
    private int mRefCount = 0;

    public ReadWriteLock increaseRefCount() {
        synchronized (mLock) {
            if (mResources == null) {
                mResources = openResourceInternal();
            }
            mRefCount++;
            return mReleaseLock;
        }
    }

    @NonNull
    protected abstract R openResourceInternal();

    protected abstract void closeResourceInternal(@NonNull R res);

    public void decreaseRefCount() {
        synchronized (mLock) {
            mRefCount--;
            if (mRefCount == 0) {
                Lock lock = mReleaseLock.writeLock();
                lock.lock();
                try {
                    closeResourceInternal(mResources);
                    mResources = null;
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    @Nullable
    public R getResources() {
        return mResources;
    }

}
