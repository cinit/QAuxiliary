package top.linl.util;

public class CheckClassType {

    //缩小范围匹配字节引用类型
    public static boolean CheckClass(Class<?> clz, Class<?> convert) {
        if (clz.equals(convert)) {
            return true;
        }
        if (clz.equals(hasType(convert))) {
            return true;
        }
        if (clz.isAssignableFrom(convert)) {
            return true;
        }
        return false;
    }

    private static Class<?> hasType(Class<?> clz) {
        try {
            if (clz.equals(Boolean.class)) {
                return boolean.class;
            }
            if (clz.equals(Integer.class)) {
                return int.class;
            }
            if (clz.equals(Long.class)) {
                return long.class;
            }
            if (clz.equals(Byte.class)) {
                return byte.class;
            }
            if (clz.equals(Short.class)) {
                return short.class;
            }
            if (clz.equals(Float.class)) {
                return float.class;
            }
            if (clz.equals(Double.class)) {
                return double.class;
            }
            if (clz.equals(Character.class)) {
                return char.class;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
