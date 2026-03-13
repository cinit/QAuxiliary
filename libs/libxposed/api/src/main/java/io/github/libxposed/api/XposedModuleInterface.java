package io.github.libxposed.api;

import android.app.AppComponentFactory;
import android.content.pm.ApplicationInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import io.github.libxposed.api.annotations.XposedApiExact;
import io.github.libxposed.api.annotations.XposedApiMin;

/**
 * Interface for module initialization.
 */
@SuppressWarnings("unused")
public interface XposedModuleInterface {
    /**
     * Wraps information about the process in which the module is loaded.
     */
    interface ModuleLoadedParam {
        /**
         * Returns whether the current process is system server.
         *
         * @return {@code true} if the current process is system server
         */
        boolean isSystemServer();

        /**
         * Gets the process name.
         *
         * @return The process name
         */
        @NonNull
        String getProcessName();
    }

    /**
     * Wraps information about system server.
     */
    @XposedApiExact(100)
    interface SystemServerLoadedParam {
        /**
         * Gets the class loader of system server.
         *
         * @return The class loader
         */
        @NonNull
        ClassLoader getClassLoader();
    }

    /**
     * Wraps information about the package being loaded.
     */
    interface PackageLoadedParam {
        /**
         * Gets the package name of the current package.
         *
         * @return The package name.
         */
        @NonNull
        String getPackageName();

        /**
         * Gets the {@link ApplicationInfo} of the current package.
         *
         * @return The ApplicationInfo.
         */
        @NonNull
        ApplicationInfo getApplicationInfo();

        /**
         * Returns whether this is the first and main package loaded in the app process.
         *
         * @return {@code true} if this is the first package.
         */
        boolean isFirstPackage();

        /**
         * Gets the default classloader of the current package. This is the classloader that loads
         * the app's code, resources and custom {@link AppComponentFactory}.
         */
        @RequiresApi(Build.VERSION_CODES.Q)
        @NonNull
        ClassLoader getDefaultClassLoader();

        /**
         * Gets the class loader of the package being loaded.
         *
         * @return The class loader.
         */
        @XposedApiExact(100)
        @NonNull
        ClassLoader getClassLoader();

    }

    /**
     * Wraps information about the package whose classloader is ready.
     */
    @XposedApiMin(101)
    interface PackageReadyParam extends PackageLoadedParam {
        /**
         * Gets the classloader of the current package. It may be different from {@link #getDefaultClassLoader()}
         * if the package has a custom {@link android.app.AppComponentFactory} that creates a different classloader.
         */
        @NonNull
        ClassLoader getClassLoader();

        /**
         * Gets the {@link AppComponentFactory} of the current package.
         */
        @RequiresApi(Build.VERSION_CODES.P)
        @NonNull
        AppComponentFactory getAppComponentFactory();
    }

    /**
     * Wraps information about system server.
     */
    @XposedApiMin(101)
    interface SystemServerStartingParam {
        /**
         * Gets the class loader of system server.
         */
        @NonNull
        ClassLoader getClassLoader();
    }


    /**
     * Gets notified when the module is loaded into the target process.<br/>
     * This callback is guaranteed to be called exactly once for a process.
     *
     * @param param Information about the process in which the module is loaded
     */
    @XposedApiMin(101)
    default void onModuleLoaded(@NonNull ModuleLoadedParam param) {
    }

    /**
     * Gets notified when a package is loaded into the app process. This is the time when the default
     * classloader is ready but before the instantiation of custom {@link android.app.AppComponentFactory}.<br/>
     * This callback could be invoked multiple times for the same process on each package.
     *
     * @param param Information about the package being loaded
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    default void onPackageLoaded(@NonNull PackageLoadedParam param) {
    }

    /**
     * Gets notified when custom {@link android.app.AppComponentFactory} has instantiated the app
     * classloader and is ready to create {@link android.app.Activity} and {@link android.app.Service}.<br/>
     * This callback could be invoked multiple times for the same process on each package.
     *
     * @param param Information about the package being loaded
     */
    @XposedApiMin(101)
    default void onPackageReady(@NonNull PackageReadyParam param) {
    }

    /**
     * Gets notified when system server is ready to start critical services.
     *
     * @param param Information about system server
     */
    @XposedApiMin(101)
    default void onSystemServerStarting(@NonNull SystemServerStartingParam param) {
    }

    /**
     * Gets notified when the system server is loaded.
     *
     * @param param Information about system server
     */
    @XposedApiExact(100)
    default void onSystemServerLoaded(@NonNull SystemServerLoadedParam param) {
    }
}
