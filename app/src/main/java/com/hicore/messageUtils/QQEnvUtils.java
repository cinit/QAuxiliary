package com.hicore.messageUtils;

import com.hicore.ReflectUtil.MClass;
import com.hicore.ReflectUtil.MMethod;

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
}
