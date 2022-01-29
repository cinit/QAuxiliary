package mqq.app;

public interface Constants {

    public static final String FILE_INSTANCE_STATE = "savedInstanceState";
    public static final String OPEN_SDK = "com.tencent.mobileqq:openSdk";

    public enum Key {
        nickname,
        _logintime,
        SvcRegister_timeStamp,
        currentAccount
    }

    public enum LogoutReason {
        user,
        expired,
        gray,
        kicked,
        tips,
        suspend,
        forceLogout,
        switchAccount,
        secKicked
    }

    public enum PropertiesKey {
        uinDisplayName,
        nickName;

        public String toString() {
            return name();
        }
    }

    public interface Action {

        public static final int ACTION_APP_GUARD = 2200;
        public static final int ACTION_CHANGE_TOKEN = 1032;
        public static final int ACTION_DELETE_ACCOUNT = 1007;
        public static final int ACTION_GET_ALTER_TICKETS = 2124;
        public static final int ACTION_GET_KEY = 1030;
        public static final int ACTION_GET_PLUGIN_CONFIG = 1018;
        public static final int ACTION_GET_RICH_MEDIA_SERVER_LIST = 1011;
        public static final int ACTION_GET_TICKET_NO_PASSWD = 1101;
        public static final int ACTION_LOGIN = 1001;
        public static final int ACTION_NET_EXCEPTION_EVENT = 1042;
        public static final int ACTION_NOTIFY_REFRESH_WEBVIEW_TICKET = 2123;
        public static final int ACTION_QUERY_SMS_STATE = 1022;
        public static final int ACTION_REGISTNEWACCOUNT_COMMITMOBILE = 1003;
        public static final int ACTION_REGISTNEWACCOUNT_COMMITSMS = 1004;
        public static final int ACTION_REGISTNEWACCOUNT_QUERYMOBILE = 1041;
        public static final int ACTION_REGISTNEWACCOUNT_REFETCH_SMS = 1020;
        public static final int ACTION_REGISTNEWACCOUNT_SEND_PASSWORD = 1005;
        public static final int ACTION_REGIST_COMMAND_PUSH = 1040;
        public static final int ACTION_REGIST_MESSAGE_PUSH = 1002;
        public static final int ACTION_REGIST_MESSAGE_PUSH_PROXY = 1025;
        public static final int ACTION_REPORT = 1012;
        public static final int ACTION_SEND_WIRELESS_MEIBAOREQ = 1044;
        public static final int ACTION_SEND_WIRELESS_PSWREQ = 1043;
        public static final int ACTION_SSO_GET_A1_WITH_A1 = 1102;
        public static final int ACTION_SSO_LOGIN_ACCOUNT = 1100;
        public static final int ACTION_SUBACCOUNT_GETKEY = 1037;
        public static final int ACTION_SUBACCOUNT_LOGIN = 1035;
        public static final int ACTION_UNREGIST_MESSAGE_PUSH_PROXY = 1026;
        public static final int ACTION_UNREGIST_PROXY = 1028;
        public static final int ACTION_VERITYCODE_CLOSE = 1024;
        public static final int ACTION_VERITYCODE_RECV = 1023;
        public static final int ACTION_WTLOGIN_AskDevLockSms = 2109;
        public static final int ACTION_WTLOGIN_CheckDevLockSms = 2110;
        public static final int ACTION_WTLOGIN_CheckDevLockStatus = 2108;
        public static final int ACTION_WTLOGIN_CheckPictureAndGetSt = 2102;
        public static final int ACTION_WTLOGIN_CheckSMSAndGetSt = 2113;
        public static final int ACTION_WTLOGIN_CheckSMSAndGetStExt = 2114;
        public static final int ACTION_WTLOGIN_CheckSMSVerifyLoginAccount = 2118;
        public static final int ACTION_WTLOGIN_CloseCode = 2105;
        public static final int ACTION_WTLOGIN_CloseDevLock = 2111;
        public static final int ACTION_WTLOGIN_Exception = 2107;
        public static final int ACTION_WTLOGIN_GetA1WithA1 = 2106;
        public static final int ACTION_WTLOGIN_GetStViaSMSVerifyLogin = 2121;
        public static final int ACTION_WTLOGIN_GetStWithPasswd = 2100;
        public static final int ACTION_WTLOGIN_GetStWithoutPasswd = 2101;
        public static final int ACTION_WTLOGIN_GetSubaccountStViaSMSVerifyLogin = 2122;
        public static final int ACTION_WTLOGIN_RefreshPictureData = 2103;
        public static final int ACTION_WTLOGIN_RefreshSMSData = 2112;
        public static final int ACTION_WTLOGIN_RefreshSMSVerifyLoginCode = 2119;
        public static final int ACTION_WTLOGIN_RegGetSMSVerifyLoginAccount = 2117;
        public static final int ACTION_WTLOGIN_VerifyCode = 2104;
        public static final int ACTION_WTLOGIN_VerifySMSVerifyLoginCode = 2120;
        public static final int ACTION_WTLOGIN_setRegDevLockFlag = 2125;
    }
}
