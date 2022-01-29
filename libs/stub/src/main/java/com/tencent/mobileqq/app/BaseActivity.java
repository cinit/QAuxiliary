package com.tencent.mobileqq.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import mqq.app.AppActivity;
import mqq.app.AppRuntime;

public class BaseActivity extends AppActivity {

    public static boolean isUnLockSuccess = false;
    public static boolean mAppForground = true;
    public static BaseActivity sTopActivity;

    public QQAppInterface app;

    public static boolean isMoveTaskToBack(Context context, Intent intent) {
        return intent.getComponent() == null || !intent.getComponent().getPackageName()
            .equals(context.getPackageName());
    }

    @SuppressLint({"SdCardPath"})
    public boolean doDispatchKeyEvent(KeyEvent keyEvent) {
        throw new RuntimeException("Stub!");
    }

    public boolean doOnCreate(Bundle bundle) {
        throw new RuntimeException("Stub!");
    }

    public void doOnStart() {
        super.doOnStart();
        throw new RuntimeException("Stub!");
    }

    public void doOnConfigurationChanged(Configuration configuration) {
        super.doOnConfigurationChanged(configuration);
        throw new RuntimeException("Stub!");
    }

    public void doOnStop() {
        super.doOnStop();
        throw new RuntimeException("Stub!");
    }

    public void doOnResume() {
        super.doOnResume();
        sTopActivity = this;
        throw new RuntimeException("Stub!");
    }

    public void doOnPause() {
        super.doOnPause();
        throw new RuntimeException("Stub!");
    }

    public void doOnDestroy() {
        super.doOnDestroy();
        throw new RuntimeException("Stub!");
    }

    public void doOnBackPressed() {
        throw new RuntimeException("Stub!");
    }

    @Deprecated
    public Activity getActivity() {
        return this;
    }

//    public void addObserver(BusinessObserver businessObserver) {
//        throw new RuntimeException("Stub!");
//    }
//
//    public void removeObserver(BusinessObserver businessObserver) {
//        throw new RuntimeException("Stub!");
//    }

    public void turnOnShake() {
        throw new RuntimeException("Stub!");
    }

    public void turnOffShake() {
        throw new RuntimeException("Stub!");
    }

    public void addTouchFeedback() {
        throw new RuntimeException("Stub!");
    }

    public int getTitleBarHeight() {
        throw new RuntimeException("Stub!");
    }

    public void startActivity(Intent intent) {
        throw new RuntimeException("Stub!");
    }

    public void onPreThemeChanged() {
    }

    public void onPostThemeChanged() {
    }

    public void onAccountChanged() {
        super.onAccountChanged();
        throw new RuntimeException("Stub!");
    }

    public void setCanLock(boolean z) {
        throw new RuntimeException("Stub!");
    }

    public void startActivityForResult(Intent intent, int i) {
        throw new RuntimeException("Stub!");
    }

    public void startActivityForResult(Intent intent, int i, int i2) {
        throw new RuntimeException("Stub!");
    }

    public boolean preloadData(AppRuntime appRuntime, boolean z) {
        return false;
    }
}
