package com.hicore.messageUtils;

import com.hicore.ReflectUtil.MClass;
import com.hicore.ReflectUtil.MMethod;
import java.lang.reflect.Method;

public class QQEnvUtils {

    public static String getCurrentUin() {
        try {
            Object AppRuntime = getAppRuntime();
            return MMethod.CallMethodNoParam(AppRuntime, "getCurrentAccountUin", String.class);
        } catch (Exception e) {
            return "";
        }
    }
    public static Object getAppRuntime() throws Exception {
        Object sApplication = MMethod.CallStaticMethod(MClass.loadClass("com.tencent.common.app.BaseApplicationImpl"),
                "getApplication", MClass.loadClass("com.tencent.common.app.BaseApplicationImpl"));

        return MMethod.CallMethodNoParam(sApplication, "getRuntime", MClass.loadClass("mqq.app.AppRuntime"));
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
}
