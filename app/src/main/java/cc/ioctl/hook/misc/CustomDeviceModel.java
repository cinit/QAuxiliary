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
package cc.ioctl.hook.misc;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.dialog.RikkaCustomDeviceModelDialog;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.util.xpcompat.XC_MethodReplacement;
import io.github.qauxv.util.xpcompat.XposedBridge;
import io.github.qauxv.util.xpcompat.XposedHelpers;
import io.github.qauxv.base.IUiItemAgent;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonConfigFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.QQVersion;
import io.github.qauxv.util.SyncUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import kotlin.Unit;
import kotlin.jvm.functions.Function3;
import kotlinx.coroutines.flow.MutableStateFlow;

//自定义机型
@FunctionHookEntry
@UiItemAgentEntry
public class CustomDeviceModel extends CommonConfigFunctionHook {

    public static final CustomDeviceModel INSTANCE = new CustomDeviceModel();

    private CustomDeviceModel() {
        super(SyncUtils.PROC_ANY);
    }

    @NonNull
    @Override
    public String getName() {
        return "自定义机型";
    }

    @Nullable
    @Override
    public MutableStateFlow<String> getValueState() {
        return null;
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.MISC_CATEGORY;
    }

    @NonNull
    @Override
    public Function3<IUiItemAgent, Activity, View, Unit> getOnUiItemClickListener() {
        return (agent, activity, view) -> {
            RikkaCustomDeviceModelDialog dialog = new RikkaCustomDeviceModelDialog();
            dialog.showDialog(activity);
            return Unit.INSTANCE;
        };
    }

    @Override
    public boolean initOnce() throws Exception {
        Class<?> Clz = Initiator.load("android.os.Build");
        Field manufacturerField = XposedHelpers.findField(Clz, "MANUFACTURER");
        Field modelField = XposedHelpers.findField(Clz, "MODEL");
        manufacturerField.setAccessible(true);
        modelField.setAccessible(true);
        final String targetManufacturer = RikkaCustomDeviceModelDialog.getCurrentDeviceManufacturer();
        final String targetModel = RikkaCustomDeviceModelDialog.getCurrentDeviceModel();
        if (TextUtils.isEmpty(targetModel) || TextUtils.isEmpty(targetManufacturer)) {
            // enabled but null?
            return false;
        }
        manufacturerField.set(Clz.newInstance(), targetManufacturer);
        modelField.set(Clz.newInstance(), targetModel);
        //hook 替换QQ获取缓存里的设备信息
        Class<?> devInfoManager = Initiator.load("com.tencent.mobileqq.Pandora.deviceInfo.DeviceInfoManager");
        if (devInfoManager == null) {
            devInfoManager = Initiator.load("com.tencent.mobileqq.pandora.deviceinfo.DeviceInfoManager");
        }
        if (devInfoManager != null) {
            Method getMODEL = XposedHelpers.findMethodExactIfExists(devInfoManager, "getModel", Context.class);
            if (getMODEL == null) {
                getMODEL = XposedHelpers.findMethodExactIfExists(devInfoManager, "h", Context.class);
            }
            if (getMODEL != null) {
                XposedBridge.hookMethod(getMODEL, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) {
                        return targetModel;
                    }
                });
            }
        }

        // 适配新DeviceInfoManager
        Class<?> devInfoMonitorClazz = Initiator.load("com.tencent.qmethod.pandoraex.monitor.DeviceInfoMonitor");
        if(devInfoMonitorClazz != null) {
            Method getModelMethod = XposedHelpers.findMethodExactIfExists(devInfoMonitorClazz, "getModel");
            if(getModelMethod != null) {
                XposedBridge.hookMethod(getModelMethod, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) {
                        return targetModel;
                    }
                });
            }
        }

        return true;
    }

    @Override
    public boolean isApplicationRestartRequired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return RikkaCustomDeviceModelDialog.IsEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        // not supported.
    }

    @Override
    public boolean isAvailable() {
        return HostInfo.requireMinQQVersion(QQVersion.QQ_8_8_98);
    }
}
