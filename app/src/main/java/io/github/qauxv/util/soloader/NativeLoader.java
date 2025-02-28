/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2024 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is an opensource software: you can redistribute it
 * and/or modify it under the terms of the General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the General Public License for more details.
 *
 * You should have received a copy of the General Public License
 * along with this software.
 * If not, see
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package io.github.qauxv.util.soloader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Process;
import android.system.Os;
import android.system.StructUtsname;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HostInfo;
import dalvik.system.BaseDexClassLoader;
import io.github.qauxv.BuildConfig;
import io.github.qauxv.core.NativeCoreBridge;
import io.github.qauxv.loader.hookapi.IHookBridge;
import io.github.qauxv.poststartup.StartupInfo;
import io.github.qauxv.util.IoUtils;
import io.github.qauxv.util.LoaderExtensionHelper;
import io.github.qauxv.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipFile;

@Keep
public class NativeLoader {

    // There are two logical native libraries in QAuxiliary:
    // Let's say that user is running an arm64 QQ on an x86_64 device.
    // The primary native library is x86_64, and the secondary native library is arm64.
    // The primary native consists of MMKV, Dobby(x86_64), and LSPlant(x86_64), and it may or may hook linker and/or ART.
    // The secondary native library consists of Dobby(arm64), and it may hook the arm64 libkernel.so.
    // The two native libraries are loaded into the same process, but they are in different linker namespaces.
    // For a user running an arm64 QQ on an arm64 device, the primary native library is arm64, and the secondary native library is arm64.
    // There will be only one native library in the process, and it is the primary native library, but it can be considered "primary+secondary" in logic.
    // primary native library: libqauxv-core0.so
    // secondary native library: libqauxv-core1.so (not the real name, just for illustration, the real name is also libqauxv-core0.so)
    // For a typical case in host process, the sequence of loading is:
    // stage 1: primary load, no context available, e.g. postSpecialize: -> load "primary" + "primary" pre-init, register native methods
    // stage 2: primary pre-init, no context available, this happens right after primary load: -> init Dobby, init LSPlant
    //          at this stage, Dobby and LSPlant are initialized, ART/linker hook available, but MMKV is not initialized
    // stage 3: context available, e.g. Application.onCreate: -> determine whether we need a secondary native library
    //                                                        -> load "secondary" if needed + init "primary" + init "secondary"
    //          at this stage, MMKV is initialized, and the secondary native library is loaded if needed
    //          note that the secondary native library also initializes MMKV, although no Java bindings.
    // just after 3rd stage, both primary and secondary native libraries are fully initialized, everything is ready
    // Note only the primary native library has pre-init. The secondary native library does not have pre-init.
    //
    // The implementation details are as follows:
    // primary pre-init: get the actual class loader, register native methods, init Dobby, init LSPlant
    // primary init: init MMKV
    // secondary init: init Dobby(arm64)

    private NativeLoader() {
        throw new AssertionError("No instances for you!");
    }

    public static final int ELF_CLASS_32 = 1;
    public static final int ELF_CLASS_64 = 2;
    public static final int ISA_NONE = 0;
    // EM_386 = 3
    public static final int ISA_X86 = (ELF_CLASS_32 << 16) | 3;
    // EM_X86_64 = 62
    public static final int ISA_X86_64 = (ELF_CLASS_64 << 16) | 62;
    // EM_ARM = 40
    public static final int ISA_ARM = (ELF_CLASS_32 << 16) | 40;
    // EM_AARCH64 = 183
    public static final int ISA_ARM64 = (ELF_CLASS_64 << 16) | 183;
    // EM_MIPS = 8
    public static final int ISA_MIPS = (ELF_CLASS_32 << 16) | 8;
    public static final int ISA_MIPS64 = (ELF_CLASS_64 << 16) | 8;
    // EM_RISCV = 243
    public static final int ISA_RISCV64 = (ELF_CLASS_64 << 16) | 243;

    private static int sCurrentRuntimeIsa = ISA_NONE;

