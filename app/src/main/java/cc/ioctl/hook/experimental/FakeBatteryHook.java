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
package cc.ioctl.hook.experimental;

import static io.github.qauxv.util.Initiator.load;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Parcelable;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.fragment.FakeBatteryConfigFragment;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.base.IEntityAgent;
import io.github.qauxv.base.RuntimeErrorTracer;
import io.github.qauxv.util.xpcompat.XC_MethodHook;
import io.github.qauxv.util.xpcompat.XposedBridge;
import io.github.qauxv.util.xpcompat.XposedHelpers;
import io.github.qauxv.util.SyncUtils;
import io.github.qauxv.activity.SettingsUiFragmentHostActivity;
import io.github.qauxv.base.ISwitchCellAgent;
import io.github.qauxv.base.IUiItemAgent;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.config.ConfigItems;
import io.github.qauxv.config.ConfigManager;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.BaseFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.dexkit.CDialogUtil;
import io.github.qauxv.util.dexkit.DexKitTarget;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlowKt;

@FunctionHookEntry
@UiItemAgentEntry
public class FakeBatteryHook extends BaseFunctionHook implements InvocationHandler, SyncUtils.BroadcastListener {

    public static final FakeBatteryHook INSTANCE = new FakeBatteryHook();

    private FakeBatteryHook() {
        super(null, false, new DexKitTarget[]{CDialogUtil.INSTANCE});
    }

    private static final String ACTION_UPDATE_BATTERY_STATUS = "io.github.qauxv.ACTION_UPDATE_BATTERY_STATUS";
    private static final String _FLAG_MANUAL_CALL = "flag_manual_call";
    private WeakReference<BroadcastReceiver> mBatteryLevelRecvRef = null;
    private WeakReference<BroadcastReceiver> mBatteryStatusRecvRef = null;
    private Object origRegistrar = null;
    private Object origStatus = null;
    private int lastFakeLevel = -1;
    private int lastFakeStatus = -1;
    private MutableStateFlow<String> mBatteryStateFlow = null;

    private static void doPostReceiveEvent(final BroadcastReceiver recv, final Context ctx, final Intent intent) {
        SyncUtils.post(() -> {
            SyncUtils.setTlsFlag(_FLAG_MANUAL_CALL);
            try {
                recv.onReceive(ctx, intent);
            } catch (Throwable e) {
                FakeBatteryHook.INSTANCE.traceError(e);
            }
            SyncUtils.clearTlsFlag(_FLAG_MANUAL_CALL);
        });
    }

    private static void BatteryProperty_setLong(Parcelable prop, long val) {
        if (prop == null) {
            return;
        }
        try {
            Field field;
            field = prop.getClass().getDeclaredField("mValueLong");
            field.setAccessible(true);
            field.set(prop, val);
        } catch (Throwable e) {
            FakeBatteryHook.INSTANCE.traceError(e);
        }
    }

