package mqq.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.tencent.qphone.base.remote.SimpleAccount;
import java.util.ArrayList;
import mqq.manager.Manager;

public abstract class AppRuntime {

    public static final int ACCOUNT_MANAGER = 0;
    public static final int END_UN_LOGIN_MANAGER = 6;
    public static final int PUSH_MANAGER = 4;
    public static final int SERVER_CONFIG_MANAGER = 3;
    public static final int TICKET_MANAGER = 2;
    public static final int TYPE_CREATENEWRUNTIME_CHANGUIN_LOGIN = 4;
    public static final int TYPE_CREATENEWRUNTIME_DIRECT_LOGIN = 1;
    public static final int TYPE_CREATENEWRUNTIME_DIRECT_NET_LOGIN = 3;
    public static final int TYPE_CREATENEWRUNTIME_DOINIT = 5;
    public static final int TYPE_CREATENEWRUNTIME_SWITCHACCOUNT = 2;
    public static final int VERIFYCODE_MANAGER = 5;
    public static final int VERIFYDEVLOCK_MANAGER = 6;
    public static final int WTLOGIN_MANAGER = 1;

    public boolean isBackground_Pause = false;
    public boolean isBackground_Stop = false;
    public boolean isClearTaskBySystem = false;

    public boolean isLogin;

    public MobileQQ mContext;

    byte[] uinSign = null;

    public static String showInfo() {
        throw new RuntimeException("Stub!");
    }

//    void init(MobileQQ context, MainService mService2, SimpleAccount account) {
//        throw new RuntimeException("Stub!");
//    }

    public static ArrayList<String> getAccountList() {
        throw new RuntimeException("Stub!");
    }

    protected boolean canAutoLogin(String account) {
        return true;
    }

//    MainService getService() {
//        return this.mService;
//    }
//
//    ServletContainer getServletContainer() {
//        return this.mServletContainer;
//    }

    public void setAutoLogin(boolean auto) {
    }

//    public void login(String account, byte[] password, AccountObserver observer) {
//        throw new RuntimeException("Stub!");
//    }
//
//    public void loginSubAccount(String mainaccount, String subaccount, String password, SubAccountObserver observer) {
//        throw new RuntimeException("Stub!");
//    }
//
//    public void ssoLogin(String ssoAccount, String switchPasswd, int targetTicket, SSOAccountObserver observer) {
//        throw new RuntimeException("Stub!");
//    }
//
//    public void ssoGetTicketNoPasswd(String ssoAccount, int targetTicket, SSOAccountObserver observer) {
//        throw new RuntimeException("Stub!");
//    }
//
//    public void ssoGetA1WithA1(String ssoAccount, byte[] dstAppName, long dwDstSsoVer, long dwDstAppid, long dwSubDstAppid, byte[] dstAppVer, byte[] dstAppSign, SSOAccountObserver observer) {
//        throw new RuntimeException("Stub!");
//    }
//
//    public void getSubAccountKey(String account, String subaccount, SubAccountObserver observer) {
//        throw new RuntimeException("Stub!");
//    }

    public MobileQQ getApplication() {
        return this.mContext;
    }

    public void login(final SimpleAccount newAccount) {
        runOnUiThread(new Runnable() {
            public void run() {
                AppRuntime.this.mContext.createNewRuntime(newAccount, true, true, 1);
            }
        });
    }

    public void logout(boolean needSendStatus) {
        logout(Constants.LogoutReason.user, needSendStatus);
    }

    protected void userLogoutReleaseData() {
    }

    void logout(final Constants.LogoutReason reason, boolean needSendStatus) {
        throw new RuntimeException("Stub!");
    }

    public void switchAccount(SimpleAccount newAccount) {
        if (newAccount != null) {
            this.mContext.createNewRuntime(newAccount, false, true, 2);
            return;
        }
        throw new IllegalArgumentException("the newAccount can not be null.");
    }

    public boolean isLogin() {
        return this.isLogin;
    }

