package com.hicore.ReflectUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;

public class MClass {
    private static HashMap<String,Class> clzMap;
    public static <T> T NewInstance(Class clz,Class[] paramTypes,Object... params) throws Exception{
        Loop:
        for (Constructor con : clz.getDeclaredConstructors()){
            Class[] CheckParam = con.getParameterTypes();
            if (CheckParam.length != paramTypes.length)continue;
            for (int i = 0;i < paramTypes.length;i++){
                if (!CheckClass(CheckParam[i],paramTypes[i])){
                    continue Loop;
                }
            }
            con.setAccessible(true);
            return (T) con.newInstance(params);
        }
        return null;
    }
    public static <T> T NewInstance(Class clz,Object... params) throws Exception{
        Class[] paramTypes = new Class[params.length];
        for (int i=0;i<params.length;i++){
            paramTypes[i] = params[i].getClass();
        }
        return NewInstance(clz,paramTypes,params);
    }
    public static boolean CheckClass(Class clz,Class convert){
        if (clz.equals(convert))return true;
        if (clz.equals(hasType(convert)))return true;
        if (clz.isAssignableFrom(convert))return true;
        return false;
    }
    private static Class hasType(Class clz){
        try{
            Field f = clz.getDeclaredField("TYPE");
            if (f.getType().equals(Class.class)){
                return (Class) f.get(null);
            }
            return null;
        }catch (Exception e){
            return null;
        }
    }
}
