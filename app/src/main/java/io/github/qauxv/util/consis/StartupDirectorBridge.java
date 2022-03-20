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

package io.github.qauxv.util.consis;

import android.app.Activity;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.tencent.common.app.BaseApplicationImpl;
import io.github.qauxv.SyncUtils;
import io.github.qauxv.activity.BaseActivity;
import io.github.qauxv.util.HostInfo;
import io.github.qauxv.util.Log;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.HashSet;
import mqq.app.MobileQQ;

/**
 * Transaction with host's startup director
 *
 * @author cinit
 */
public class StartupDirectorBridge {

    private StartupDirectorBridge() {
        initialize();
    }

    private static StartupDirectorBridge sInstance = null;

    @NonNull
    public static StartupDirectorBridge getInstance() {
        if (sInstance == null) {
            sInstance = new StartupDirectorBridge();
        }
        return sInstance;
    }

    private final HashSet<WeakReference<BaseActivity>> mSuspendedActivities = new HashSet<>(2);
    private boolean mNeedInterceptStartActivity = false;
    private boolean mStartupFinished = false;
    private boolean mProbeStarted = false;
    private Field mDirectorField;

    private void initialize() {
        if (HostInfo.isInModuleProcess() || !SyncUtils.isMainProcess()) {
            mNeedInterceptStartActivity = false;
            return;
        }
        // only in host main process
        MobileQQ mqq = BaseApplicationImpl.sMobileQQ;
        if (mqq instanceof BaseApplicationImpl) {
            try {
                Field fDirector = BaseApplicationImpl.class.getDeclaredField("sDirector");
                fDirector.setAccessible(true);
                mDirectorField = fDirector;
                mNeedInterceptStartActivity = true;
            } catch (ReflectiveOperationException e) {
                Log.e(e);
            }
        }
    }

    public void notifyStartupFinished() {
        mStartupFinished = true;
        mNeedInterceptStartActivity = false;
        SyncUtils.runOnUiThread(this::callActivityOnCreate);
    }

    private void callActivityOnCreate() {
        mStartupFinished = true;
        mNeedInterceptStartActivity = false;
        for (WeakReference<BaseActivity> ref : mSuspendedActivities) {
            BaseActivity activity = ref.get();
            if (activity != null) {
                activity.callOnCreateProcedureInternal();
            }
        }
        mSuspendedActivities.clear();
    }

    /**
     * Whether the host is in splash screen
     *
     * @param activity the activity on creating
     * @param intent   the intent of the activity
     * @return true if the host is in splash screen
     */
    public boolean onActivityCreate(@NonNull Activity activity, @Nullable Intent intent) {
        if (mStartupFinished || !mNeedInterceptStartActivity) {
            return false;
        }
        if (!hasSteps()) {
            return false;
        }
        Log.i("maybe in splash screen, intercepting activity onCreate");
        if (activity instanceof BaseActivity) {
            mSuspendedActivities.add(new WeakReference<>((BaseActivity) activity));
            if (!mProbeStarted) {
                Intent probeIntent = new Intent(activity, ShadowStartupAgentActivity.class);
                activity.startActivity(probeIntent);
                mProbeStarted = true;
            }
            return true;
        }
        return false;
    }

    public boolean hasSteps() {
        if (mStartupFinished || !mNeedInterceptStartActivity) {
            return false;
        }
        Object director;
        try {
            director = mDirectorField.get(null);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
        // after startup finished, the director is null
        if (director == null) {
            mNeedInterceptStartActivity = false;
            return false;
        } else {
            Log.d("director is not null");
            return true;
        }
    }

    public void onActivityFocusChanged(@NonNull Activity activity, boolean hasFocus) {
    }

}