    void setLogined() {
        this.isLogin = true;
    }

    public boolean isRunning() {
        throw new RuntimeException("Stub!");
    }

    public String getAccount() {
        throw new RuntimeException("Stub!");
    }

    public long getLongAccountUin() {
        throw new RuntimeException("Stub!");
    }

    public String getSid() {
        throw new RuntimeException("Stub!");
    }

//    public void startServlet(NewIntent intent) {
//        throw new RuntimeException("Stub!");
//    }

    public void sendAppDataIncermentMsg(String flowUin, String[] tag2, long flow) {
        throw new RuntimeException("Stub!");
    }

    public final void runOnUiThread(Runnable action) {
        throw new RuntimeException("Stub!");
    }

    public void sendOnlineStatus(Status s, boolean isKickPC) {
        throw new RuntimeException("Stub!");
    }

    public void onAppGuardModeChange(boolean beGuarded, int activeDegree, int memoryCost) {
        throw new RuntimeException("Stub!");
    }

    public void setCmdCallbacker() {
        throw new RuntimeException("Stub!");
    }

    public void reportNetworkException(int ExcepType) {
        throw new RuntimeException("Stub!");
    }

    public void sendWirelessPswReq(int cmd) {
        throw new RuntimeException("Stub!");
    }

    public void sendWirelessMeibaoReq(int cmd) {
        throw new RuntimeException("Stub!");
    }

    public synchronized Status getOnlineStatus() {
        throw new RuntimeException("Stub!");
    }
//
//    public void registObserver(BusinessObserver observer) {
//        throw new RuntimeException("Stub!");
//    }
//
//    public void unRegistObserver(BusinessObserver observer) {
//        throw new RuntimeException("Stub!");
//    }
//
//    public void notifyObservers(Class<? extends BusinessObserver> filter, int type, boolean isSuccess, Bundle data) {
//        throw new RuntimeException("Stub!");
//    }
//
//    void notifyObserver(BusinessObserver observer, int type, boolean isSuccess, Bundle data) {
//        throw new RuntimeException("Stub!");
//    }
//
//    protected Class<? extends MSFServlet>[] getMessagePushServlets() {
//        return null;
//    }

    synchronized void setOnlineStatus(Status onlineStatus2) {
        throw new RuntimeException("Stub!");
    }

    protected String[] getMessagePushSSOCommands() {
        return null;
    }

    protected void addManager(int name, Manager m) {
        throw new RuntimeException("Stub!");
    }

    public Manager getManager(int name) {
        throw new RuntimeException("Stub!");
    }

    protected void onCreate(Bundle savedInstanceState) {
        throw new RuntimeException("Stub!");
    }

    public void start(boolean inOnCreate) {
        throw new RuntimeException("Stub!");
    }

    protected void onDestroy() {
        throw new RuntimeException("Stub!");
    }

    protected void onRunningBackground(Bundle outState) {
        this.isBackground_Stop = true;
    }

    public void saveLastAccountState() {
        throw new RuntimeException("Stub!");
    }

    protected void onRunningForeground() {
        this.isBackground_Stop = false;
    }

    protected void finalize() throws Throwable {
        super.finalize();
        throw new RuntimeException("Stub!");
    }

    public void updateSubAccountLogin(String subAccount, boolean isLogined) {
        throw new RuntimeException("Stub!");
    }

    public final SharedPreferences getPreferences() {
        throw new RuntimeException("Stub!");
    }

    public byte[] getUinSign() {
        throw new RuntimeException("Stub!");
    }

    public Intent getKickIntent() {
        throw new RuntimeException("Stub!");
    }

    public void setKickIntent(Intent i) {
        throw new RuntimeException("Stub!");
    }

    public Intent getDevLockIntent() {
        throw new RuntimeException("Stub!");
    }

    public void setDevLockIntent(Intent i) {
        throw new RuntimeException("Stub!");
    }

    public enum Status {
        online,
        offline,
        away,
        invisiable,
        receiveofflinemsg
    }
}
