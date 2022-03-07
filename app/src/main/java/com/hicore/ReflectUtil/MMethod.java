/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
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

package com.hicore.ReflectUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MMethod {
    public static <T> T CallMethod(Object obj,Class ReturnType,Class[] ParamTypes,Object... params)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = FindMethod(obj.getClass(), null, ReturnType, ParamTypes);
        if (method == null) {
            StringBuilder builder = new StringBuilder();
            for (Class clzErr : ParamTypes)
                builder.append(clzErr.getName()).append(";");
            builder.append(")").append(ReturnType.getName());
            throw new NoSuchMethodException("No Such Method " + builder + " in class " + obj.getClass().getName());
        }
        return (T) method.invoke(obj, params);
    }
    public static Method FindMethod(Class FindClass,String MethodName,Class ReturnType,Class[] ParamTypes){

        Class Current_Find = FindClass;
        while (Current_Find != null){
            Loop:
            for(Method method : Current_Find.getDeclaredMethods()){
                if ((method.getName().equals(MethodName) || MethodName == null) && method.getReturnType().equals(ReturnType)){
                    Class[] params = method.getParameterTypes();

                    if (params.length == ParamTypes.length){
                        for (int i=0;i< params.length;i++){
                            if (!MClass.CheckClass(params[i],ParamTypes[i]))continue Loop;
                        }
                        method.setAccessible(true);
                        return method;
                    }
                }
            }
            Current_Find = Current_Find.getSuperclass();
        }
        return null;
    }
}
