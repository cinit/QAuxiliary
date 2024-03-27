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

package io.github.qauxv.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.FragmentManager;
import io.github.qauxv.ui.ResUtils;
import io.github.qauxv.util.HostInfo;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.consis.StartupDirectorBridge;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Compatible with host app's BaseActivity
 */
public abstract class BaseActivity extends AppCompatTransferActivity {

    private static final String FRAGMENTS_TAG = "android:support:fragments";
    private boolean mIsFinishingInOnCreate = false;
    private boolean mIsResultWaiting;
    private boolean mIsResume = false;
    private boolean mIsInitializing = false;
    private boolean mIsStartSkipped = false;
    private Intent mNewIntent;
    private Bundle mOnCreateBundle = null;
    private Bundle mOnRestoreBundle;
    private Bundle mPostCreateBundle = null;
    private int mRequestCode;
    private int mResultCode;
    private Intent mResultData;
    private int mWindowFocusState = -1;
    private StartupDirectorBridge mStartupBridge = null;
    private ArrayList<?> mLifecycleCallbacks = null;

    @CallSuper
    @Override
    protected void attachBaseContext(Context newBase) {
        if (!HostInfo.isInModuleProcess()) {
            // sync theme with host
            // note that ResUtils.getNightModeMasked() is not what AppCompatDelegate.setDefaultNightMode requires
            AppCompatDelegate.setDefaultNightMode(ResUtils.isInNightMode() ?
                    AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        }
        super.attachBaseContext(newBase);
        initLifecycleHooks();
        mStartupBridge = StartupDirectorBridge.getInstance();
        mLifecycleCallbacks = createInstanceLifecycleCallbacks();
    }

    /**
     * Called before {@code super.onCreate(savedInstanceState)}
     * <p>
     * Be cautious when overriding this method.
     *
     * @param savedInstanceState the bundle to save state
     * @param isInitializing     true if is initializing
     */
    @SuppressWarnings("unused")
    protected void doOnEarlyCreate(@Nullable Bundle savedInstanceState, boolean isInitializing) {
        // to be overridden
    }

    /**
     * @deprecated use {@link #doOnCreate(Bundle)} and {@link #doOnEarlyCreate(Bundle, boolean)} instead.
     */
    @Deprecated
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Intent intent = getIntent();
        this.mIsInitializing = mStartupBridge.onActivityCreate(this, intent);
        doOnEarlyCreate(savedInstanceState, mIsInitializing);
        if (this.mIsInitializing) {
            this.mOnCreateBundle = savedInstanceState;
            if (savedInstanceState != null) {
                savedInstanceState.remove(FRAGMENTS_TAG);
            }
            super.onCreate(shouldRetainActivitySavedInstanceState() ? savedInstanceState : null);
        } else {
            super.onCreate(shouldRetainActivitySavedInstanceState() ? savedInstanceState : null);
            doOnCreate(savedInstanceState);
        }
    }

    @CallSuper
    @SuppressWarnings("unused")
    protected boolean doOnCreate(@Nullable Bundle savedInstanceState) {
        dispatchActivityPreCreated(this, savedInstanceState);
        this.mOnCreateBundle = null;
        dispatchActivityOnCreate(this, savedInstanceState);
        dispatchActivityPostCreated(this, savedInstanceState);
        return false;
    }