    public static int getCurrentRuntimeIsa() {
        if (sCurrentRuntimeIsa == ISA_NONE) {
            try (FileInputStream fis = new FileInputStream("/proc/self/exe")) {
                // we only need the first 32 bytes
                byte[] header = new byte[32];
                IoUtils.readExactly(fis, header, 0, header.length);
                int isa = getIsaFromElfHeader(header);
                // is an ISA we support?
                if (isa == ISA_X86 || isa == ISA_X86_64 || isa == ISA_ARM || isa == ISA_ARM64 || isa == ISA_MIPS || isa == ISA_MIPS64 || isa == ISA_RISCV64) {
                    sCurrentRuntimeIsa = isa;
                    return isa;
                } else {
                    throw new IllegalArgumentException("Unsupported ISA: " + isa);
                }
            } catch (IOException e) {
                throw IoUtils.unsafeThrow(e);
            }
        }
        return sCurrentRuntimeIsa;
    }

    public static int getIsaFromElfHeader(@NonNull byte[] header) {
        if (header.length < 32) {
            throw new IllegalArgumentException("Invalid ELF header: length < 32");
        }
        if (header[0] != (byte) 0x7f || header[1] != (byte) 'E' || header[2] != (byte) 'L' || header[3] != (byte) 'F') {
            throw new IllegalArgumentException("Invalid ELF heade: bad magic");
        }
        int elfClass = header[4];
        if (elfClass != ELF_CLASS_32 && elfClass != ELF_CLASS_64) {
            throw new IllegalArgumentException("Invalid ELF header: bad class: " + elfClass);
        }
        int offsetMachine = 16 + 2;
        byte m0 = header[offsetMachine];
        byte m1 = header[offsetMachine + 1];
        int machine = ((m1 << 8) & 0xff00) | (m0 & 0xff);
        return (elfClass << 16) | machine;
    }

    public static int getIsaFromName(@NonNull String name) {
        switch (name) {
            case "x86":
            case "i386":
            case "i486":
            case "i586":
            case "i686":
                return ISA_X86;
            case "x86_64":
            case "amd64":
                return ISA_X86_64;
            case "arm":
            case "armhf":
            case "armv7l":
            case "armeabi":
            case "armeabi-v7a":
            case "armv8l":
                // armv8l is ARMv8 CPU in 32-bit compatibility mode
                return ISA_ARM;
            case "aarch64":
            case "arm64":
            case "arm64-v8a":
                return ISA_ARM64;
            case "mips":
            case "mipsel":
                return ISA_MIPS;
            case "mips64":
                return ISA_MIPS64;
            case "riscv64":
                return ISA_RISCV64;
            default:
                throw new IllegalArgumentException("Unsupported ISA: " + name);
        }
    }

    @NonNull
    public static String getIsaName(int isa) {
        switch (isa) {
            case ISA_NONE:
                return "none";
            case ISA_X86:
                return "x86";
            case ISA_X86_64:
                return "x86_64";
            case ISA_ARM:
                return "arm";
            case ISA_ARM64:
                return "arm64";
            case ISA_MIPS:
                return "mips";
            case ISA_MIPS64:
                return "mips64";
            case ISA_RISCV64:
                return "riscv64";
            default:
                return "unknown(" + (isa >> 16) + ":" + (isa & 0xffff) + ")";
        }
    }

    public static String getNativeLibraryDirName(int isa) {
        switch (isa) {
            case ISA_X86:
                return "x86";
            case ISA_X86_64:
                return "x86_64";
            case ISA_ARM:
                // we only support armeabi-v7a, not armeabi
                return "armeabi-v7a";
            case ISA_ARM64:
                return "arm64-v8a";
            case ISA_MIPS:
                // not sure, I have never seen a mips device
                return "mips";
            case ISA_MIPS64:
                // not sure, I have never seen a mips64 device
                return "mips64";
            case ISA_RISCV64:
                // not sure, I have never seen a riscv64 device
                return "riscv64";
            default:
                throw new IllegalArgumentException("Unsupported ISA: " + isa);
        }
    }

