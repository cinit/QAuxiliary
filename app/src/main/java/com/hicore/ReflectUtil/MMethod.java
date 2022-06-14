package com.hicore.ReflectUtil;

import java.lang.reflect.Method;
import java.util.HashMap;

public class MMethod {

    public static <T> T CallMethodNoName(Object obj, Class<?> ReturnType, Class<?>[] ParamTypes, Object... params) throws Exception {
        Method method = FindMethod(obj.getClass(), null, ReturnType, ParamTypes);
        if (method == null) {
            StringBuilder builder = new StringBuilder();
            for (Class<?> clzErr : ParamTypes) {
                builder.append(clzErr.getName()).append(";");
            }
            builder.append(")").append(ReturnType.getName());
            throw new NoMethodError("No Such Method " + builder + " in class " + obj.getClass().getName());
        }
        return (T) method.invoke(obj, params);
    }

    public static <T> T CallMethodNoReturn(Object obj, String MethodName, Class<?>[] ParamTypes, Object... params) throws Exception {
        Method method = FindMethod(obj.getClass(), MethodName, ParamTypes);
        if (method == null) {
            StringBuilder builder = new StringBuilder(MethodName).append("(");
            for (Class<?> clzErr : ParamTypes) {
                builder.append(clzErr.getName()).append(";");
            }
            builder.append(")");
            throw new NoMethodError("No Such Method " + builder + " in class " + obj.getClass().getName());
        }
        return (T) method.invoke(obj, params);
    }

    public static <T, V> T CallMethodNoReturnSingle(Object obj, String MethodName, Class<?>[] ParamTypes, V param) throws Exception {
        return CallMethodNoReturn(obj, MethodName, ParamTypes, new Object[]{param});
    }

    public static <T> T CallMethod(Object obj, String MethodName, Class<?> ReturnType, Class<?>[] ParamTypes, Object... params) throws Exception {
        return CallMethod(obj, obj.getClass(), MethodName, ReturnType, ParamTypes, params);
    }

    public static <T> T CallMethod(Object obj, Class<?> ReturnType, Class<?>[] ParamTypes, Object... params) throws Exception {
        return CallMethodNoName(obj, ReturnType, ParamTypes, params);
    }

    public static <T> T CallMethodNoParam(Object obj, String MethodName, Class<?> ReturnType) throws Exception {
        return CallMethodParams(obj, MethodName, ReturnType);
    }

    public static <T, V> T CallMethodSingle(Object obj, String MethodName, Class<?> ReturnType, V Value) throws Exception {
        if (Value.getClass().isArray()) {
            return CallMethodParams(obj, MethodName, ReturnType, new Object[]{Value});
        } else {
            return CallMethodParams(obj, MethodName, ReturnType, Value);
        }

    }

    public static <T> T CallMethodParams(Object obj, String MethodName, Class<?> ReturnType, Object... params) throws Exception {
        Class<?>[] ParamTypes = new Class[params.length];
        for (int i = 0; i < ParamTypes.length; i++) {
            ParamTypes[i] = params[i].getClass();
        }
        return CallMethod(obj, obj.getClass(), MethodName, ReturnType, ParamTypes, params);
    }

    public static <T> T CallMethodNoParamType(Object obj, Class<?> clz, String MethodName, Class<?> ReturnType, Object... params) throws Exception {
        Class<?>[] ParamTypes = new Class[params.length];
        for (int i = 0; i < ParamTypes.length; i++) {
            ParamTypes[i] = params[i].getClass();
        }
        return CallMethod(obj, clz, MethodName, ReturnType, ParamTypes, params);
    }

    public static <T> T CallStaticMethod(Class<?> clz, String MethodName, Class<?> ReturnType, Object... params) throws Exception {
        return CallMethodNoParamType(null, clz, MethodName, ReturnType, params);
    }

    public static <T> T CallStaticMethodNoParam(Class<?> clz, String MethodName, Class<?> ReturnType) throws Exception {
        return CallMethodNoParamType(null, clz, MethodName, ReturnType);
    }

