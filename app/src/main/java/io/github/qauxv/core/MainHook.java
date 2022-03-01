/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */
package io.github.qauxv.core;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import cc.ioctl.hook.GagInfoDisclosure;
import cc.ioctl.hook.MuteAtAllAndRedPacket;
import cc.ioctl.hook.MuteQZoneThumbsUp;
import cc.ioctl.hook.RevokeMsgHook;
import cc.ioctl.util.Reflex;
import com.rymmmmm.hook.CustomSplash;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import io.github.qauxv.SyncUtils;
import io.github.qauxv.bridge.AppRuntimeHelper;
import io.github.qauxv.config.ConfigItems;
import io.github.qauxv.lifecycle.JumpActivityEntryHook;
import io.github.qauxv.lifecycle.Parasitics;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.LicenseStatus;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.MainProcess;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import me.kyuubiran.hook.RemoveCameraButton;
import xyz.nextalone.hook.RemoveSuperQQShow;

/*TitleKit:Lcom/tencent/mobileqq/widget/navbar/NavBarCommon*/

@SuppressWarnings("rawtypes")
public class MainHook {

    private static MainHook SELF;

    boolean third_stage_inited = false;

    private MainHook() {
    }

    public static MainHook getInstance() {
        if (SELF == null) {
            SELF = new MainHook();
        }
        return SELF;
    }

    public static void openProfileCard(Context ctx, long uin) {
        try {
            Log.d("class=" + Initiator._AllInOne());
            Parcelable allInOne = (Parcelable) Reflex.newInstance(
                Initiator._AllInOne(), "" + uin, 35,
                String.class, int.class);
            Intent intent = new Intent(ctx, Initiator._FriendProfileCardActivity());
            intent.putExtra("AllInOne", allInOne);
            ctx.startActivity(intent);
        } catch (Exception e) {
            Log.e(e);
        }
    }

    private static void injectLifecycleForProcess(Context ctx) {
        if (SyncUtils.isMainProcess()) {
            Parasitics.injectModuleResources(ctx.getApplicationContext().getResources());
        }
        if (SyncUtils.isTargetProcess(SyncUtils.PROC_MAIN | SyncUtils.PROC_PEAK)) {
            Parasitics.initForStubActivity(ctx);
        }
    }

    public void performHook(Context ctx, Object step) {
        SyncUtils.initBroadcast(ctx);
        try {
            Class<?> _NewRuntime = Initiator.load("com.tencent.mobileqq.startup.step.NewRuntime");
            Method[] methods = _NewRuntime.getDeclaredMethods();
            Method doStep = null;
            if (methods.length == 1) {
                doStep = methods[0];
            } else {
                for (Method m : methods) {
                    if (Modifier.isProtected(m.getModifiers()) || m.getName().equals("doStep")) {
                        doStep = m;
                        break;
                    }
                }
            }
            XposedBridge.hookMethod(doStep, new XC_MethodHook(52) {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    // fix error in :video, and QZone启动失败
                    AppRuntimeHelper.$access$set$sAppRuntimeInit(true);
                }
            });
        } catch (Throwable e) {
            Log.e("NewRuntime/E hook failed: " + e);
            AppRuntimeHelper.$access$set$sAppRuntimeInit(true);
        }
        injectLifecycleForProcess(ctx);
        HookInstaller.allowEarlyInit(RevokeMsgHook.INSTANCE);
        HookInstaller.allowEarlyInit(MuteQZoneThumbsUp.INSTANCE);
        HookInstaller.allowEarlyInit(MuteAtAllAndRedPacket.INSTANCE);
        HookInstaller.allowEarlyInit(GagInfoDisclosure.INSTANCE);
        HookInstaller.allowEarlyInit(CustomSplash.INSTANCE);
        HookInstaller.allowEarlyInit(RemoveCameraButton.INSTANCE);
        HookInstaller.allowEarlyInit(RemoveSuperQQShow.INSTANCE);
        if (SyncUtils.isMainProcess()) {
            ConfigItems.removePreviousCacheIfNecessary();
            injectStartupHookForMain(ctx);
            Class loadData = Initiator.load("com/tencent/mobileqq/startup/step/LoadData");
            Method doStep = null;
            for (Method method : loadData.getDeclaredMethods()) {
                if (method.getReturnType().equals(boolean.class) && method.getParameterTypes().length == 0) {
                    doStep = method;
                    break;
                }
            }
            XposedBridge.hookMethod(doStep, new XC_MethodHook(51) {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (third_stage_inited) {
                        return;
                    }
                    Class director = Initiator._StartupDirector();
                    Object dir = Reflex.getInstanceObjectOrNull(param.thisObject, "mDirector", director);
                    if (dir == null) {
                        dir = Reflex.getInstanceObjectOrNull(param.thisObject, "a", director);
                    }
                    if (dir == null) {
                        dir = Reflex.getFirstNSFByType(param.thisObject, director);
                    }
                    InjectDelayableHooks.step(dir);
                    third_stage_inited = true;
                }
            });
        } else {
            if (LicenseStatus.hasUserAcceptEula()) {
                Class director = Initiator._StartupDirector();
                Object dir = Reflex.getInstanceObjectOrNull(step, "mDirector", director);
                if (dir == null) {
                    dir = Reflex.getInstanceObjectOrNull(step, "a", director);
                }
                if (dir == null) {
                    dir = Reflex.getFirstNSFByType(step, director);
                }
                InjectDelayableHooks.step(dir);
            }
        }
    }

    @MainProcess
    private void injectStartupHookForMain(Context ctx) {
        JumpActivityEntryHook.initForJumpActivityEntry(ctx);
    }

}
