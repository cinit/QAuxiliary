package top.linl.util.reflect;


import io.github.qauxv.util.Initiator;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class ClassUtils {

    private static final Object[][] baseTypes = {{"int", int.class}, {"boolean", boolean.class}, {"byte", byte.class}, {"long", long.class},
            {"char", char.class}, {"double", double.class}, {"float", float.class}, {"short", short.class}, {"void", void.class}};
    private static ClassLoader ModuleLoader;//模块类加载器
    private static ClassLoader HostLoader;//宿主应用类加载器

    static {
        setHostClassLoader(Initiator.getHostClassLoader());
        setModuleLoader(Initiator.getPluginClassLoader());
    }

    /**
     * 获取基本类型
     */
    private static Class<?> getBaseTypeClass(String baseTypeName) {

        if (baseTypeName.length() == 1) {
            return findSimpleType(baseTypeName.charAt(0));
        }
        for (Object[] baseType : baseTypes) {
            if (baseTypeName.equals(baseType[0])) {
                return (Class<?>) baseType[1];
            }
        }
        throw new ReflectException(baseTypeName + " <-不是基本的数据类型");
    }

    /**
     * conversion base type
     *
     * @param simpleType Smali Base Type V,Z,B,I...
     */
    public static Class<?> findSimpleType(char simpleType) {
        switch (simpleType) {
            case 'V':
                return void.class;
            case 'Z':
                return boolean.class;
            case 'B':
                return byte.class;
            case 'S':
                return short.class;
            case 'C':
                return char.class;
            case 'I':
                return int.class;
            case 'J':
                return long.class;
            case 'F':
                return float.class;
            case 'D':
                return double.class;
        }
        throw new RuntimeException("Not an underlying type");
    }

    /**
     * 排除常用类
     */
    public static boolean isCommonlyUsedClass(String name) {
        return name.startsWith("androidx.") || name.startsWith("android.") || name.startsWith("kotlin.") || name.startsWith("kotlinx.") || name.startsWith(
                "com.tencent.mmkv.") || name.startsWith("com.android.tools.r8.") || name.startsWith("com.google.android.") || name.startsWith(
                "com.google.gson.") || name.startsWith("com.google.common.") || name.startsWith("com.microsoft.appcenter.") || name.startsWith(
                "org.intellij.lang.annotations.") || name.startsWith("org.jetbrains.annotations.");
    }

    /**
     * 获取类
     */
    public static Class<?> getClass(String className) {
        try {
            return HostLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setHostClassLoader(ClassLoader loader) {
        if (loader == null) {
            throw new ReflectException("类加载器为Null 无法设置");
        }
        HostLoader = new XClassLoader(loader);
    }

    public static ClassLoader getHostLoader() {
        return HostLoader;
    }

    public static ClassLoader getModuleLoader() {
        return ModuleLoader;
    }

    public static void setModuleLoader(ClassLoader loader) {
        ModuleLoader = loader;
    }

    public static class XClassLoader extends ClassLoader {

        private static final Map<String, Class<?>> CLASS_CACHE = new HashMap<>();
        private final ClassLoader oldClassLoader;

        private XClassLoader(ClassLoader classLoader) {
            super(classLoader);
            this.oldClassLoader = classLoader;
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            Class<?> clazz = CLASS_CACHE.get(name);
            if (clazz != null) {
                return clazz;
            }
            try {
                //可能是数组类型的
                if (name.startsWith("[")) {
                    int index = name.lastIndexOf('[');
                    //获取原类型
                    try {
                        clazz = getBaseTypeClass(name.substring(index + 1));
                    } catch (Exception e) {
                        clazz = oldClassLoader.loadClass(name.substring(index + 1));
                    }
                    //转换数组类型
                    for (int i = 0; i < name.length(); i++) {
                        char ch = name.charAt(i);
                        if (ch == '[') {
                            clazz = Array.newInstance(clazz, 0).getClass();
                        } else {
                            break;
                        }
                    }
                    CLASS_CACHE.put(name, clazz);
                    return clazz;
                }
                //可能是基础类型
                try {
                    clazz = getBaseTypeClass(name);
                } catch (Exception e) {
                    //因为默认的ClassLoader.load() 不能加载"int"这种类型
                    clazz = oldClassLoader.loadClass(name);
                }
                CLASS_CACHE.put(name, clazz);
                return clazz;
            } catch (Throwable throwable) {
                throw new ReflectException("没有找到类: " + name);
            }
        }

        @Override
        public URL getResource(String name) {
            return oldClassLoader.getResource(name);
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            return oldClassLoader.getResources(name);
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            return oldClassLoader.getResourceAsStream(name);
        }


        @Override
        public void setDefaultAssertionStatus(boolean enabled) {
            oldClassLoader.setDefaultAssertionStatus(enabled);
        }

        @Override
        public void setPackageAssertionStatus(String packageName, boolean enabled) {
            oldClassLoader.setPackageAssertionStatus(packageName, enabled);
        }

        @Override
        public void setClassAssertionStatus(String className, boolean enabled) {
            oldClassLoader.setClassAssertionStatus(className, enabled);
        }

        @Override
        public void clearAssertionStatus() {
            oldClassLoader.clearAssertionStatus();
        }
    }
}