    public static int getApplicationIsa(@NonNull ApplicationInfo ai) {
        Objects.requireNonNull(ai);
        try {
            // should work on Android 5.0+
            // it's still a hidden/unsupported field on SDK 35, not in blocked fields list
            // TODO: 2024-08-08 find a way to get the primary CPU ABI of an application w/o using hidden API
            @SuppressLint("DiscouragedPrivateApi")
            Field field = ApplicationInfo.class.getDeclaredField("primaryCpuAbi");
            field.setAccessible(true);
            String primaryCpuAbi = (String) field.get(ai);
            return getIsaFromName(primaryCpuAbi);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw IoUtils.unsafeThrow(e);
        }
    }

    private static ClassLoader sPrimaryNativeLibraryNativeLoader = null;

    private static synchronized ClassLoader getIsolatedPrimaryNativeLibraryNativeLoader() {
        if (sPrimaryNativeLibraryNativeLoader == null) {
            String nativeLibraryDir = System.getProperty("java.library.path");
            String dexPath = StartupInfo.getModulePath();
            ClassLoader reference = NativeLoader.class.getClassLoader();
            Objects.requireNonNull(dexPath, "dexPath");
            Objects.requireNonNull(nativeLibraryDir, "java.library.path");
            Objects.requireNonNull(reference, "reference");
            sPrimaryNativeLibraryNativeLoader = new NativeLoaderInvokerClassLoader(dexPath, nativeLibraryDir, reference);
        }
        return sPrimaryNativeLibraryNativeLoader;
    }

    private static ClassLoader sSecondaryNativeLibraryNativeLoader = null;

    @SuppressLint("DiscouragedPrivateApi")
    private static synchronized ClassLoader getIsolatedSecondaryNativeLibraryNativeLoader(@NonNull Context ctx) {
        if (sSecondaryNativeLibraryNativeLoader == null) {
            Objects.requireNonNull(ctx, "context");
            // find a BaseDexClassLoader in the context
            ClassLoader hostClassLoader = ctx.getClassLoader();
            while (hostClassLoader != null && !(hostClassLoader instanceof BaseDexClassLoader)) {
                hostClassLoader = hostClassLoader.getParent();
            }
            if (hostClassLoader == null) {
                throw new IllegalStateException("Cannot find a BaseDexClassLoader in the context");
            }
            String nativeLibraryDir = ctx.getApplicationInfo().nativeLibraryDir;
            String dexPath = StartupInfo.getModulePath();
            ClassLoader reference = NativeLoader.class.getClassLoader();
            assert reference != null;
            ClassLoader dexLoader = new NativeLoaderInvokerClassLoader(dexPath, null, reference);
            ClassLoader nativeLoader = new NativeLoaderInvokerStubClassLoader(dexLoader, hostClassLoader, nativeLibraryDir);
            sSecondaryNativeLibraryNativeLoader = nativeLoader;
        }
        return sSecondaryNativeLibraryNativeLoader;
    }

    private static volatile boolean sPrimaryNativeLibraryLoaded = false;
    private static volatile boolean sPrimaryNativeLibraryAttached = false;
    private static volatile boolean sPrimaryNativeLibraryPreInitialized = false;
    private static volatile boolean sPrimaryNativeLibraryFullInitialized = false;
    private static volatile Boolean sIsSecondaryNativeLibraryNeeded = null;
    private static volatile boolean sSecondaryNativeLibraryLoaded = false;
    private static volatile boolean sSecondaryNativeLibraryInitialized = false;
    private static volatile Set<Integer> sModuleSupportedIsas = null;
    private static volatile long sPrimaryNativeLibraryHandle = 0;
    private static volatile long sSecondaryNativeLibraryHandle = 0;

