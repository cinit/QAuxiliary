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

package io.github.qauxv.util.libart;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.config.ConfigManager;
import io.github.qauxv.poststartup.StartupInfo;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.IoUtils;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.Natives;
import io.github.qauxv.util.NonUiThread;
import io.github.qauxv.util.SyncUtils;
import io.github.qauxv.util.dexkit.DexDeobfsProvider;
import io.github.qauxv.util.dexkit.DexMethodDescriptor;
import io.github.qauxv.util.dexkit.impl.DexKitDeobfs;
import io.github.qauxv.util.xpcompat.ArrayUtils;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.MethodDataList;

public class OatInlineDeoptManager {

    private static final OatInlineDeoptManager INSTANCE = new OatInlineDeoptManager();

    public static OatInlineDeoptManager getInstance() {
        return INSTANCE;
    }

    private OatInlineDeoptManager() {
    }

    private final ConfigManager mConfig = ConfigManager.getOatInlineDeoptCache();

    private static final String KEY_LAST_HOST_VERSION_CODE = "KEY_LAST_HOST_VERSION_CODE";
    private static final String KEY_LAST_HOST_VERSION_CODE_FOR_PROCESS = "KEY_LAST_HOST_VERSION_CODE_FOR_PROCESS_";
    private static final String KEY_LAST_DEOPT_LIST = "KEY_LAST_DEOPT_LIST";
    private static final String KEY_OAT_INLINE_DEOPT_ENABLED = "KEY_OAT_INLINE_DEOPT_ENABLED";
    // KEY_DISABLE_ART_PROFILE_SAVER is used in native
    private static final String KEY_DISABLE_ART_PROFILE_SAVER = "KEY_DISABLE_ART_PROFILE_SAVER";

    public void clearOatInlineListCache() {
        mConfig.remove(KEY_LAST_DEOPT_LIST);
        mConfig.remove(KEY_LAST_HOST_VERSION_CODE);
        mConfig.apply();
    }

    public boolean isOatInlineDeoptEnabled() {
        return mConfig.getBooleanOrDefault(KEY_OAT_INLINE_DEOPT_ENABLED, false);
    }

    public void setOatInlineDeoptEnabled(boolean enabled) {
        mConfig.putBoolean(KEY_OAT_INLINE_DEOPT_ENABLED, enabled);
    }

    public boolean isDisableArtProfileSaverEnabled() {
        File file = new File(HostInfo.getApplication().getFilesDir(), "qa_misc/" + KEY_DISABLE_ART_PROFILE_SAVER);
        return file.exists();
    }

    public void setDisableArtProfileSaverEnabled(boolean enabled) {
        if (enabled != isDisableArtProfileSaverEnabled()) {
            File file = new File(HostInfo.getApplication().getFilesDir(), "qa_misc/" + KEY_DISABLE_ART_PROFILE_SAVER);
            try {
                if (enabled) {
                    //noinspection ResultOfMethodCallIgnored
                    file.createNewFile();
                } else {
                    IoUtils.deleteSingleFileOrThrowEx(file);
                }
            } catch (IOException e) {
                if (enabled != isDisableArtProfileSaverEnabled()) {
                    throw IoUtils.unsafeThrow(e);
                }
            }
        }
    }

