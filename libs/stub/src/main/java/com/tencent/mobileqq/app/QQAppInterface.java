package com.tencent.mobileqq.app;

import android.os.Bundle;
import com.tencent.common.app.AppInterface;
import com.tencent.common.app.BaseApplicationImpl;
import mqq.manager.Manager;

public class QQAppInterface extends AppInterface {

    public QQAppInterface(BaseApplicationImpl baseApplicationImpl, String str) {
        super(baseApplicationImpl, str);
    }

    public void start(boolean z2) {

    }

    protected void addManager(int i2, Manager manager) {

    }

    public void onRunningBackground(Bundle bundle) {
    }

    public Manager getManager(int i2) {
        throw new RuntimeException("Stub!");
    }

    public void onRunningForeground() {

    }

    protected Class[] getMessagePushServlets() {
        throw new RuntimeException("Stub!");
    }

    protected String[] getMessagePushSSOCommands() {
        throw new RuntimeException("Stub!");
    }

    protected boolean canAutoLogin(String str) {
        throw new RuntimeException("Stub!");
    }

    public void setAutoLogin(boolean z2) {

    }

    public void onDestroy() {
    }

    protected void finalize() {
        throw new RuntimeException("Stub!");
    }

    protected void userLogoutReleaseData() {
    }

    public void logout(boolean z2) {
    }

    public void onCreate(Bundle bundle) {

    }

}