    public static Set<Integer> getModuleSupportedIsas() {
        if (sModuleSupportedIsas == null) {
            synchronized (NativeLoader.class) {
                final String soname = "libqauxv-core0.so";
                if (sModuleSupportedIsas == null) {
                    String path = StartupInfo.getModulePath();
                    // open a zip file
                    HashSet<Integer> isas = new HashSet<>();
                    try (ZipFile zipFile = new ZipFile(path)) {
                        // check the ABI of the native libraries
                        for (int isa : new int[]{ISA_X86, ISA_X86_64, ISA_ARM, ISA_ARM64, ISA_MIPS, ISA_MIPS64, ISA_RISCV64}) {
                            String dir = "lib/" + getNativeLibraryDirName(isa) + "/" + soname;
                            if (zipFile.getEntry(dir) != null) {
                                isas.add(isa);
                            }
                        }
                    } catch (IOException e) {
                        throw IoUtils.unsafeThrow(e);
                    }
                    sModuleSupportedIsas = isas;
                }
            }
        }
        return sModuleSupportedIsas;
    }

    public static void loadPrimaryNativeLibrary(@NonNull File dataDir, @Nullable ApplicationInfo hostAppInfo) {
        if (sPrimaryNativeLibraryAttached) {
            return;
        }
        if (StartupInfo.isInHostProcess()) {
            loadPrimaryNativeLibraryInHost(dataDir, hostAppInfo);
        } else {
            // in my own app_process, it's so easy
            System.loadLibrary("qauxv-core0");
            sPrimaryNativeLibraryLoaded = true;
            try {
                Class.forName("io.github.qauxv.isolated.soloader.LoadLibraryInvoker", false, NativeLoader.class.getClassLoader())
                        .getMethod("invokeAttachClassLoader", ClassLoader.class)
                        .invoke(null, NativeLoader.class.getClassLoader());
                sPrimaryNativeLibraryHandle = nativeGetPrimaryNativeLibraryHandle();
                if (sPrimaryNativeLibraryHandle == 0) {
                    throw new AssertionError("nativeGetPrimaryNativeLibraryHandle returned 0");
                }
                sPrimaryNativeLibraryAttached = true;
            } catch (ReflectiveOperationException e) {
                // should not happen
                throw IoUtils.unsafeThrowForIteCause(e);
            }
        }
    }

    public static void loadSecondaryNativeLibrary(@NonNull Context context) {
        if (!StartupInfo.isInHostProcess()) {
            throw new IllegalStateException("Secondary native library can only be loaded in host process");
        }
        if (!sPrimaryNativeLibraryFullInitialized) {
            throw new IllegalStateException("Primary native library must be fully initialized before loading secondary native library");
        }
        if (!isSecondaryNativeLibraryNeeded(context)) {
            return;
        }
        if (sSecondaryNativeLibraryLoaded) {
            return;
        }
        int appIsa = getApplicationIsa(context.getApplicationInfo());
        // check whether we support the application's ISA
        if (!getModuleSupportedIsas().contains(appIsa)) {
            Set<Integer> supportedIsas = getModuleSupportedIsas();
            throw new IllegalStateException("Unsupported application ISA: " + getIsaName(appIsa)
                    + ", supported ISAs: " + isaSetToString(supportedIsas));
        }
        String modulePath = StartupInfo.getModulePath();
        String zipEntry = "lib/" + getNativeLibraryDirName(appIsa) + "/libqauxv-core0.so";
        ClassLoader classLoader = getIsolatedSecondaryNativeLibraryNativeLoader(context);
        // the native loader will then patch the "libqauxv-core0.so" to "libqauxv-core1.so" before loading
        sSecondaryNativeLibraryHandle = nativeLoadSecondaryNativeLibrary(modulePath, zipEntry, classLoader, appIsa);
        if (sSecondaryNativeLibraryHandle == 0) {
            throw new AssertionError("nativeLoadSecondaryNativeLibrary returned 0");
        }
        sSecondaryNativeLibraryLoaded = true;
    }

