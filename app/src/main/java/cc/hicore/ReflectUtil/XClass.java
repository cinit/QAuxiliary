package cc.hicore.ReflectUtil;

import io.github.qauxv.util.Initiator;
import java.lang.reflect.Constructor;

public class XClass {
    public static Class<?> load(String name) throws ClassNotFoundException {
        Class<?> findResult = null;
        try {
            findResult = Initiator.load(name);
            if (findResult == null) {
                throw new ClassNotFoundException(name);
            }
        } catch (ClassNotFoundException ignored) { }
        if (findResult == null){
            return Class.forName(name);
        }
        return findResult;
    }
    public static Class<?> loadEx(String name){
        try {
            return load(name);
        }catch (Exception e){
            return null;
        }
    }
    public static Constructor<?> getInit(Class<?> clz, Class<?>... paramTypes) throws NoSuchMethodException {
        Loop:
        for (Constructor<?> con : clz.getDeclaredConstructors()) {
            Class<?>[] CheckParam = con.getParameterTypes();
            if (CheckParam.length != paramTypes.length) continue;
            for (int i = 0; i < paramTypes.length; i++) {
                if (!CheckClass(CheckParam[i], paramTypes[i])) {
                    continue Loop;
                }
            }
            con.setAccessible(true);
            return con;
        }
        throw new NoSuchMethodException("No Instance for " + clz);
    }
    public static <T> T newInstance(Class<?> clz, Object... params) throws Exception {
        Class<?>[] paramTypes = new Class[params.length];
        for (int i = 0; i < params.length; i++) {
            paramTypes[i] = params[i].getClass();
        }
        return newInstance(clz, paramTypes, params);
    }
    public static <T> T newInstance(Class<?> clz, Class<?>[] paramTypes, Object... params) throws Exception {
        return (T) getInit(clz, paramTypes).newInstance(params);
    }
    protected static boolean CheckClass(Class<?> clz, Class<?> convert) {
        if (clz.equals(convert)) return true;
        if (clz.isAssignableFrom(hasType(convert))) return true;
        return clz.isAssignableFrom(convert);
    }
    private static Class<?> hasType(Class<?> clz) {
        try {

            if (clz.equals(Boolean.class)) return boolean.class;
            if (clz.equals(Integer.class)) return int.class;
            if (clz.equals(Long.class)) return long.class;
            if (clz.equals(Byte.class)) return byte.class;
            if (clz.equals(Short.class)) return short.class;
            if (clz.equals(Float.class)) return float.class;
            if (clz.equals(Double.class)) return double.class;
            if (clz.equals(Character.class)) return char.class;
            return clz;
        } catch (Exception e) {
            return null;
        }
    }
}
