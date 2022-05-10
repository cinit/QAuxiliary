package com.hicore.messageUtils;

import com.hicore.ReflectUtil.MClass;
import com.hicore.ReflectUtil.MField;
import com.hicore.ReflectUtil.MMethod;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class QQEnvUtils {

    private static final String TAG = "QQInfoUtils";

    public static int getTargetID(String IDName) {
        try {
            return MField.GetField(null, MClass.loadClass("com.tencent.mobileqq.R$id"), IDName, int.class);
        } catch (Exception e) {
            return 0;
        }
    }

    public static String getCurrentUin() {
        try {
            Object AppRuntime = getAppRuntime();
            return MMethod.CallMethodNoParam(AppRuntime, "getCurrentAccountUin", String.class);
        } catch (Exception e) {
            return "";
        }
    }

    public static String getCurrentTinyID() {
        try {
            Object IGpsService = getRuntimeService(MClass.loadClass("com.tencent.mobileqq.qqguildsdk.api.IGPSService"));
            return MMethod.CallMethodNoParam(IGpsService, "getSelfTinyId", String.class);
        } catch (Exception e) {
            return "";
        }
    }

    public static String getFriendName(String FriendUin) {
        try {
            Object FriendManager = MMethod.CallMethod(getAppRuntime(), "getManager",
                    MClass.loadClass("mqq.manager.Manager"),
                    new Class[]{int.class},
                    new Object[]{MField.GetField(null, MClass.loadClass("com.tencent.mobileqq.app.QQManagerFactory"), "FRIENDS_MANAGER", int.class)}
            );
            Object mService = MField
                    .GetFirstField(FriendManager, FriendManager.getClass(), MClass.loadClass("com.tencent.mobileqq.friend.api.IFriendDataService"));
            Object friend = MMethod.CallMethodSingle(mService, "getFriend", MClass.loadClass("com.tencent.mobileqq.data.Friends"), FriendUin);
            return MField.GetField(friend, "name", String.class);
        } catch (Exception e) {
            return FriendUin;
        }


    }

    public static ArrayList<FriendInfo> getFriendList() {
        try {
            Object FriendManager = MMethod.CallMethod(getAppRuntime(), "getManager",
                    MClass.loadClass("mqq.manager.Manager"),
                    new Class[]{int.class},
                    new Object[]{MField.GetField(null, MClass.loadClass("com.tencent.mobileqq.app.QQManagerFactory"), "FRIENDS_MANAGER", int.class)}
            );
            Object mService = MField
                    .GetFirstField(FriendManager, FriendManager.getClass(), MClass.loadClass("com.tencent.mobileqq.friend.api.IFriendDataService"));
            ArrayList friendList = MMethod.CallMethodNoParam(mService, "getAllFriends", List.class);
            ArrayList<FriendInfo> NewInfos = new ArrayList<>();
            for (Object friends : friendList) {
                FriendInfo NewItem = new FriendInfo();
                NewItem.Name = MField.GetField(friends, "name", String.class);
                NewItem.Uin = MField.GetField(friends, "uin", String.class);
                NewItem.GroupID = MField.GetField(friends, "groupid", int.class);
                NewInfos.add(NewItem);
            }
            return NewInfos;
        } catch (Exception e) {
        }

        return null;
    }

    public static class FriendInfo {

        public String Uin;
        public String Name;
        public int GroupID;
        public Object source;
    }

    public static Object getAppRuntime() throws Exception {
        Object sApplication = MMethod.CallStaticMethod(MClass.loadClass("com.tencent.common.app.BaseApplicationImpl"),
                "getApplication", MClass.loadClass("com.tencent.common.app.BaseApplicationImpl"));

        return MMethod.CallMethodNoParam(sApplication, "getRuntime", MClass.loadClass("mqq.app.AppRuntime"));
    }

    public static Object GetIGpsManager() throws Exception {
        return getRuntimeService(MClass.loadClass("com.tencent.mobileqq.qqguildsdk.api.IGPSService"));
    }

    public static Object getRuntimeService(Class<?> Clz) throws Exception {
        Method Invoked = null;
        for (Method fs : getAppRuntime().getClass().getSuperclass().getSuperclass().getSuperclass().getDeclaredMethods()) {
            if (fs.getName().equals("getRuntimeService")) {
                Invoked = fs;
                break;
            }
        }
        Object MessageFacade = Invoked.invoke(getAppRuntime(), Clz, "");
        return MessageFacade;
    }

    public static void sendLike(String QQUin, int Count) {
        byte[] sCookie = {12, 24, 0, 1, 6, 1, 49, 22, 1, 49};
        long Selfuin = Long.parseLong(getCurrentUin());
        long TargetUin = Long.parseLong(QQUin);

        try {
            Method m = MMethod.FindMethod("com.tencent.mobileqq.app.CardHandler", "a", void.class, new Class[]{
                    long.class, long.class, byte[].class, int.class, int.class, int.class
            });
            Object CardHandler = MMethod.CallMethodSingle(getAppRuntime(), "getBusinessHandler",
                    MClass.loadClass("com.tencent.mobileqq.app.BusinessHandler"),
                    MField.GetStaticField(MClass.loadClass("com.tencent.mobileqq.app.BusinessHandlerFactory"), "CARD_HANLDER"));

            m.invoke(CardHandler, Selfuin, TargetUin, sCookie, IsFriends(QQUin) ? 1 : 10, Count, 0);
        } catch (Exception exception) {
        }
    }

    public static boolean IsFriends(String uin) {
        try {
            Object FriendsManager = MMethod.CallMethod(null, MClass.loadClass("com.tencent.mobileqq.friend.api.impl.FriendDataServiceImpl"), "getService",
                    MClass.loadClass("com.tencent.mobileqq.friend.api.impl.FriendDataServiceImpl"), new Class[]{MClass.loadClass("mqq.app.AppRuntime")},
                    QQEnvUtils.getAppRuntime()
            );
            return MMethod.CallMethod(FriendsManager, "isFriend", boolean.class, new Class[]{String.class}, uin);
        } catch (Exception exception) {
            return false;
        }
    }

    public static long GetServerTime() throws Exception {
        return MMethod.CallStaticMethodNoParam(MClass.loadClass("com.tencent.mobileqq.msf.core.NetConnInfoCenter"), "getServerTime", long.class);
    }

    public static Object getBusinessHandler(String Clz) throws Exception {
        Method Invoked = null;
        for (Method fs : getAppRuntime().getClass().getSuperclass().getSuperclass().getDeclaredMethods()) {
            if (fs.getName().equals("getBusinessHandler")) {
                Invoked = fs;
                break;
            }
        }
        Object MessageFacade = Invoked.invoke(getAppRuntime(), Clz);
        return MessageFacade;
    }

}
