/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package top.linl.util.reflect;

import io.github.qauxv.util.Initiator;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MethodTool {

    private static final Map<String, Method> METHOD_CACHE = new HashMap<>();
    private TargetMethodInfo targetMethod;

    private MethodTool() {

    }

    public static MethodTool find(String findClassName) {
        MethodTool methodTool = new MethodTool();
        methodTool.targetMethod = new TargetMethodInfo();
        methodTool.targetMethod.findClassName = findClassName;
        return methodTool;
    }

    public static MethodTool find(Class<?> findClass) {
        MethodTool methodTool = new MethodTool();
        methodTool.targetMethod = new TargetMethodInfo();
        methodTool.targetMethod.findClass = findClass;
        return methodTool;
    }

    /**
     * 构建方法签名
     *
     * @return com.linl.get(Object, int)的格式
     */
    private static StringBuilder buildMethodSignature(String findClass, String methodName, Class<?>[] paramTypes, Class<?> returnType) {
        StringBuilder sb = new StringBuilder();
        sb.append(findClass).append(".").append(methodName).append("(");

        for (Class<?> type : paramTypes) {
            sb.append(type.getName()).append(",");
        }

        /*char splitChar = '\0';
        for (Class<?> type : paramTypes) {
            sb.append(splitChar);
            splitChar = ',';
            sb.append(type.getName());
        }*/
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.delete(sb.length() - 1, sb.length());
        }
        sb.append(")");
        if (returnType != null) {
            sb.append(returnType.getName());
        }
        return sb;
    }

    public MethodTool name(String name) {
        this.targetMethod.methodName = name;
        return this;
    }

    public MethodTool returnType(Class<?> returnType) {
        this.targetMethod.returnType = returnType;
        return this;
    }

    public MethodTool params(Class<?>... methodParamsType) {
        this.targetMethod.methodParams = methodParamsType;
        return this;
    }

    public <T> T call(Object target, Object... params) {
        try {
            Method method = get();
            return (T) method.invoke(target, params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Method get() {
        TargetMethodInfo target = this.targetMethod;

        //构造方法签名
        String signature = buildMethodSignature(target.findClassName, targetMethod.methodName, target.methodParams, target.returnType).toString();
        if (METHOD_CACHE.containsKey(signature)) {
            return METHOD_CACHE.get(signature);
        }

        try {
            for (Class<?> currentFindClass = target.findClass == null ? Initiator.loadClass(target.findClassName) : target.findClass;
                    currentFindClass != Object.class; currentFindClass = currentFindClass.getSuperclass()) {
                MethodFor:
                for (Method method : currentFindClass.getDeclaredMethods()) {
                    if ((method.getName().equals(target.methodName) || target.methodName == null) && (method.getReturnType().equals(target.returnType)
                            || target.returnType == null)) {
                        Class<?>[] methodParams = method.getParameterTypes();
                        if (methodParams.length == target.methodParams.length) {
                            for (int i = 0; i < methodParams.length; i++) {
                                if (target.methodParams[i] == Object.class) {
                                    continue;
                                }
                                if (!Objects.equals(methodParams[i], target.methodParams[i])) {
                                    continue MethodFor;
                                }
                                if (!CheckClassType.CheckClass(methodParams[i], target.methodParams[i])) {
                                    continue MethodFor;
                                }
                            }
                            method.setAccessible(true);
                            METHOD_CACHE.put(signature, method);
                            return method;
                        }
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        throw new ReflectException("没有查找到方法 : " + signature);
    }

    private static class TargetMethodInfo {

        public Class<?> findClass;
        public String findClassName;
        public String methodName;
        public Class<?> returnType;
        public Class<?>[] methodParams = new Class[0];
    }
}