    public static <T> T CallMethod(Object obj, Class<?> clz, String MethodName, Class<?> ReturnType, Class<?>[] ParamTypes, Object... params) throws Exception {
        Method method = FindMethod(clz, MethodName, ReturnType, ParamTypes);
        if (method == null) {
            StringBuilder builder = new StringBuilder();

            builder.append(MethodName).append("(");
            for (Class<?> clzErr : ParamTypes) {
                builder.append(clzErr.getName()).append(";");
            }
            builder.append(")").append(ReturnType.getName());
            throw new NoMethodError("No Such Method " + builder + " in class " + clz.getName());
        }
        return (T) method.invoke(obj, params);
    }

    private static final HashMap<String, Method> MethodCache = new HashMap<>();

    public static Method FindMethod(String FindClass, String MethodName, Class<?> ReturnType, Class<?>[] ParamTypes) {
        return FindMethod(MClass.loadClass(FindClass), MethodName, ReturnType, ParamTypes);
    }

    public static Method FindMethod(Class<?> FindClass, String MethodName, Class<?> ReturnType, Class<?>[] ParamTypes) {
        if (FindClass == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(FindClass.getName()).append(".").append(MethodName).append("(");
        for (Class<?> clz : ParamTypes) {
            builder.append(clz.getName()).append(";");
        }
        builder.append(")").append(ReturnType.getName());
        String SignText = builder.toString();
        if (MethodCache.containsKey(SignText)) {
            return MethodCache.get(SignText);
        }

        Class<?> Current_Find = FindClass;
        while (Current_Find != null) {
            Loop:
            for (Method method : Current_Find.getDeclaredMethods()) {
                if ((method.getName().equals(MethodName) || MethodName == null) && method.getReturnType().equals(ReturnType)) {
                    Class<?>[] params = method.getParameterTypes();

                    if (params.length == ParamTypes.length) {
                        for (int i = 0; i < params.length; i++) {
                            if (!MClass.CheckClass(params[i], ParamTypes[i])) {
                                continue Loop;
                            }
                        }
                        MethodCache.put(SignText, method);
                        method.setAccessible(true);
                        return method;
                    }
                }
            }
            Current_Find = Current_Find.getSuperclass();
        }
        return null;
    }

    public static Method FindMethod(Class<?> FindClass, String MethodName, Class<?>[] ParamTypes) {
        if (FindClass == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(FindClass.getName()).append(".").append(MethodName).append("(");
        for (Class<?> clz : ParamTypes) {
            builder.append(clz.getName()).append(";");
        }
        builder.append(")ReturnTypeNotSet");
        String SignText = builder.toString();
        if (MethodCache.containsKey(SignText)) {
            return MethodCache.get(SignText);
        }

        Class<?> Current_Find = FindClass;
        while (Current_Find != null) {
            Loop:
            for (Method method : Current_Find.getDeclaredMethods()) {
                if ((method.getName().equals(MethodName) || MethodName == null)) {
                    Class<?>[] params = method.getParameterTypes();

                    if (params.length == ParamTypes.length) {
                        for (int i = 0; i < params.length; i++) {
                            if (!MClass.CheckClass(params[i], ParamTypes[i])) {
                                continue Loop;
                            }
                        }
                        MethodCache.put(SignText, method);
                        method.setAccessible(true);
                        return method;
                    }
                }
            }
            Current_Find = Current_Find.getSuperclass();
        }
        return null;
    }

    private static class NoMethodError extends RuntimeException {

        public NoMethodError(String message) {
            super(message);
        }
    }

    public static Method FindFirstMethod(Class clz, Class ReturnType, Class[] ParamTYPE) {
        Lopp:
        for (Method method : clz.getDeclaredMethods()) {
            if (method.getParameterTypes().length == ParamTYPE.length) {
                Class[] params = method.getParameterTypes();
                for (int i = 0; i < method.getParameterTypes().length; i++) {
                    if (!params[i].equals(ParamTYPE[i])) {
                        continue Lopp;
                    }
                }
                if (method.getReturnType().equals(ReturnType)) {
                    return method;
                }
            }
        }
        return null;
    }
}
