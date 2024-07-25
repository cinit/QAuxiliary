
package io.github.qauxv.startup;

import android.content.Context;

public class HybridClassLoader extends ClassLoader {

    private static final ClassLoader sBootClassLoader = Context.class.getClassLoader();

    public static final HybridClassLoader INSTANCE = new HybridClassLoader();

    private HybridClassLoader() {
        super(sBootClassLoader);
    }

    private static ClassLoader sLoaderParentClassLoader;
    private static ClassLoader sHostClassLoader;

    public static void setLoaderParentClassLoader(ClassLoader loaderClassLoader) {
        if (loaderClassLoader == HybridClassLoader.class.getClassLoader()) {
            sLoaderParentClassLoader = null;
        } else {
            sLoaderParentClassLoader = loaderClassLoader;
        }
    }

    public static void setHostClassLoader(ClassLoader hostClassLoader) {
        sHostClassLoader = hostClassLoader;
    }

    public static ClassLoader getHostClassLoader() {
        return sHostClassLoader;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name == null) {
            return super.loadClass(null, resolve);
        }
        try {
            return sBootClassLoader.loadClass(name);
        } catch (ClassNotFoundException ignored) {
        }
        if (sLoaderParentClassLoader != null && name.startsWith("io.github.qauxv.loader.")) {
            return sLoaderParentClassLoader.loadClass(name);
        }
        if (isConflictingClass(name)) {
            // Nevertheless, this will not interfere with the host application,
            // classes in host application SHOULD find with their own ClassLoader, eg Class.forName()
            // use shipped androidx and kotlin lib.
            throw new ClassNotFoundException(name);
        }
        // The ClassLoader for some apk-modifying frameworks are terrible, XposedBridge.class.getClassLoader()
        // is the sane as Context.getClassLoader(), which mess up with 3rd lib, can cause the ART to crash.
        if (sLoaderParentClassLoader != null) {
            try {
                return sLoaderParentClassLoader.loadClass(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        if (sHostClassLoader != null) {
            try {
                return sHostClassLoader.loadClass(name);
            } catch (ClassNotFoundException e) {
                return super.loadClass(name, resolve);
            }
        }
        return super.loadClass(name, resolve);
    }

    /**
     * 把宿主和模块共有的 package 扔这里.
     *
     * @param name NonNull, class name
     * @return true if conflicting
     */
    public static boolean isConflictingClass(String name) {
        return name.startsWith("androidx.") || name.startsWith("android.support.")
                || name.startsWith("kotlin.") || name.startsWith("kotlinx.")
                || name.startsWith("com.tencent.mmkv.")
                || name.startsWith("com.android.tools.r8.")
                || name.startsWith("com.google.android.")
                || name.startsWith("com.google.gson.")
                || name.startsWith("com.google.common.")
                || name.startsWith("com.google.protobuf.")
                || name.startsWith("com.microsoft.appcenter.")
                || name.startsWith("org.intellij.lang.annotations.")
                || name.startsWith("org.jetbrains.annotations.")
                || name.startsWith("com.bumptech.glide.")
                || name.startsWith("com.google.errorprone.annotations.")
                || name.startsWith("org.jf.dexlib2.")
                || name.startsWith("org.jf.util.")
                || name.startsWith("javax.annotation.")
                || name.startsWith("_COROUTINE.");
    }

}
