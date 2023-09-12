package cc.hicore.ui.handygridview.utils;

import android.text.TextUtils;
import java.lang.reflect.Method;

public class ReflectUtil {
    public static Object invokeMethod(Object targetObject, String methodName, Object[] params, Class[] paramTypes) {
        Object returnObj = null;
        if (targetObject == null || TextUtils.isEmpty(methodName)) {
            return null;
        }
        Method method = null;
        for (Class cls = targetObject.getClass(); cls != Object.class; cls = cls.getSuperclass()) {
            try {
                method = cls.getDeclaredMethod(methodName, paramTypes);
                break;
            } catch (Exception e) {
//                e.printStackTrace();
//                return null;
            }
        }
        if (method != null) {
            method.setAccessible(true);
            try {
                returnObj = method.invoke(targetObject, params);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return returnObj;
    }
}