    /**
     * Get the cached deopt list.
     * <p>
     * e.g. ["Lcom/tencent/mobileqq/activity/ChatActivity;->onCreate(Landroid/os/Bundle;)V", "Lcom/tencent/mobileqq/activity/ChatActivity;->onResume()V"]
     *
     * @return the cached deopt list, or an empty array if no cached list.
     */
    @NonNull
    public String[] getCachedDeoptList() {
        long currentVersion = HostInfo.getVersionCode();
        long lastVersion = mConfig.getLong(KEY_LAST_HOST_VERSION_CODE, -1);
        if (currentVersion != lastVersion) {
            mConfig.edit().putLong(KEY_LAST_HOST_VERSION_CODE, currentVersion).apply();
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        String val = mConfig.getString(KEY_LAST_DEOPT_LIST, "");
        if (val.isEmpty()) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        return val.split(",");
    }

    public boolean isDeoptListCacheOutdated() {
        if (!getInstance().isOatInlineDeoptEnabled()) {
            return false;
        }
        long currentVersion = HostInfo.getVersionCode();
        long lastVersion = mConfig.getLong(KEY_LAST_HOST_VERSION_CODE, -1);
        String processName = SyncUtils.getProcessName();
        long lastVersionForProcess = mConfig.getLong(KEY_LAST_HOST_VERSION_CODE_FOR_PROCESS + processName, -1);
        return currentVersion != lastVersion || currentVersion != lastVersionForProcess;
    }

    @NonUiThread
    public static HashSet<String> enumerateCurrentExpectedDeoptimizedMethodDescriptors() {
        SyncUtils.requiresNonUiThread();
        // 1. Enumerate all methods that are hooked
        Set<Member> hookedMethods = StartupInfo.requireHookBridge().getHookedMethods();
        HashSet<String> hookedDescriptors = new HashSet<>(hookedMethods.size());
        for (Member member : hookedMethods) {
            hookedDescriptors.add(DexMethodDescriptor.forReflectedMethod(member).getDescriptor());
        }
        HashSet<String> deoptSet = new HashSet<>();
        // 2. DexKit: find caller methods
        try (DexKitDeobfs backend = DexDeobfsProvider.INSTANCE.getCurrentBackend()) {
            DexKitBridge bridge = backend.getDexKitBridge();
            for (String descriptor : hookedDescriptors) {
                MethodData md = bridge.getMethodData(descriptor);
                if (md != null) {
                    MethodDataList callers = md.getCallers();
                    for (int i = 0; i < callers.size(); i++) {
                        deoptSet.add(callers.get(i).getDescriptor());
                    }
                }
            }
        }
        return deoptSet;
    }

    @NonUiThread
    public void updateDeoptListForCurrentProcess() {
        if (!isOatInlineDeoptEnabled()) {
            return;
        }
        long start = System.nanoTime();
        HashSet<String> deoptSet = enumerateCurrentExpectedDeoptimizedMethodDescriptors();
        long end = System.nanoTime();
        Log.d("OatInlineDeoptManager enumerateCurrentExpectedDeoptimizedMethodDescriptors took " + (end - start) / 1000000 + "ms");
        // merge with the old list
        List<String> old = Arrays.asList(getCachedDeoptList());
        // Log.d("OatInlineDeoptManager old size: " + old.size() + ", new size: " + deoptSet.size());
        deoptSet.addAll(old);
        // Log.d("OatInlineDeoptManager merged size: " + deoptSet.size());
        long currentVersion = HostInfo.getVersionCode();
        String processName = SyncUtils.getProcessName();
        mConfig.putLong(KEY_LAST_HOST_VERSION_CODE, currentVersion);
        mConfig.putLong(KEY_LAST_HOST_VERSION_CODE_FOR_PROCESS + processName, currentVersion);
        mConfig.putString(KEY_LAST_DEOPT_LIST, String.join(",", deoptSet));
        mConfig.apply();
    }

    public static void performOatDeoptimizationForCache() {
        if (!getInstance().isOatInlineDeoptEnabled()) {
            return;
        }
        if (!StartupInfo.requireHookBridge().isDeoptimizationSupported()) {
            return;
        }
        try {
            long start = System.nanoTime();
            ClassLoader cl = Initiator.getHostClassLoader();
            OatInlineDeoptManager manager = OatInlineDeoptManager.getInstance();
            String[] deoptList = manager.getCachedDeoptList();
            for (String descriptor : deoptList) {
                if (descriptor.contains("-><clinit>()V")) {
                    continue;
                }
                Member member = getReflectedMethodFromDescriptorOrNull(cl, descriptor);
                if (member != null) {
                    //Log.d("OatInlineDeoptManager.performOatDeoptimizationForCache deopt: " + descriptor);
                    if (((Modifier.ABSTRACT | Modifier.NATIVE) & member.getModifiers()) == 0) {
                        StartupInfo.requireHookBridge().deoptimize(member);
                    }
                } else {
                    Log.d("OatInlineDeoptManager.performOatDeoptimizationForCache member not found: " + descriptor);
                }
            }
            long end = System.nanoTime();
            Log.d("OatInlineDeoptManager.performOatDeoptimizationForCache count: " + deoptList.length + ", took " + (end - start) / 1000000 + "ms");
        } catch (LinkageError | RuntimeException e) {
            Log.e(e);
        }
    }

    @Nullable
    private static Member getReflectedMethodFromDescriptorOrNull(@NonNull ClassLoader cl, @NonNull String descriptor) {
        DexMethodDescriptor dexMethodDescriptor = new DexMethodDescriptor(descriptor);
        // fast path
        try {
            Class<?> klass = cl.loadClass(dexMethodDescriptor.getDeclaringClassName());
            if (dexMethodDescriptor.name.equals("<init>")) {
                try {
                    return Natives.getReflectedMethod(klass, dexMethodDescriptor.name, dexMethodDescriptor.signature, false);
                } catch (NoSuchMethodError ignored) {
                }
            }
            if (dexMethodDescriptor.name.equals("<clinit>")) {
                // ignore for now
                return null;
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
        try {
            return dexMethodDescriptor.getMethodInstance(cl);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

}
