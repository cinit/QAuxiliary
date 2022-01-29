package com.tencent.common.app;

import android.content.Intent;
import android.os.Bundle;
import mqq.app.AppActivity;
import mqq.app.AppRuntime;
import mqq.app.MobileQQ;

/* compiled from: ProGuard */
public class BaseApplicationImpl extends MobileQQ {

//    public static BaseApplicationImpl a() {
//        throw new RuntimeException("Stub!");
//    }

    public AppRuntime a() {
        throw new RuntimeException("Stub!");
    }

    public AppRuntime createRuntime(String str) {
        throw new RuntimeException("Stub!");
    }

    public int getAppId(String str) {
        throw new RuntimeException("Stub!");
    }

    public String getBootBroadcastName(String str) {
        if (str.equals("com.tencent.mobileqq")) {
            return "com.tencent.mobileqq.broadcast.qq";
        }
        if (str.equals("com.tencent.mobileqq:video")) {
            return "com.tencent.av.ui.VChatActivity";
        }
        return "";
    }

    public boolean isNeedMSF(String str) {
        throw new RuntimeException("Stub!");
    }

    public void onCreate() {
        super.onCreate();
        throw new RuntimeException("Stub!");
    }

    public boolean onActivityCreate(AppActivity appActivity, Intent intent) {
        throw new RuntimeException("Stub!");
    }

    public void onActivityFocusChanged(AppActivity appActivity, boolean z) {
        throw new RuntimeException("Stub!");
    }

    public void onRecver(String str) {
        throw new RuntimeException("Stub!");
    }

    public void startActivity(Intent intent) {
        throw new RuntimeException("Stub!");
    }

    public void startActivity(Intent intent, Bundle bundle) {
        throw new RuntimeException("Stub!");
    }
}
