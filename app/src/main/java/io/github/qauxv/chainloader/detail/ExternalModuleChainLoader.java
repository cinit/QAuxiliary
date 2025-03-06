/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2025 QAuxiliary developers
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

package io.github.qauxv.chainloader.detail;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import androidx.annotation.NonNull;
import cc.ioctl.util.HostInfo;
import dalvik.system.BaseDexClassLoader;
import dalvik.system.PathClassLoader;
import io.github.qauxv.chainloader.detail.ui.ExternalModuleConfigHook;
import io.github.qauxv.util.IoUtils;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ExternalModuleChainLoader {

    private ExternalModuleChainLoader() {
        throw new AssertionError("no instances");
    }

    private static final ArrayList<Throwable> sErrors = new ArrayList<>();
    private static final HashMap<String, BaseDexClassLoader> sClassLoaders = new HashMap<>();

    public static synchronized Throwable[] getErrors() {
        return sErrors.toArray(new Throwable[0]);
    }

    private static String getSha256LowerHex(@NonNull byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw IoUtils.unsafeThrow(e);
        }
    }

    private static void loadModuleFromApkLocked(String packageName, ApplicationInfo ai, ExternalModuleManager.ExternalModuleInfo info)
            throws ReflectiveOperationException, IOException {
        if (sClassLoaders.containsKey(packageName)) {
            return;
        }
        File apkFile = new File(ai.sourceDir);
        if (!apkFile.exists()) {
            throw new IOException("apk not exists: " + apkFile);
        }
        String entryClassName;
        try (ZipFile apk = new ZipFile(apkFile)) {
            ZipEntry propEntry = apk.getEntry("META-INF/qauxv/module.prop");
            if (propEntry == null) {
                throw new IllegalArgumentException("apk " + apkFile + " does not contains META-INF/qauxv/module.prop");
            }
            Properties prop = new Properties();
            prop.load(apk.getInputStream(propEntry));
            entryClassName = prop.getProperty("entry", null);
        }
        if (entryClassName == null) {
            throw new IllegalArgumentException("Module " + apkFile
                    + "!/META-INF/qauxv/module.prop do not contain a 'entry' property");
        }
        ClassLoader parent = ChainLoaderParentClassLoader.INSTANCE;
        PathClassLoader pcl = new PathClassLoader(ai.sourceDir, ai.nativeLibraryDir, parent);
        Class<? extends Runnable> entryRunnableClass = pcl.loadClass(entryClassName).asSubclass(Runnable.class);
        sClassLoaders.put(packageName, pcl);
        // public EntryRunnableV3(@NonNull String modulePath, @Nullable String hostDataDir,
        //                        @Nullable Map<String, Method> xblService);
        try {
            Runnable obj = entryRunnableClass.getConstructor(String.class, String.class, Map.class)
                    .newInstance(ai.sourceDir, HostInfo.getApplication().getDataDir().getAbsolutePath(), null);
            obj.run();
        } catch (InvocationTargetException e) {
            throw IoUtils.unsafeThrowForIteCause(e);
        }
    }

    public static synchronized void loadExternalModules(@NonNull ExternalModuleManager.ExternalModuleInfo[] modules) {
        Context ctx = HostInfo.getApplication();
        PackageManager pms = ctx.getPackageManager();
        for (ExternalModuleManager.ExternalModuleInfo module : modules) {
            try {
                String pkg = module.getPackageName();
                PackageInfo pi = pms.getPackageInfo(pkg, PackageManager.GET_SIGNATURES);
                ApplicationInfo ai = pi.applicationInfo;
                assert ai != null;
                Signature[] sigs = pi.signatures;
                assert sigs != null;
                String certBase64 = getSha256LowerHex(sigs[0].toByteArray());
                if (!module.getCertificateSha256HexLowerChars().equals(certBase64)) {
                    throw new SecurityException("signature mismatch: pkg: " + pi.packageName
                            + ", expected: " + module.getCertificateSha256HexLowerChars() + ", got " + certBase64);
                }
                loadModuleFromApkLocked(pkg, ai, module);
            } catch (PackageManager.NameNotFoundException | RuntimeException |
                     ReflectiveOperationException | IOException | LinkageError e) {
                sErrors.add(e);
                ExternalModuleConfigHook.INSTANCE.traceError(e);
            }
        }
    }

    public static void loadExternalModulesForStartup() {
        loadExternalModules(ExternalModuleManager.INSTANCE.getActiveExternalModules());
    }

}