    public static void secondaryNativeLibraryFullInitialize(@NonNull Context context) {
        if (!isSecondaryNativeLibraryNeeded(context)) {
            return;
        }
        if (!sSecondaryNativeLibraryLoaded) {
            throw new IllegalStateException("Secondary native library must be loaded before full initialization");
        }
        if (sSecondaryNativeLibraryInitialized) {
            return;
        }
        int initMode = NATIVE_LIBRARY_INIT_MODE_SECONDARY_ONLY;
        String packageName = HostInfo.getPackageName();
        int currentSdkLevel = Build.VERSION.SDK_INT;
        String versionName = HostInfo.getVersionName();
        long longVersionCode = HostInfo.getLongVersionCode();
        String dataDir = context.getDataDir().getAbsolutePath();
        boolean isDebugBuild = BuildConfig.DEBUG;
        nativeSecondaryNativeLibraryFullInit(initMode, dataDir, packageName, currentSdkLevel, versionName, longVersionCode, isDebugBuild);
        sSecondaryNativeLibraryHandle = sPrimaryNativeLibraryHandle;
        if (sSecondaryNativeLibraryHandle == 0) {
            throw new AssertionError("(sSecondaryNativeLibraryHandle = sPrimaryNativeLibraryHandle) == 0");
        }
        sSecondaryNativeLibraryInitialized = true;
    }

    private static void loadPrimaryNativeLibraryInHost(@NonNull File dataDir, @Nullable ApplicationInfo hostAppInfo) {
        if (sPrimaryNativeLibraryAttached) {
            return;
        }
        int runtimeIsa = getCurrentRuntimeIsa();
        // do we support the current runtime ISA?
        Set<Integer> supportedIsas = getModuleSupportedIsas();
        if (!supportedIsas.contains(runtimeIsa)) {
            throw new IllegalStateException("Unsupported runtime ISA: " + getIsaName(runtimeIsa)
                    + ", supported ISAs: " + isaSetToString(supportedIsas));
        }
        // do we need to use isolated SoLoader?
        boolean useIsolatedSoLoader;
        if (hostAppInfo != null) {
            int appIsa = getApplicationIsa(hostAppInfo);
            useIsolatedSoLoader = appIsa != runtimeIsa;
        } else {
            // we don't know the host app's ABI, so had better use isolated SoLoader
            useIsolatedSoLoader = true;
        }
        ClassLoader loader = useIsolatedSoLoader ? getIsolatedPrimaryNativeLibraryNativeLoader() : NativeLoader.class.getClassLoader();
        assert loader != null;
        Method invokeLoad;
        Method invokeAttach;
        try {
            Class<?> invoker = loader.loadClass("io.github.qauxv.isolated.soloader.LoadLibraryInvoker");
            invokeLoad = invoker.getDeclaredMethod("invokeLoadLibrary", String.class);
            invokeAttach = invoker.getDeclaredMethod("invokeAttachClassLoader", ClassLoader.class);
        } catch (ReflectiveOperationException e) {
            // should not happen
            throw IoUtils.unsafeThrowForIteCause(e);
        }
        String apkPath = StartupInfo.getModulePath();
        String soname = "libqauxv-core0.so";
        if (!sPrimaryNativeLibraryLoaded) {
            // case 1: direct load without extracting, since the native library is expected to be 4K aligned without compression
            try {
                String path = apkPath + "!/lib/" + getNativeLibraryDirName(runtimeIsa) + "/" + soname;
                invokeLoad.invoke(null, path);
                if (sPrimaryNativeLibraryNativeLoader == null) {
                    sPrimaryNativeLibraryNativeLoader = loader;
                }
                sPrimaryNativeLibraryLoaded = true;
            } catch (InvocationTargetException e) {
                Throwable cause = e.getTargetException();
                Log.w("Failed to load native library directly", cause);
                if (cause instanceof UnsatisfiedLinkError) {
                    throwIfJniError((UnsatisfiedLinkError) cause);
                }
            } catch (IllegalAccessException e) {
                // should not happen
                throw IoUtils.unsafeThrow(e);
            }
        }
        if (!sPrimaryNativeLibraryLoaded) {
            // case 2: extract and load if direct mmap failed
            File filesDir = new File(dataDir, "files");
            IoUtils.mkdirsOrThrow(filesDir);
            File soFile = extractNativeLibrary(filesDir, soname, getNativeLibraryDirName(runtimeIsa));
            try {
                invokeLoad.invoke(null, soFile.getAbsolutePath());
                if (sPrimaryNativeLibraryNativeLoader == null) {
                    sPrimaryNativeLibraryNativeLoader = loader;
                }
                sPrimaryNativeLibraryLoaded = true;
            } catch (InvocationTargetException e) {
                Throwable cause = e.getTargetException();
                Log.w("Failed to load native library after extraction", cause);
                // always treat as fatal error
                throw IoUtils.unsafeThrow(e);
            } catch (IllegalAccessException e) {
                // should not happen
                throw IoUtils.unsafeThrow(e);
            }
        }
        // attach the class loader
        ClassLoader self = NativeLoader.class.getClassLoader();
        assert self != null;
        try {
            invokeAttach.invoke(null, self);
            sPrimaryNativeLibraryAttached = true;
            sPrimaryNativeLibraryHandle = nativeGetPrimaryNativeLibraryHandle();
            if (sPrimaryNativeLibraryHandle == 0) {
                throw new AssertionError("nativeGetPrimaryNativeLibraryHandle returned 0");
            }
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            Log.w("Failed to attach primary native library", cause);
            // always treat as fatal error
            throw IoUtils.unsafeThrow(e);
        } catch (IllegalAccessException e) {
            // should not happen
            throw IoUtils.unsafeThrow(e);
        }
        // attach done, most native methods are now available
    }

