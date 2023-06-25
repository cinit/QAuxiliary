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
import cc.ioctl.util.Reflex;
import com.tencent.common.app.BaseApplicationImpl;
import io.github.qauxv.util.SyncUtils;
import io.github.qauxv.activity.BaseActivity;
import io.github.qauxv.util.HostInfo;
import io.github.qauxv.util.Initiator;
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
    private Field mNtStartupDirectorInstanceField = null;
    private Field mNtStartupDirectorStatusStringField = null;

    private void initialize() {
        if (HostInfo.isInModuleProcess() || !SyncUtils.isMainProcess()) {
            mNeedInterceptStartActivity = false;
        } else {
            initializeInternalNoInline();
        }
    }

    private void initializeInternalNoInline() {
        // move access to BaseApplicationImpl in a separate method to avoid R8 instruction optimization
        // const-class v0, Lcom/tencent/common/app/BaseApplicationImpl;
        // R8 may move the const-class instruction to the beginning of the method
        // only in host main process
        MobileQQ mqq = BaseApplicationImpl.sMobileQQ;
        if (mqq instanceof BaseApplicationImpl) {
            // NT version
            Class<?> kNtStartupDirector = Initiator._NtStartupDirector();
            if (kNtStartupDirector != null) {
                // find instance field
                Field fInstance = Reflex.findFirstDeclaredStaticFieldByTypeOrNull(kNtStartupDirector, kNtStartupDirector);
                if (fInstance != null) {
                    fInstance.setAccessible(true);
                    // find status string field, there should be only one
                    Field candidate = null;
                    for (Field f : kNtStartupDirector.getDeclaredFields()) {
                        if (f.getType() == String.class) {
                            if (candidate == null) {
                                candidate = f;
                            } else {
                                Log.e("multiple status string fields found in StartupDirector");
                                candidate = null;
                                break;
                            }
                        }
                    }
                    if (candidate != null) {
                        candidate.setAccessible(true);
                        mNtStartupDirectorInstanceField = fInstance;
                        mNtStartupDirectorStatusStringField = candidate;
                        mNeedInterceptStartActivity = true;
                    } else {
                        Log.e("no status string field found in StartupDirector");
                    }
                    return;
                }
            }
            // older
            try {
                Field fDirector = BaseApplicationImpl.class.getDeclaredField("sDirector");
                fDirector.setAccessible(true);
                mDirectorField = fDirector;
                mNeedInterceptStartActivity = true;
            } catch (NoSuchFieldException nfe) {
                Class<?> kStartupDirector = Initiator._StartupDirector();
                Field fDirector = null;
                if (kStartupDirector != null) {
                    fDirector = Reflex.findFirstDeclaredStaticFieldByTypeOrNull(BaseApplicationImpl.class, kStartupDirector);
                }
                if (fDirector != null) {
                    mDirectorField = fDirector;
                    mNeedInterceptStartActivity = true;
                } else {
                    Log.e("StartupDirector field not found", nfe);
                }
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
        if (mNtStartupDirectorInstanceField != null && mNtStartupDirectorStatusStringField != null) {
            // NT
            Object director = null;
            try {
                director = mNtStartupDirectorInstanceField.get(null);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
            if (director == null) {
                mNeedInterceptStartActivity = false;
                return false;
            }
            String status = null;
            try {
                status = (String) mNtStartupDirectorStatusStringField.get(director);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
            Log.d("director status: " + status);
            if ("BackgroundCreate".equals(status) || "ApplicationCreate".equals(status)) {
                return true;
            }
            mNeedInterceptStartActivity = false;
            return false;
        } else {
            // older
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
    }

    public void onActivityFocusChanged(@NonNull Activity activity, boolean hasFocus) {
    }

}
