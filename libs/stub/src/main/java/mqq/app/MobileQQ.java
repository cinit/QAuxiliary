package mqq.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import com.tencent.qphone.base.remote.SimpleAccount;
import com.tencent.qphone.base.util.BaseApplication;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class MobileQQ extends BaseApplication {

    public static final String KEY_UIN = "uin";
    public static final String PREF_LAST_LOGIN = "last";
    public static final int STATE_EMPTY = 1;
    public static final int STATE_INITING = 2;
    public static final int STATE_READY = 3;
    static final int MSG_ACCOUNT = 1;
    private static final String PREF_KEY = "currentAccount";
    private static final String PREF_SHARE = "share";
    private static final String PROPERTY_NAME = "Properties";
    public static String processName;
    public static MobileQQ sMobileQQ;
    /* access modifiers changed from: private */
    public final List activitys = new ArrayList();
    protected final ArrayList appActivities = new ArrayList<>();
    final Handler mHandler = null;
    /* access modifiers changed from: private */
    private final Properties properties = new Properties();
    /* access modifiers changed from: private */
    public boolean accountChanged;
    /* access modifiers changed from: private */
    public Runnable doExit = null;
    /* access modifiers changed from: private */
    public boolean isCrashed;
    /* access modifiers changed from: private */
    public AppRuntime mAppRuntime;
    /* access modifiers changed from: private */
    public Bundle savedInstanceState;
    /* access modifiers changed from: private */
    public boolean stopMSF = false;
    List<SimpleAccount> sortAccountList = null;
    private volatile AtomicInteger mRuntimeState = new AtomicInteger(1);

    public static MobileQQ getMobileQQ() {
        return sMobileQQ;
    }

    public static boolean killProcess(Context context, String processName2) {
        throw new RuntimeException("Stub!");
    }

    public abstract AppRuntime createRuntime(String str);

    public abstract int getAppId(String str);

    public abstract String getBootBroadcastName(String str);

    public abstract boolean isNeedMSF(String str);

    void addActivity(BaseActivity act) {
        throw new RuntimeException("Stub!");
    }

    void removeActivity(BaseActivity act) {
        throw new RuntimeException("Stub!");
    }

    public int getVisibleActivityCount() {
        throw new RuntimeException("Stub!");
    }

    public void onCreate() {
        super.onCreate();
        throw new RuntimeException("Stub!");
    }

    public final void doInit(boolean startService) {
        throw new RuntimeException("Stub!");
    }

    public void startService() {
        throw new RuntimeException("Stub!");
    }

    public boolean isRuntimeReady() {
        return this.mRuntimeState.get() == 3;
    }

    private void loadProperites() {
        throw new RuntimeException("Stub!");
    }

    private void restoreBundle() {
        throw new RuntimeException("Stub!");
    }

    public void crashed() {
        this.isCrashed = true;
    }

    public void QQProcessExit(boolean bStopMSF) {
        exit(false, bStopMSF);
    }

    public void otherProcessExit(boolean callByMSF) {
        exit(callByMSF, false);
    }

    private void exit(final boolean callByMSF, boolean _stopMSF) {
        throw new RuntimeException("Stub!");
    }

    void closeAllActivitys() {
        throw new RuntimeException("Stub!");
    }

    void delStateFile() {
        throw new RuntimeException("Stub!");
    }

    public List<SimpleAccount> refreAccountList() {
        throw new RuntimeException("Stub!");
    }

    public void setSortAccountList(List<SimpleAccount> all) {
        throw new RuntimeException("Stub!");
    }

    public List<SimpleAccount> getAllNotSynAccountList() {
        return this.sortAccountList;
    }

    public SimpleAccount getFirstSimpleAccount() {
        List<SimpleAccount> list = getAllAccounts();
        if (list == null || list.size() == 0) {
            return null;
        }
        return list.get(0);
    }

    public List<SimpleAccount> getAllAccounts() {
        throw new RuntimeException("Stub!");
    }

    public long string2Long(String number) {
        if (!TextUtils.isEmpty(number)) {
            try {
                return Long.parseLong(number);
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        }
        return 0;
    }

    void createNewRuntime(SimpleAccount account, boolean forLogin, boolean needSaveLoginTime,
        int cnrType) {
        throw new RuntimeException("Stub!");
    }

    void dispatchAccountEvent(Constants.LogoutReason reason, int crnType, boolean isFirstTime) {
        throw new RuntimeException("Stub!");
    }

    public AppRuntime waitAppRuntime(BaseActivity c) {
        throw new RuntimeException("Stub!");
    }

    public AppRuntime getAppRuntime(String account) throws AccountNotMatchException {
        throw new RuntimeException("Stub!");
    }

//    public String getProcessName() {
//        throw new RuntimeException("Stub!");
//    }

    public void setProperty(String name, String value) {
        this.properties.setProperty(name, value);
        OutputStream os = null;
        try {
            os = openFileOutput(PROPERTY_NAME, 0);
            this.properties.store(os, (String) null);
            os.flush();
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e2) {
            e2.printStackTrace();
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            }
        } catch (Throwable th) {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e4) {
                    e4.printStackTrace();
                }
            }
            throw th;
        }
    }

    public String getProperty(String name) {
        return this.properties.getProperty(name);
    }

    public void setAutoLogin(boolean isLogin) {
        if (this.mAppRuntime != null && this.mAppRuntime.getAccount() != null
            && this.mAppRuntime.getAccount().length() > 0) {
            this.mAppRuntime.setAutoLogin(isLogin);
        }
    }

    public boolean onActivityCreate(AppActivity appActivity, Intent intent) {
        return false;
    }

    public void onActivityFocusChanged(AppActivity appActivity, boolean hasFocus) {
    }
}