    /**
     * @deprecated use {@link #doOnPostCreate(Bundle)} instead.
     */
    @Override
    @Deprecated
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(shouldRetainActivitySavedInstanceState() ? savedInstanceState : null);
        if (!this.mIsInitializing) {
            doOnPostCreate(savedInstanceState);
        } else {
            this.mPostCreateBundle = savedInstanceState;
        }
    }

    /**
     * Get whether the savedInstanceState should be ignored. This is useful for activities that saved instance are transient and should not be saved. If this
     * method returns true, the savedInstanceState passed to {@link Activity#onCreate(Bundle)}, {@link Activity#onPostCreate(Bundle)} and
     * {@link Activity#onRestoreInstanceState(Bundle)} will be null, but {@link BaseActivity#doOnCreate(Bundle)} and {@link BaseActivity#doOnPostCreate(Bundle)}
     * will still receive the original savedInstanceState.
     * <p>
     * A trivial activity that does not handle savedInstanceState specially can return true.
     * <p>
     * Note: This method is called before {@link #doOnCreate(Bundle)} and should be constexpr.
     *
     * @return true if the savedInstanceState should be ignored.
     */
    protected boolean shouldRetainActivitySavedInstanceState() {
        return true;
    }

    /**
     * @deprecated use {@link #doOnRestoreInstanceState(Bundle)} instead.
     */
    @Override
    @Deprecated
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        if (!shouldRetainActivitySavedInstanceState()) {
            return;
        }
        if (!this.mIsInitializing) {
            doOnRestoreInstanceState(savedInstanceState);
        } else {
            this.mOnRestoreBundle = savedInstanceState;
        }
    }

    /**
     * @deprecated use {@link #doOnSaveInstanceState(Bundle)} instead.
     */
    @Override
    @Deprecated
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (!this.mIsInitializing) {
            doOnSaveInstanceState(outState);
        }
    }

    /**
     * @deprecated use {@link #doOnDestroy()} instead.
     */
    @Override
    @Deprecated
    protected void onDestroy() {
        if (!this.mIsInitializing || this.mIsFinishingInOnCreate) {
            doOnDestroy();
        }
        super.onDestroy();
    }

    public void callOnCreateProcedureInternal() {
        boolean hasFocus = true;
        if (this.mIsInitializing) {
            this.mIsInitializing = false;
            bypassStateLossCheck();
            if (doOnCreate(this.mOnCreateBundle) && !isFinishing()) {
                if (this.mIsStartSkipped) {
                    doOnStart();
                    this.mIsStartSkipped = false;
                    if (this.mOnRestoreBundle != null) {
                        doOnRestoreInstanceState(this.mOnRestoreBundle);
                        this.mOnRestoreBundle = null;
                    }
                    doOnPostCreate(this.mPostCreateBundle);
                    if (this.mIsResultWaiting) {
                        doOnActivityResult(this.mRequestCode, this.mResultCode, this.mResultData);
                        this.mIsResultWaiting = false;
                        this.mResultData = null;
                    }
                    if (this.mNewIntent != null) {
                        doOnNewIntent(this.mNewIntent);
                        this.mNewIntent = null;
                    }
                    if (isResume()) {
                        doOnResume();
                        doOnPostResume();
                    }
                    if (this.mWindowFocusState != -1) {
                        if (this.mWindowFocusState != 1) {
                            hasFocus = false;
                        }
                        doOnWindowFocusChanged(hasFocus);
                    }
                }
            } else if (isFinishing()) {
                this.mIsInitializing = true;
                this.mIsFinishingInOnCreate = true;
            }
        }
    }

    private static final Field fStateSaved;
    private static final Field fStopped;

    static {
        try {
            Class<?> clz = Class.forName("androidx.fragment.app.FragmentManager");
            fStateSaved = clz.getDeclaredField("mStateSaved");
            fStateSaved.setAccessible(true);
            fStopped = clz.getDeclaredField("mStopped");
            fStopped.setAccessible(true);
        } catch (NoSuchFieldException e) {
            // we can hardly do anything here, we just want to die die die...
            throw new NoSuchFieldError(e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new NoClassDefFoundError(e.getMessage());
        }
    }

    private void bypassStateLossCheck() {
        FragmentManager fragmentMgr = getSupportFragmentManager();
        try {
            fStopped.set(fragmentMgr, false);
            fStateSaved.set(fragmentMgr, false);
        } catch (IllegalAccessException ignored) {
            // should not happen
        }
    }

    /**
     * @deprecated use {@link #doOnNewIntent(Intent)} instead.
     */
    @Override
    @Deprecated
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (!this.mIsInitializing) {
            doOnNewIntent(intent);
        } else {
            this.mNewIntent = intent;
        }
    }

    /**
     * @deprecated use {@link #doOnActivityResult(int, int, Intent)} instead.
     */
    @Override
    @Deprecated
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (!this.mIsInitializing) {
            doOnActivityResult(requestCode, resultCode, intent);
        } else {
            this.mIsResultWaiting = true;
            this.mRequestCode = requestCode;
            this.mResultCode = resultCode;
            this.mResultData = intent;
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    /**
     * @deprecated use {@link #doOnStart()} instead.
     */
    @Override
    @Deprecated
    protected void onStart() {
        super.onStart();
        if (!this.mIsInitializing) {
            doOnStart();
        } else {
            this.mIsStartSkipped = true;
        }
    }

    /**
     * @deprecated use {@link #doDispatchKeyEvent(KeyEvent)} instead.
     */
    @Override
    @Deprecated
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (this.mIsInitializing) {
            return false;
        }
        return doDispatchKeyEvent(event);
    }

    protected boolean doDispatchKeyEvent(KeyEvent event) {
        return super.dispatchKeyEvent(event);
    }

    /**
     * @deprecated use {@link #doOnStop()} instead.
     */
    @Override
    @Deprecated
    protected void onStop() {
        if (!this.mIsInitializing) {
            doOnStop();
        } else {
            this.mIsStartSkipped = false;
        }
        super.onStop();
    }

    /**
     * @deprecated use {@link #doOnResume()} instead.
     */
    @Override
    @Deprecated
    @SuppressWarnings("JavaReflectionMemberAccess")
    @SuppressLint("DiscouragedPrivateApi")
    protected void onResume() {
        try {
            super.onResume();
        } catch (IllegalArgumentException e) {
            try {
                Field mCalled = Activity.class.getDeclaredField("mCalled");
                mCalled.setAccessible(true);
                mCalled.set(this, true);
            } catch (ReflectiveOperationException ignored) {
                throw (NoSuchFieldError) new NoSuchFieldError("set Activity.mCalled failed while super.onResume() throw IllegalArgumentException")
                        .initCause(e);
            }
        } catch (NullPointerException ignored) {
            // i don't know
        }
        this.mIsResume = true;
        if (!this.mIsInitializing) {
            doOnResume();
        }
    }

    /**
     * @deprecated use {@link #doOnPostResume()} instead.
     */
    @Override
    @Deprecated
    protected void onPostResume() {
        super.onPostResume();
        if (!this.mIsInitializing) {
            doOnPostResume();
        }
    }

    /**
     * @deprecated use {@link #doOnConfigurationChanged(Configuration)} instead.
     */
    @Override
    @Deprecated
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        if (!this.mIsInitializing) {
            doOnConfigurationChanged(newConfig);
        }
        super.onConfigurationChanged(newConfig);
    }

    /**
     * @deprecated use {@link #doOnWindowFocusChanged(boolean)} instead.
     */
    @Override
    @Deprecated
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        mStartupBridge.onActivityFocusChanged(this, hasFocus);
        if (!this.mIsInitializing) {
            doOnWindowFocusChanged(hasFocus);
        } else {
            this.mWindowFocusState = hasFocus ? 1 : 0;
        }
    }

    /**
     * @deprecated use {@link #doOnBackPressed()} instead.
     */
    @SuppressLint("MissingSuperCall")
    @Override
    @Deprecated
    public void onBackPressed() {
        if (!this.mIsInitializing) {
            doOnBackPressed();
        }
    }

    /**
     * @deprecated use {@link #doOnKeyDown(int, KeyEvent)} instead.
     */
    @Override
    @Deprecated
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (this.mIsInitializing) {
            return false;
        }
        return doOnKeyDown(keyCode, event);
    }

    /**
     * @deprecated use {@link #doOnUserLeaveHint()} instead.
     */
    @Override
    @Deprecated
    protected void onUserLeaveHint() {
        if (!this.mIsInitializing) {
            doOnUserLeaveHint();
        }
        super.onUserLeaveHint();
    }

    /**
     * @deprecated use {@link #doOnPause()} instead.
     */
    @Override
    @Deprecated
    protected void onPause() {
        if (!this.mIsInitializing) {
            doOnPause();
        }
        this.mIsResume = false;
        super.onPause();
    }

    @SuppressWarnings("unused")
    @CallSuper
    protected void doOnPostCreate(Bundle bundle) {
        this.mPostCreateBundle = null;
    }

    @SuppressWarnings("unused")
    protected void doOnRestoreInstanceState(@NonNull Bundle bundle) {
        // to implement
    }

    protected void doOnSaveInstanceState(@NonNull Bundle outState) {
        // to implement
    }

    @CallSuper
    protected void doOnStart() {
        dispatchActivityStarted(this);
    }

    @SuppressWarnings("unused")
    @CallSuper
    protected void doOnNewIntent(Intent intent) {
        dispatchNewInstant(this, intent);
    }

    @SuppressWarnings("unused")
    protected void doOnActivityResult(int requestCode, int resultCode, Intent intent) {
        // to implement
    }

    @CallSuper
    protected void doOnResume() {
        dispatchActivityPreResumed(this);
        dispatchActivityResumed(this);
        dispatchActivityPostResumed(this);
    }

    protected void doOnPostResume() {
        // to implement
    }

    @SuppressWarnings("unused")
    @CallSuper
    protected void doOnWindowFocusChanged(boolean hasFocus) {
        dispatchActivityWindowFocusChanged(this, hasFocus);
    }

    @SuppressWarnings("unused")
    protected void doOnConfigurationChanged(Configuration newConfig) {
        // to implement
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode, Configuration newConfig) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig);
        dispatchOnMultiWindowModeChanged(this, isInMultiWindowMode);
    }

    protected void doOnBackPressed() {
        super.onBackPressed();
    }

    public static boolean isMoveTaskToBack(@NonNull Context context, @NonNull Intent intent) {
        return intent.getComponent() == null || !intent.getComponent().getPackageName().equals(context.getPackageName());
    }

    protected boolean doOnKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    protected void doOnUserLeaveHint() {
        // to implement
    }

    @CallSuper
    protected void doOnPause() {
        dispatchActivityPrePaused(this);
        dispatchActivityOnPause(this);
        dispatchActivityPostPaused(this);
    }

    @CallSuper
    protected void doOnStop() {
        dispatchActivityStopped(this);
    }

    @CallSuper
    protected void doOnDestroy() {
        dispatchActivityDestroyed(this);
    }

    protected final boolean isResume() {
        return this.mIsResume;
    }

    protected boolean isWrapContent() {
        return true;
    }

    // callbacks

    private void invokeBaseActivityLifecycleCallbacks(@NonNull Method callback, Object... args) {
        if (sLifecycleCallbacksInitSuccess) {
            try {
                if (mLifecycleCallbacks != null && !mLifecycleCallbacks.isEmpty()) {
                    for (Object cb : mLifecycleCallbacks) {
                        // Log.d("invoke instance callback " + Reflex.getShortClassName(cb) + "." + callback.getName());
                        callback.invoke(cb, args);
                    }
                }
                ArrayList<?> globalCallbacks = collectGlobalLifecycleCallbacks();
                if (globalCallbacks != null && !globalCallbacks.isEmpty()) {
                    for (Object cb : globalCallbacks) {
                        // Log.d("invoke global callback " + Reflex.getShortClassName(cb) + "." + callback.getName());
                        callback.invoke(cb, args);
                    }
                }
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            } catch (InvocationTargetException ite) {
                Throwable cause = ite.getCause();
                if (cause == null) {
                    cause = ite;
                }
                Log.e("invokeCallback: " + callback, cause);
            }
        }
    }

    private void dispatchActivityPreCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
        invokeBaseActivityLifecycleCallbacks(sOnActivityPreCreatedMethod, activity, bundle);
    }

    private void dispatchActivityOnCreate(@NonNull Activity activity, @Nullable Bundle bundle) {
        invokeBaseActivityLifecycleCallbacks(sOnActivityCreateMethod, activity, bundle);
    }

    private void dispatchActivityPostCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
        invokeBaseActivityLifecycleCallbacks(sOnActivityPostCreatedMethod, activity, bundle);
    }

    private void dispatchActivityStopped(@NonNull Activity activity) {
        invokeBaseActivityLifecycleCallbacks(sOnActivityStoppedMethod, activity);
    }

    private void dispatchActivityStarted(@NonNull Activity activity) {
        invokeBaseActivityLifecycleCallbacks(sOnActivityStartedMethod, activity);
    }

    private void dispatchActivityPrePaused(@NonNull Activity activity) {
        invokeBaseActivityLifecycleCallbacks(sOnActivityPrePausedMethod, activity);
    }

    private void dispatchActivityOnPause(@NonNull Activity activity) {
        invokeBaseActivityLifecycleCallbacks(sOnActivityOnPauseMethod, activity);
    }

    private void dispatchActivityPostPaused(@NonNull Activity activity) {
        invokeBaseActivityLifecycleCallbacks(sOnActivityPostPausedMethod, activity);
    }

    private void dispatchActivityPreResumed(@NonNull Activity activity) {
        invokeBaseActivityLifecycleCallbacks(sOnActivityPreResumedMethod, activity);
    }

    private void dispatchActivityResumed(@NonNull Activity activity) {
        invokeBaseActivityLifecycleCallbacks(sOnActivityResumedMethod, activity);
    }

    private void dispatchActivityPostResumed(@NonNull Activity activity) {
        invokeBaseActivityLifecycleCallbacks(sOnActivityPostResumedMethod, activity);
    }

    private void dispatchNewInstant(@NonNull Activity activity, Intent intent) {
        invokeBaseActivityLifecycleCallbacks(sOnNewIntentMethod, activity, intent);
    }

    private void dispatchActivityDestroyed(@NonNull Activity activity) {
        invokeBaseActivityLifecycleCallbacks(sOnActivityDestroyedMethod, activity);
    }

    private void dispatchOnMultiWindowModeChanged(@NonNull Activity activity, boolean isInMultiWindowMode) {
        invokeBaseActivityLifecycleCallbacks(sOnMultiWindowModeChangedMethod, activity, isInMultiWindowMode);
    }

    private void dispatchActivityWindowFocusChanged(@NonNull Activity activity, boolean hasFocus) {
        invokeBaseActivityLifecycleCallbacks(sOnActivityWindowFocusChangedMethod, activity, hasFocus);
    }

    private static boolean sLifecycleCallbacksTryInit = false;
    private static boolean sLifecycleCallbacksInitSuccess = false;

    private static Method sOnActivityPreCreatedMethod = null;
    private static Method sOnActivityCreateMethod = null;
    private static Method sOnActivityPostCreatedMethod = null;
    private static Method sOnActivityStoppedMethod = null;
    private static Method sOnActivityStartedMethod = null;
    private static Method sOnActivityPrePausedMethod = null;
    private static Method sOnActivityOnPauseMethod = null;
    private static Method sOnActivityPostPausedMethod = null;
    private static Method sOnActivityPreResumedMethod = null;
    private static Method sOnActivityResumedMethod = null;
    private static Method sOnActivityPostResumedMethod = null;
    private static Method sOnNewIntentMethod = null;
    private static Method sOnActivityDestroyedMethod = null;
    private static Method sOnMultiWindowModeChangedMethod = null;
    private static Method sOnActivityWindowFocusChangedMethod = null;

    private static Field sfGlobalLifecycleCallbacks = null;
    private static Field sfGlobalLifecycleCallbacksArrayList = null;
    private static ArrayList<Class<?>> sInstanceLifecycleCallbacksArrayList = null;

    private static void initLifecycleHooks() {
        if (sLifecycleCallbacksTryInit) {
            return;
        }
        sLifecycleCallbacksTryInit = true;
        if (HostInfo.isInModuleProcess()) {
            // only available in host
            return;
        }
        try {
            Class<?> activityLifecycleCallbacksClass = Initiator.load("com.tencent.mobileqq.app.lifecycle.BaseActivityLifecycleCallbacks");
            if (activityLifecycleCallbacksClass == null) {
                // fail silently if not found
                return;
            }
            sOnActivityPreCreatedMethod = activityLifecycleCallbacksClass.getMethod("doOnActivityPreCreated", Activity.class, Bundle.class);
            sOnActivityCreateMethod = activityLifecycleCallbacksClass.getMethod("doOnActivityCreate", Activity.class, Bundle.class);
            sOnActivityPostCreatedMethod = activityLifecycleCallbacksClass.getMethod("doOnActivityPostCreated", Activity.class, Bundle.class);
            sOnActivityStoppedMethod = activityLifecycleCallbacksClass.getMethod("doOnActivityStopped", Activity.class);
            sOnActivityStartedMethod = activityLifecycleCallbacksClass.getMethod("doOnActivityOnStart", Activity.class);
            sOnActivityPrePausedMethod = activityLifecycleCallbacksClass.getMethod("doOnActivityPrePaused", Activity.class);
            sOnActivityOnPauseMethod = activityLifecycleCallbacksClass.getMethod("doOnActivityPause", Activity.class);
            sOnActivityPostPausedMethod = activityLifecycleCallbacksClass.getMethod("doOnActivityPostPaused", Activity.class);
            sOnActivityPreResumedMethod = activityLifecycleCallbacksClass.getMethod("doOnActivityPreResumed", Activity.class);
            sOnActivityResumedMethod = activityLifecycleCallbacksClass.getMethod("doOnActivityResume", Activity.class);
            sOnActivityPostResumedMethod = activityLifecycleCallbacksClass.getMethod("doOnActivityPostResumed", Activity.class);
            sOnNewIntentMethod = activityLifecycleCallbacksClass.getMethod("doOnNewIntent", Activity.class, Intent.class);
            sOnActivityDestroyedMethod = activityLifecycleCallbacksClass.getMethod("doOnActivityDestroyed", Activity.class);
            sOnMultiWindowModeChangedMethod = activityLifecycleCallbacksClass.getMethod("onMultiWindowModeChanged", Activity.class, boolean.class);
            sOnActivityWindowFocusChangedMethod = activityLifecycleCallbacksClass.getMethod("doOnWindowFocusChanged", Activity.class, boolean.class);

            Class<?> kQBaseActivity = Initiator.loadClass("com.tencent.mobileqq.app.QBaseActivity");
            sfGlobalLifecycleCallbacks = kQBaseActivity.getDeclaredField("lifecycleCallbacks");
            sfGlobalLifecycleCallbacks.setAccessible(true);

            Class<?> kGlobalLifecycleCallbacks = Initiator.load("com.tencent.mobileqq.app.GlobalLifecycleCallbacks");
            if (kGlobalLifecycleCallbacks == null) {
                kGlobalLifecycleCallbacks = Initiator.load("com.tencent.mobileqq.app.lifecycle.GlobalLifecycleCallbacks");
            }
            if (kGlobalLifecycleCallbacks == null) {
                throw new ClassNotFoundException("com.tencent.mobileqq.app.(lifecycle)?.GlobalLifecycleCallbacks");
            }
            try {
                sfGlobalLifecycleCallbacksArrayList = kGlobalLifecycleCallbacks.getDeclaredField("a");
            } catch (NoSuchFieldException ignored) {
                sfGlobalLifecycleCallbacksArrayList = kGlobalLifecycleCallbacks.getDeclaredField("mActivityLifecycleCallbacks");
            }
            sfGlobalLifecycleCallbacksArrayList.setAccessible(true);

            Class<?> kQBaseActivityInjectUtil = Initiator.loadClass("com.tencent.mobileqq.app.QBaseActivityInjectUtil");
            try {
                Field instanceCallbacks = kQBaseActivityInjectUtil.getDeclaredField("instanceCallbacks");
                instanceCallbacks.setAccessible(true);
                sInstanceLifecycleCallbacksArrayList = (ArrayList<Class<?>>) instanceCallbacks.get(null);
            } catch (NoSuchFieldException nfe) {
                // TODO: 2022-03-20 check if there will be more callbacks required
            }

            sLifecycleCallbacksInitSuccess = true;
        } catch (ReflectiveOperationException e) {
            Log.e("LifecycleHook: missing methods or fields", e);
        }
    }

    @Nullable
    private ArrayList<?> collectGlobalLifecycleCallbacks() {
        if (!sLifecycleCallbacksInitSuccess) {
            return null;
        }
        try {
            Object globalLifecycleCallbacks = sfGlobalLifecycleCallbacks.get(null);
            if (globalLifecycleCallbacks == null) {
                return null;
            }
            return (ArrayList<?>) sfGlobalLifecycleCallbacksArrayList.get(globalLifecycleCallbacks);
        } catch (IllegalAccessException e) {
            // should not happen, because we set accessible = true
            throw new AssertionError(e);
        }
    }

    @Nullable
    private ArrayList<?> createInstanceLifecycleCallbacks() {
        if (!sLifecycleCallbacksInitSuccess) {
            return null;
        }
        ArrayList<Class<?>> instanceLifecycleCallbacksClasses = sInstanceLifecycleCallbacksArrayList;
        if (instanceLifecycleCallbacksClasses == null || instanceLifecycleCallbacksClasses.isEmpty()) {
            return null;
        }
        ArrayList<Object> callbacks = new ArrayList<>(instanceLifecycleCallbacksClasses.size());
        for (Class<?> instanceLifecycleCallbacksClass : instanceLifecycleCallbacksClasses) {
            try {
                Object cb = instanceLifecycleCallbacksClass.newInstance();
                callbacks.add(cb);
            } catch (InstantiationException | IllegalAccessException e) {
                Log.e("LifecycleHook: failed to create instance of " + instanceLifecycleCallbacksClass, e);
            }
        }
        return callbacks;
    }
}
