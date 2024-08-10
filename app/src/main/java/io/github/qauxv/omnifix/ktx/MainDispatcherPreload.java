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

package io.github.qauxv.omnifix.ktx;

import android.util.Base64;
import io.github.qauxv.util.IoUtils;
import io.github.qauxv.util.Log;
import java.lang.reflect.Field;

public class MainDispatcherPreload {

    private MainDispatcherPreload() {
        throw new AssertionError("No instance for you!");
    }

    // A device running PixelOS Android 14 has "kotlinx.coroutines.fast.service.loader" set to false.
    // I don't know why. Maybe it is set by the system or something else. I didn't investigate where it comes from.
    // It causes a crash on QQ 9.0.60.17095.
    // This is a workaround to preload the MainDispatcherLoader to avoid the crash.

    public static void preload() {
        try {
            Class<?> klass = Class.forName(getMainDispatcherLoaderClassName(), false, MainDispatcherPreload.class.getClassLoader());
            Field dispatcherField = klass.getDeclaredField("dispatcher");
            dispatcherField.setAccessible(true);
            // deliberately get the reflection object before we check whether manual invention is needed
            // so that if anything goes wrong we can find it as soon as possible
            String oldValue = System.getProperty("kotlinx.coroutines.fast.service.loader");
            if (oldValue != null) {
                Log.w("'kotlinx.coroutines.fast.service.loader' is already set to " + oldValue + ", this is unexpected");
            }
            boolean useFastLoader = Boolean.parseBoolean(System.getProperty("kotlinx.coroutines.fast.service.loader", "true"));
            if (!useFastLoader) {
                System.setProperty("kotlinx.coroutines.fast.service.loader", "true");
                try {
                    // do preload
                    Object dispatcher = dispatcherField.get(null);
                    Log.d("preload MainDispatcherLoader: " + dispatcher);
                } finally {
                    // set the system property back
                    if (oldValue == null) {
                        System.clearProperty("kotlinx.coroutines.fast.service.loader");
                    } else {
                        System.setProperty("kotlinx.coroutines.fast.service.loader", oldValue);
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            throw IoUtils.unsafeThrowForIteCause(e);
        }
    }

    private static String getMainDispatcherLoaderClassName() {
        // Disable stupid R8 optimization, we need some obfuscation so that the R8 do its magic.
        // The great R8 supports compile-time reflection, so we need to make it not that obvious.
        // result: kotlinx.coroutines.internal.MainDispatcherLoader
        byte[] d = Base64.decode("a290bGlueC5jb3JvdXRpbmVzLmludGVybmFsLk1haW5EaXNwYXRjaGVyTG9hZGVy", Base64.DEFAULT);
        return new String(d).intern();
    }

    // 08-10 00:15:15.456  5389  5389 E AndroidRuntime: FATAL EXCEPTION: main
    // Process: com.tencent.mobileqq, PID: 5389
    // java.lang.NoSuchMethodError: No direct method <init>(Lo;Ljava/net/URL;)V in class Ln; or its super classes (declaration of 'n' appears in /data/app/??/com.tencent.mobileqq-??/base.apk!classes4.dex)
    //    at o.openConnection(Unknown Source:2)
    //    at java.net.URL.openConnection(URL.java:1006)
    //    at java.util.ServiceLoader$LazyClassPathLookupIterator.parse(ServiceLoader.java:1119)
    //    at java.util.ServiceLoader$LazyClassPathLookupIterator.nextProviderClass(ServiceLoader.java:1167)
    //    at java.util.ServiceLoader$LazyClassPathLookupIterator.hasNextService(ServiceLoader.java:1184)
    //    at java.util.ServiceLoader$LazyClassPathLookupIterator.hasNext(ServiceLoader.java:1238)
    //    at java.util.ServiceLoader$2.hasNext(ServiceLoader.java:1367)
    //    at kotlin.sequences.SequencesKt___SequencesKt.toList(_Sequences.kt:813)
    //    at kotlinx.coroutines.internal.MainDispatcherLoader.loadMainDispatcher(MainDispatchers.kt:31)
    //    at kotlinx.coroutines.internal.MainDispatcherLoader.<clinit>(MainDispatchers.kt:18)
    //    at kotlinx.coroutines.Dispatchers.getMain(Dispatchers.kt:20)
    //    at androidx.lifecycle.LifecycleKt.getCoroutineScope(Lifecycle.kt:329)
    //    at androidx.lifecycle.LifecycleOwnerKt.getLifecycleScope(LifecycleOwner.kt:45)
    //    at io.github.qauxv.fragment.SettingsMainFragment.doOnCreateView(SettingsMainFragment.kt:164)
    //    at io.github.qauxv.fragment.BaseSettingFragment.onCreateView(BaseSettingFragment.java:109)
    //    at androidx.fragment.app.Fragment.performCreateView(Fragment.java:3104)
    //    at androidx.fragment.app.FragmentStateManager.createView(FragmentStateManager.java:524)
    //    at androidx.fragment.app.FragmentStateManager.moveToExpectedState(FragmentStateManager.java:261)
    //    at androidx.fragment.app.FragmentManager.executeOpsTogether(FragmentManager.java:1899)
    //    at androidx.fragment.app.FragmentManager.removeRedundantOperationsAndExecute(FragmentManager.java:1817)
    //    at androidx.fragment.app.FragmentManager.execPendingActions(FragmentManager.java:1760)
    //    at androidx.fragment.app.FragmentManager$5.run(FragmentManager.java:547)
    //    at android.os.Handler.handleCallback(Handler.java:959)
    //    at android.os.Handler.dispatchMessage(Handler.java:100)
    //    at android.os.Looper.loopOnce(Looper.java:232)
    //    at android.os.Looper.loop(Looper.java:317)
    //    at android.app.ActivityThread.main(ActivityThread.java:8592)
    //    at java.lang.reflect.Method.invoke(Native Method)
    //    at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:580)
    //    at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:878

}