    private static void throwIfJniError(@NonNull UnsatisfiedLinkError error) {
        if (error.getMessage() != null && error.getMessage().contains("JNI_ERR")) {
            throw error;
        }
    }

    /**
     * Extract or update native library into "qa_dyn_lib" dir
     *
     * @param filesDir directory to store the extracted native library, get from {@link Context#getFilesDir()}
     * @param soname   the name of the native library, e.g. "libqauxv-core0.so"
     * @param abi      the ABI of the native library, e.g. "arm64-v8a"
     */
    private static File extractNativeLibrary(@NonNull File filesDir, String soname, String abi) {
        String soName = soname + "." + BuildConfig.VERSION_CODE + "." + abi;
        File dir = new File(filesDir, "qa_dyn_lib");
        IoUtils.mkdirsOrThrow(dir);
        ClassLoader cl = NativeLoader.class.getClassLoader();
        assert cl != null;
        File soFile = new File(dir, soName);
        if (soFile.isFile() && soFile.canWrite()) {
            // dynamically loaded code should not be writable, or ART may complain about it
            IoUtils.deleteSingleFileOrThrow(soFile);
        }
        if (!soFile.exists()) {
            InputStream in = cl.getResourceAsStream("lib/" + abi + "/" + soname);
            if (in == null) {
                throw new UnsatisfiedLinkError("Unsupported ABI: " + abi);
            }
            // clean up old files
            for (String name : dir.list()) {
                if (name.startsWith(soname)) {
                    new File(dir, name).delete();
                }
            }
            try {
                // extract so file
                soFile.createNewFile();
                FileOutputStream fout = new FileOutputStream(soFile);
                IoUtils.makeFileReadOnly(soFile);
                byte[] buf = new byte[1024];
                int i;
                while ((i = in.read(buf)) > 0) {
                    fout.write(buf, 0, i);
                }
                in.close();
                fout.flush();
                fout.close();
            } catch (IOException ioe) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
                throw IoUtils.unsafeThrow(ioe);
            }
        }
        return soFile;
    }

    // not used
    private static final int NATIVE_LIBRARY_INIT_MODE_NONE = 0;
    // config the native library to function as primary only
    private static final int NATIVE_LIBRARY_INIT_MODE_PRIMARY_ONLY = 1;
    // config the native library to function as secondary only
    private static final int NATIVE_LIBRARY_INIT_MODE_SECONDARY_ONLY = 2;
    // config the native library to function as both primary and secondary
    private static final int NATIVE_LIBRARY_INIT_MODE_BOTH_PRIMARY_AND_SECONDARY = 3;

    private static native void nativePrimaryNativeLibraryPreInit(@NonNull String dataDir, boolean allowHookLinker);

    private static native void nativePrimaryNativeLibraryFullInit(int initMode, @NonNull String dataDir,
            String packageName, int currentSdkLevel, String versionName, long longVersionCode, boolean isDebugBuild);

    private static native long nativeLoadSecondaryNativeLibrary(@NonNull String modulePath, @NonNull String entryPath,
            @NonNull ClassLoader classLoader, int isa);

    private static native void nativeSecondaryNativeLibraryFullInit(int initMode, @NonNull String dataDir,
            String packageName, int currentSdkLevel, String versionName, long longVersionCode, boolean isDebugBuild);

    public static native int getPrimaryNativeLibraryIsa();

    public static native int getSecondaryNativeLibraryIsa();

    private static native long nativeGetPrimaryNativeLibraryHandle();

    public static long getPrimaryNativeLibraryHandle() {
        return sPrimaryNativeLibraryHandle;
    }

    public static long getSecondaryNativeLibraryHandle() {
        return sSecondaryNativeLibraryHandle;
    }

    public static boolean isSecondaryNativeLibraryNeeded(@NonNull Context context) {
        Objects.requireNonNull(context);
        if (sIsSecondaryNativeLibraryNeeded == null) {
            synchronized (NativeLoader.class) {
                if (sIsSecondaryNativeLibraryNeeded == null) {
                    if (StartupInfo.isInHostProcess()) {
                        int runtimeIsa = getCurrentRuntimeIsa();
                        int appIsa = getApplicationIsa(context.getApplicationInfo());
                        sIsSecondaryNativeLibraryNeeded = runtimeIsa != appIsa;
                    } else {
                        sIsSecondaryNativeLibraryNeeded = false;
                    }
                }
            }
        }
        return sIsSecondaryNativeLibraryNeeded;
    }

    // TODO: 2024-08-08 refactor extracting so files logic so that registerNativeLibEntry can be removed
    private static void registerNativeLibEntry(String soTailingName) {
        if (soTailingName == null || soTailingName.isEmpty()) {
            return;
        }
        try {
            ClassLoader apiClassLoader = IHookBridge.class.getClassLoader().getParent();
            if (apiClassLoader != null) {
                apiClassLoader.loadClass(LoaderExtensionHelper.getObfuscatedLsposedNativeApiClassName())
                        .getMethod("recordNativeEntrypoint", String.class)
                        .invoke(null, soTailingName);
            }
        } catch (ClassNotFoundException ignored) {
            // not LSPosed, ignore
        } catch (NoSuchMethodException | IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
            Log.e(e);
        }
    }

    public static void primaryNativeLibraryPreInitialize(@NonNull File dataDir, @Nullable ApplicationInfo hostAppInfo, boolean allowHookLinker) {
        if (!dataDir.canWrite()) {
            throw new IllegalArgumentException("dataDir not writable: " + dataDir);
        }
        if (!sPrimaryNativeLibraryAttached) {
            loadPrimaryNativeLibrary(dataDir, hostAppInfo);
        }
        if (!sPrimaryNativeLibraryPreInitialized) {
            // check whether the primary native library ISA matches the current runtime ISA
            int primaryIsa = getPrimaryNativeLibraryIsa();
            int runtimeIsa = getCurrentRuntimeIsa();
            if (primaryIsa != runtimeIsa) {
                // Stop here. Any further operation will cause the ART crash.
                throw new IllegalStateException("Primary native library ISA mismatch: runtime ISA=" + getIsaName(runtimeIsa)
                        + ", primary ISA=" + getIsaName(primaryIsa));
            }
            nativePrimaryNativeLibraryPreInit(dataDir.getAbsolutePath(), allowHookLinker);
            sPrimaryNativeLibraryPreInitialized = true;
        }
    }

    public static void primaryNativeLibraryFullInitialize(@NonNull Context context) {
        Objects.requireNonNull(context);
        if (sPrimaryNativeLibraryFullInitialized) {
            return;
        }
        if (!sPrimaryNativeLibraryPreInitialized) {
            throw new IllegalStateException("Primary native library must be pre-initialized before full initialization");
        }
        String packageName = HostInfo.getPackageName();
        int currentSdkLevel = Build.VERSION.SDK_INT;
        String versionName = HostInfo.getVersionName();
        long longVersionCode = HostInfo.getLongVersionCode();
        String dataDir = context.getDataDir().getAbsolutePath();
        int initMode;
        boolean isSecondaryNeeded;
        if (StartupInfo.isInHostProcess()) {
            isSecondaryNeeded = isSecondaryNativeLibraryNeeded(context);
            initMode = isSecondaryNeeded ? NATIVE_LIBRARY_INIT_MODE_PRIMARY_ONLY
                    : NATIVE_LIBRARY_INIT_MODE_BOTH_PRIMARY_AND_SECONDARY;
        } else {
            // own app_process, keep it simple
            initMode = NATIVE_LIBRARY_INIT_MODE_PRIMARY_ONLY;
            isSecondaryNeeded = false;
        }
        boolean isDebugBuild = BuildConfig.DEBUG;
        nativePrimaryNativeLibraryFullInit(initMode, dataDir, packageName, currentSdkLevel, versionName, longVersionCode, isDebugBuild);
        NativeCoreBridge.initializeMmkvForPrimaryNativeLibrary(context);
        sPrimaryNativeLibraryFullInitialized = true;
        if (!isSecondaryNeeded && initMode == NATIVE_LIBRARY_INIT_MODE_BOTH_PRIMARY_AND_SECONDARY) {
            // mark secondary native library as initialized, because the primary and secondary native libraries are the same
            sSecondaryNativeLibraryLoaded = true;
            sSecondaryNativeLibraryInitialized = true;
        }
    }

    private static String isaSetToString(@Nullable Set<Integer> isas) {
        if (isas == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (int isa : isas) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(getIsaName(isa));
        }
        sb.append("]");
        return sb.toString();
    }

    // ---------- diagnostic ----------

    private static void throwIfLinkerComplainAboutIsaMismatch(@NonNull Throwable throwable) {
        Throwable cause = IoUtils.getIteCauseOrSelf(throwable);
        if (!(cause instanceof UnsatisfiedLinkError)) {
            return;
        }
        UnsatisfiedLinkError e = (UnsatisfiedLinkError) cause;
        String msg = e.getMessage();
        if (msg == null) {
            return;
        }
        // Android 10-14
        String s1 = " is for ";
        String s2 = " instead of ";
        String s3 = "EM_";
        if (msg.contains(s1) && msg.contains(s2) && msg.contains(s3)) {
            throw e;
        }
        // Android 5-9
        String s4 = " has unexpected e_machine: ";
        if (msg.contains(s4)) {
            throw e;
        }
    }

    @SuppressWarnings("deprecation")
    public static void dumpDeviceAbiInfoToLog() {
        // give enough information to help debug
        // Is this CPU_ABI bad?
        Log.e("Build.SDK_INT=" + Build.VERSION.SDK_INT);
        Log.e("Build.CPU_ABI is: " + Build.CPU_ABI);
        Log.e("Build.CPU_ABI2 is: " + Build.CPU_ABI2);
        Log.e("Build.SUPPORTED_ABIS is: " + Arrays.toString(Build.SUPPORTED_ABIS));
        Log.e("Build.SUPPORTED_32_BIT_ABIS is: " + Arrays.toString(Build.SUPPORTED_32_BIT_ABIS));
        Log.e("Build.SUPPORTED_64_BIT_ABIS is: " + Arrays.toString(Build.SUPPORTED_64_BIT_ABIS));
        // check whether this is a 64-bit ART runtime
        Log.e("Process.is64bit is: " + Process.is64Bit());
        StructUtsname uts = Os.uname();
        Log.e("uts.machine is: " + uts.machine);
        Log.e("uts.version is: " + uts.version);
        Log.e("uts.sysname is: " + uts.sysname);
    }

}