    @SuppressLint("SoonBlockedPrivateApi")
    @Override
    public boolean initOnce() throws Exception {
        updateSettingsUiState();
        //for :MSF
        Method mGetSendBatteryStatus = null;
        for (Method m : load("com/tencent/mobileqq/msf/sdk/MsfSdkUtils").getMethods()) {
            if (m.getName().equals("getSendBatteryStatus") && m.getReturnType().equals(int.class)) {
                mGetSendBatteryStatus = m;
                break;
            }
        }
        XposedBridge.hookMethod(mGetSendBatteryStatus, new XC_MethodHook(49) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!isEnabled()) {
                    return;
                }
                param.setResult(getFakeBatteryStatus());
            }
        });
        Class<?> cBatteryBroadcastReceiver = load("com.tencent.mobileqq.app.BatteryBroadcastReceiver");
        if (cBatteryBroadcastReceiver != null) {
            XposedHelpers.findAndHookMethod(cBatteryBroadcastReceiver,
                    "onReceive", Context.class, Intent.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (SyncUtils.hasTlsFlag(_FLAG_MANUAL_CALL)) {
                                return;
                            }
                            Intent intent = (Intent) param.args[1];
                            String action = intent.getAction();
                            if (action.equals("android.intent.action.ACTION_POWER_CONNECTED")
                                    || action.equals("android.intent.action.ACTION_POWER_DISCONNECTED")) {
                                if (mBatteryStatusRecvRef == null || mBatteryStatusRecvRef.get() != param.thisObject) {
                                    mBatteryStatusRecvRef = new WeakReference<>((BroadcastReceiver) param.thisObject);
                                }
                            } else if (action.equals("android.intent.action.BATTERY_CHANGED")) {
                                if (mBatteryLevelRecvRef == null || mBatteryLevelRecvRef.get() != param.thisObject) {
                                    mBatteryLevelRecvRef = new WeakReference<>((BroadcastReceiver) param.thisObject);
                                }
                            }
                            if (!isEnabled()) {
                                return;
                            }
                            if (action.equals("android.intent.action.ACTION_POWER_CONNECTED")
                                    || action.equals("android.intent.action.ACTION_POWER_DISCONNECTED")) {
                                if (isFakeBatteryCharging()) {
                                    lastFakeStatus = BatteryManager.BATTERY_STATUS_CHARGING;
                                    intent.setAction("android.intent.action.ACTION_POWER_CONNECTED");
                                } else {
                                    lastFakeStatus = BatteryManager.BATTERY_STATUS_DISCHARGING;
                                    intent.setAction("android.intent.action.ACTION_POWER_DISCONNECTED");
                                }
                            } else if (action.equals("android.intent.action.BATTERY_CHANGED")) {
                                intent.putExtra(BatteryManager.EXTRA_LEVEL, lastFakeLevel = getFakeBatteryCapacity());
                                intent.putExtra(BatteryManager.EXTRA_SCALE, 100);
                                if (isFakeBatteryCharging()) {
                                    intent.putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_CHARGING);
                                    intent.putExtra(BatteryManager.EXTRA_PLUGGED, BatteryManager.BATTERY_PLUGGED_AC);
                                } else {
                                    intent.putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_DISCHARGING);
                                    intent.putExtra(BatteryManager.EXTRA_PLUGGED, 0);
                                }
                            }
                        }
                    });
        }
        // @MainProcess
        // 接下去是UI stuff, 给自己看的
        // 本来还想用反射魔改Binder/ActivityThread$ApplicationThread实现Xposed-less拦截广播onReceive的,太肝了,就不搞了
        BatteryManager batmgr = (BatteryManager) HostInfo.getApplication().getSystemService(Context.BATTERY_SERVICE);
        if (batmgr == null) {
            Log.e("Wtf, init FakeBatteryHook but BatteryManager is null!");
            return false;
        }
        Field fBatteryPropertiesRegistrar = BatteryManager.class.getDeclaredField("mBatteryPropertiesRegistrar");
        fBatteryPropertiesRegistrar.setAccessible(true);
        origRegistrar = fBatteryPropertiesRegistrar.get(batmgr);
        Class<?> cIBatteryPropertiesRegistrar = fBatteryPropertiesRegistrar.getType();
        if (origRegistrar == null) {
            Log.e("Error! mBatteryPropertiesRegistrar(original) got null");
            return false;
        }
        Class<?> cIBatteryStatus = null;
        Field fBatteryStatus = null;
        try {
            fBatteryStatus = BatteryManager.class.getDeclaredField("mBatteryStats");
            fBatteryStatus.setAccessible(true);
            origStatus = fBatteryStatus.get(batmgr);
            cIBatteryStatus = fBatteryStatus.getType();
            if (origStatus == null) {
                Log.e("FakeBatteryHook/W Field mBatteryStats found, but instance got null");
            }
        } catch (NoSuchFieldException e) {
            traceError(e);
            Log.e("FakeBatteryHook/W Field mBatteryStats not found, but SDK_INT is " + Build.VERSION.SDK_INT);
        }
        Object proxy;
        if (origStatus != null && cIBatteryStatus != null) {
            proxy = Proxy.newProxyInstance(Initiator.getPluginClassLoader(),
                new Class[]{cIBatteryPropertiesRegistrar, cIBatteryStatus}, this);
            fBatteryPropertiesRegistrar.set(batmgr, proxy);
            fBatteryStatus.set(batmgr, proxy);
        } else {
            proxy = Proxy.newProxyInstance(Initiator.getPluginClassLoader(),
                new Class[]{cIBatteryPropertiesRegistrar}, this);
            fBatteryPropertiesRegistrar.set(batmgr, proxy);
        }
        SyncUtils.addBroadcastListener(this);
        return true;
    }

    private void scheduleReceiveBatteryLevel() {
        BroadcastReceiver recv;
        if (mBatteryLevelRecvRef == null || (recv = mBatteryLevelRecvRef.get()) == null) {
            if (mBatteryStatusRecvRef == null || (recv = mBatteryStatusRecvRef.get()) == null) {
                return;
            }
        }
        final Intent intent = new Intent("android.intent.action.BATTERY_CHANGED");
        intent.putExtra(BatteryManager.EXTRA_LEVEL, lastFakeLevel = getFakeBatteryCapacity());
        intent.putExtra(BatteryManager.EXTRA_SCALE, 100);
        intent.putExtra(BatteryManager.EXTRA_PRESENT, true);
        intent.putExtra(BatteryManager.EXTRA_TECHNOLOGY, "Li-ion");
        if (isFakeBatteryCharging()) {
            intent.putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_CHARGING);
            intent.putExtra(BatteryManager.EXTRA_PLUGGED, BatteryManager.BATTERY_PLUGGED_AC);
        } else {
            intent.putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_DISCHARGING);
            intent.putExtra(BatteryManager.EXTRA_PLUGGED, 0);
        }
        doPostReceiveEvent(recv, HostInfo.getApplication(), intent);
    }

    private void scheduleReceiveBatteryStatus() {
        BroadcastReceiver recv;
        if (mBatteryStatusRecvRef == null || (recv = mBatteryStatusRecvRef.get()) == null) {
            if (mBatteryLevelRecvRef == null || (recv = mBatteryLevelRecvRef.get()) == null) {
                return;
            }
        }
        String act = isFakeBatteryCharging() ? "android.intent.action.ACTION_POWER_CONNECTED"
            : "android.intent.action.ACTION_POWER_DISCONNECTED";
        final Intent intent = new Intent(act);
        intent.putExtra(BatteryManager.EXTRA_LEVEL, getFakeBatteryCapacity());
        intent.putExtra(BatteryManager.EXTRA_SCALE, 100);
        intent.putExtra(BatteryManager.EXTRA_PRESENT, true);
        intent.putExtra(BatteryManager.EXTRA_TECHNOLOGY, "Li-ion");
        if (isFakeBatteryCharging()) {
            intent.putExtra(BatteryManager.EXTRA_STATUS, lastFakeStatus = BatteryManager.BATTERY_STATUS_CHARGING);
            intent.putExtra(BatteryManager.EXTRA_PLUGGED, BatteryManager.BATTERY_PLUGGED_AC);
        } else {
            intent.putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_DISCHARGING);
            intent.putExtra(BatteryManager.EXTRA_PLUGGED, 0);
        }
        doPostReceiveEvent(recv, HostInfo.getApplication(), intent);
    }

    @Override
    public boolean onReceive(Context context, Intent intent) {
        if (ACTION_UPDATE_BATTERY_STATUS.equals(intent.getAction())) {
            if (isInitialized() && isEnabled()) {
                if (lastFakeLevel != getFakeBatteryCapacity()) {
                    scheduleReceiveBatteryLevel();
                }
                if (lastFakeStatus == -1 ||
                    lastFakeStatus == BatteryManager.BATTERY_STATUS_DISCHARGING == isFakeBatteryCharging()) {
                    scheduleReceiveBatteryStatus();
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            if (isEnabled()) {
                if (method.getName().equals("getProperty") && args.length == 2) {
                    int id = (int) args[0];
                    Parcelable prop = (Parcelable) args[1];
                    if (id == BatteryManager.BATTERY_PROPERTY_STATUS) {
                        if (isFakeBatteryCharging()) {
                            BatteryProperty_setLong(prop, BatteryManager.BATTERY_STATUS_CHARGING);
                        } else {
                            BatteryProperty_setLong(prop, BatteryManager.BATTERY_STATUS_DISCHARGING);
                        }
                        return 0;
                    } else if (id == BatteryManager.BATTERY_PROPERTY_CAPACITY) {
                        BatteryProperty_setLong(prop, getFakeBatteryCapacity());
                        return 0;
                    }
                } else if (method.getName().equals("isCharging") && (args == null || args.length == 0)) {
                    return isFakeBatteryCharging();
                }
            }
        } catch (Exception e) {
            traceError(e);
        }
        try {
            String className = method.getDeclaringClass().getName();
            if (className.endsWith("IBatteryPropertiesRegistrar")) {
                return method.invoke(origRegistrar, args);
            } else if (className.endsWith("IBatteryStats")) {
                return method.invoke(origStatus, args);
            } else if (className.endsWith("Object")) {
                if (method.getName().equals("toString")) {
                    return "a.a.a.a$Stub$Proxy@" + Integer.toHexString(hashCode());
                } else if (method.getName().equals("equals")) {
                    return args[0] == proxy;
                } else if (method.getName().equals("hashCode")) {
                    return hashCode();
                }
                return null;
            } else {
                // WTF QAQ
                Log.e("Panic, unexpected method " + method);
                return null;
            }
        } catch (InvocationTargetException ite) {
            traceError(ite);
            throw ite.getCause();
        }
    }

    public void setFakeSendBatteryStatus(int val) {
        ConfigManager cfg = ConfigManager.getDefaultConfig();
        cfg.putInt(ConfigItems.qn_fake_bat_expr, val);
        cfg.save();
        Intent intent = new Intent(ACTION_UPDATE_BATTERY_STATUS);
        SyncUtils.sendGenericBroadcast(intent);
        updateSettingsUiState();
    }

    public int getFakeBatteryStatus() {
        int val = ConfigManager.getDefaultConfig().getIntOrDefault(ConfigItems.qn_fake_bat_expr, -1);
        return Math.max(val, 0); //safe value
    }

    public boolean isFakeBatteryCharging() {
        return (getFakeBatteryStatus() & 128) > 0;
    }

    public int getFakeBatteryCapacity() {
        return getFakeBatteryStatus() & 127;
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.EXPERIMENTAL_CATEGORY;
    }

    @Override
    public int getTargetProcesses() {
        return SyncUtils.PROC_MAIN | SyncUtils.PROC_MSF;
    }

    public static void onItemClicked(Activity activity) {
        SettingsUiFragmentHostActivity.startFragmentWithContext(activity, FakeBatteryConfigFragment.class, null);
    }

    @NonNull
    private String generateValueString() {
        return isEnabled() ? (getFakeBatteryCapacity() + "%" + (isFakeBatteryCharging() ? "+" : "")) : "禁用";
    }

    private void updateSettingsUiState() {
        String value = generateValueString();
        if (mBatteryStateFlow == null) {
            mBatteryStateFlow = StateFlowKt.MutableStateFlow(value);
        } else {
            mBatteryStateFlow.setValue(value);
        }
    }

    @NonNull
    @Override
    public IUiItemAgent getUiItemAgent() {
        return mUiItemAgent;
    }

    private final IUiItemAgent mUiItemAgent = new IUiItemAgent() {

        @NonNull
        @Override
        public Function1<IEntityAgent, String> getTitleProvider() {
            return (agent) -> "自定义电量";
        }

        @Nullable
        @Override
        public Function2<IEntityAgent, Context, CharSequence> getSummaryProvider() {
            return null;
        }

        @NonNull
        @Override
        public MutableStateFlow<String> getValueState() {
            if (mBatteryStateFlow == null) {
                updateSettingsUiState();
            }
            return mBatteryStateFlow;
        }

        @Nullable
        @Override
        public Function1<IUiItemAgent, Boolean> getValidator() {
            return null;
        }

        @Nullable
        @Override
        public ISwitchCellAgent getSwitchProvider() {
            return null;
        }

        @Override
        public Function3<IUiItemAgent, Activity, View, Unit> getOnClickListener() {
            return (agent, activity, view) -> {
                onItemClicked(activity);
                return Unit.INSTANCE;
            };
        }

        @Nullable
        @Override
        public Function2<IUiItemAgent, Context, String[]> getExtraSearchKeywordProvider() {
            return null;
        }
    };

    @Nullable
    @Override
    public List<RuntimeErrorTracer> getRuntimeErrorDependentComponents() {
        return null;
    }
}
