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
package io.github.qauxv.util;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.PermissionChecker;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.base.IDynamicHook;
import io.github.qauxv.core.HookInstaller;
import io.github.qauxv.util.dexkit.DexDeobfsProvider;
import io.github.qauxv.util.libart.OatInlineDeoptManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@SuppressLint("PrivateApi")
public class SyncUtils {

    // grep ':process=' $INPUT |
    // awk '{gsub(/[ "]/,"");sub(/\/>/,"");sub(/>/,"");sub(/android:process=/,"");print $0}'
    // | sort -n | uniq

    //*MSF -- PROC_MSF
    //:TMAssistantDownloadSDKService
    //:avgame
    //:backtrace__
    //:dataline
    //:dynamic_feature_plugin_oat
    //:gameservice
    //:groupvideo
    //:guild_audio
    //:imageservice
    //:lightsdk
    //:map
    //*mini*?
    //:nearby_video
    //:openSdk
    //* peak  -- PROC_PEAK
    //* picture  (qzone picture)
    //* plugin**  -- PROC_PLUGIN_PROCESS
    //:pushservice
    //:qlink
    //* qqfav
    //* qzone**?  -- PROC_QZONE
    //:readinjoy
    //:rmonitor
    //:smartdevice
    //:standalone
    //:support
    //* tool
    //* troop**
    //* unity*
    //* video
    //:weiyun
    //* wxa_container*
    //:zplan

    public static final int PROC_ERROR = 0;
    public static final int PROC_MAIN = 1;
    public static final int PROC_MSF = 1 << 1;
    public static final int PROC_PEAK = 1 << 2;
    public static final int PROC_TOOL = 1 << 3;
    public static final int PROC_QZONE = 1 << 4;
    public static final int PROC_VIDEO = 1 << 5;
    public static final int PROC_MINI = 1 << 6;
    public static final int PROC_PLUGIN_PROCESS = 1 << 7;
    public static final int PROC_QQFAV = 1 << 8;
    public static final int PROC_TROOP = 1 << 9;
    public static final int PROC_UNITY = 1 << 10;
    public static final int PROC_WXA_CONTAINER = 1 << 11;

    public static final int PROC_OTHERS = 1 << 31;
    public static final int PROC_ANY = 0xFFFFFFFF;
    //file=0
    public static final String SYNC_FILE_CHANGED = "io.github.qauxv.SYNC_FILE_CHANGED";
    //process=010001 hook=0011000
    public static final String HOOK_DO_INIT = "io.github.qauxv.HOOK_DO_INIT";
    public static final String ENUM_PROC_REQ = "io.github.qauxv.ENUM_PROC_REQ";
    public static final String ENUM_PROC_RESP = "io.github.qauxv.ENUM_PROC_RESP";
    public static final String GENERIC_WRAPPER = "io.github.qauxv.GENERIC_WRAPPER";
    public static final String _REAL_INTENT = "__real_intent";
    public static final int FILE_DEFAULT_CONFIG = 1;
    public static final int FILE_CACHE = 2;
    public static final int FILE_UIN_DATA = 3;
    private static final ConcurrentHashMap<Integer, EnumRequestHolder> sEnumProcCallbacks = new ConcurrentHashMap<>();
    private static final Collection<OnFileChangedListener> sFileChangedListeners = Collections.synchronizedCollection(new HashSet<>());
    private static final Collection<BroadcastListener> sBroadcastListeners = Collections.synchronizedCollection(new HashSet<>());
    private static final Map<Long, Collection<String>> sTlsFlags = new HashMap<>();
    private static final Object sTlsLock = new Object();
    private static int myId = 0;
    private static boolean inited = false;
    private static int mProcType = 0;
    private static String mProcName = null;
    private static Handler sHandler;
    private static final ExecutorService sExecutor = Executors.newCachedThreadPool();
    private static final String DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION_SUFFIX = ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION";

    private SyncUtils() {
        throw new AssertionError("No instance for you!");
    }

    @Nullable
    public static String getDynamicReceiverNotExportedPermission(@NonNull Context ctx) {
        String currentPackageName = ctx.getPackageName();
        String[] possiblePermissions = new String[]{
                currentPackageName + DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION_SUFFIX,
                "com.tencent.msg.permission.pushnotify",
                "com.tencent.tim.msg.permission.pushnotify",
                "com.tencent.qqlite.msg.permission.pushnotify",
        };
        String permissionForReceiver = null;
        // pick the first available permission
        for (String permission : possiblePermissions) {
            if (PermissionChecker.checkSelfPermission(ctx, permission) == PermissionChecker.PERMISSION_GRANTED) {
                permissionForReceiver = permission;
                break;
            }
        }
        return permissionForReceiver;
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public static void initBroadcast(Context ctx) {
        if (inited) {
            return;
        }
        BroadcastReceiver recv = new IpcReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(SYNC_FILE_CHANGED);
        filter.addAction(HOOK_DO_INIT);
        filter.addAction(ENUM_PROC_REQ);
        filter.addAction(ENUM_PROC_RESP);
        filter.addAction(GENERIC_WRAPPER);
        // Must not use ContextCompat.registerReceiver here.
        // QQ do not have $packageName.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ctx.registerReceiver(recv, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            ctx.registerReceiver(recv, filter, getDynamicReceiverNotExportedPermission(ctx), null);
        }
        inited = true;
    }

    @Deprecated
    public static void sendGenericBroadcast(Intent intent) {
        sendGenericBroadcast(null, intent);
    }

    public static void sendGenericBroadcast(Context ctx, Intent intent) {
        if (ctx == null) {
            ctx = HostInfo.getApplication();
        }
        intent.putExtra(_REAL_INTENT, intent.getAction());
        intent.setAction(GENERIC_WRAPPER);
        intent.setPackage(ctx.getPackageName());
        initId();
        intent.putExtra("id", myId);
        ctx.sendBroadcast(intent);
    }

    /**
     * Send a broadcast meaning a file was changed
     *
     * @param file FILE_*
     * @param uin  0 for a common file
     * @param what 0 for unspecified
     */
    public static void onFileChanged(int file, long uin, int what) {
        Context ctx = HostInfo.getApplication();
        Intent changed = new Intent(SYNC_FILE_CHANGED);
        changed.setPackage(ctx.getPackageName());
        initId();
        changed.putExtra("id", myId);
        changed.putExtra("file", file);
        changed.putExtra("uin", uin);
        changed.putExtra("what", what);
        ctx.sendBroadcast(changed);
    }

    public static void requestInitHook(int hookId, int process) {
        Context ctx = HostInfo.getApplication();
        Intent changed = new Intent(HOOK_DO_INIT);
        changed.setPackage(ctx.getPackageName());
        initId();
        changed.putExtra("process", process);
        changed.putExtra("hook", hookId);
        ctx.sendBroadcast(changed);
    }

    public static int getProcessType() {
        if (mProcType != 0) {
            return mProcType;
        }
        String[] parts = getProcessName().split(":");
        if (parts.length == 1) {
            if ("unknown".equals(parts[0])) {
                return PROC_MAIN;
            } else if ("com.tencent.ilink.ServiceProcess".equals(parts[0])) {
                mProcType = PROC_OTHERS;
            } else {
                mProcType = PROC_MAIN;
            }
        } else {
            String tail = parts[parts.length - 1];
            if ("MSF".equals(tail)) {
                mProcType = PROC_MSF;
            } else if ("peak".equals(tail)) {
                mProcType = PROC_PEAK;
            } else if ("tool".equals(tail)) {
                mProcType = PROC_TOOL;
            } else if (tail.startsWith("qzone")) {
                mProcType = PROC_QZONE;
            } else if ("video".equals(tail)) {
                mProcType = PROC_VIDEO;
            } else if (tail.startsWith("mini")) {
                mProcType = PROC_MINI;
            } else if (tail.startsWith("plugin")) {
                mProcType = PROC_PLUGIN_PROCESS;
            } else if (tail.startsWith("troop")) {
                mProcType = PROC_TROOP;
            } else if (tail.startsWith("unity")) {
                mProcType = PROC_UNITY;
            } else if (tail.startsWith("wxa_container")) {
                mProcType = PROC_WXA_CONTAINER;
            } else if (tail.startsWith("qqfav")) {
                mProcType = PROC_QQFAV;
            } else {
                mProcType = PROC_OTHERS;
            }
        }
        return mProcType;
    }

    public static boolean isMainProcess() {
        return getProcessType() == PROC_MAIN;
    }

    public static boolean isTargetProcess(int target) {
        return (getProcessType() & target) != 0;
    }

    public static String getProcessName() {
        if (mProcName != null) {
            return mProcName;
        }
        String name = "unknown";
        int retry = 0;
        do {
            try {
                List<ActivityManager.RunningAppProcessInfo> runningAppProcesses =
                        ((ActivityManager) HostInfo.getApplication().getSystemService(Context.ACTIVITY_SERVICE))
                                .getRunningAppProcesses();
                if (runningAppProcesses != null) {
                    for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : runningAppProcesses) {
                        if (runningAppProcessInfo != null && runningAppProcessInfo.pid == android.os.Process.myPid()) {
                            mProcName = runningAppProcessInfo.processName;
                            return runningAppProcessInfo.processName;
                        }
                    }
                }
            } catch (Throwable e) {
                Log.e("getProcessName error " + e);
            }
            retry++;
            if (retry >= 3) {
                break;
            }
        } while ("unknown".equals(name));
        return name;
    }

    public static EnumRequestHolder enumerateProc(Context ctx, final int procMask, int timeout, EnumCallback callback) {
        return enumerateProc(ctx, randomInt32Bits(), procMask, timeout, callback);
    }

    public static EnumRequestHolder enumerateProc(Context ctx, final int requestSeq, final int procMask, int timeout, EnumCallback callback) {
        if (callback == null) {
            throw new NullPointerException("callback == null");
        }
        if (ctx == null) {
            throw new NullPointerException("ctx == null");
        }
        Intent changed = new Intent(ENUM_PROC_REQ);
        changed.setPackage(ctx.getPackageName());
        initId();
        changed.putExtra("mask", procMask);
        changed.putExtra("seq", requestSeq);
        EnumRequestHolder holder = new EnumRequestHolder();
        holder.callback = callback;
        holder.seq = requestSeq;
        holder.deadline = System.currentTimeMillis() + timeout;
        holder.mask = procMask;
        sEnumProcCallbacks.put(requestSeq, holder);
        ctx.sendBroadcast(changed);
        if (sHandler == null) {
            sHandler = new Handler(Looper.getMainLooper());
        }
        sHandler.postDelayed(() -> {
            EnumRequestHolder holder1 = sEnumProcCallbacks.remove(requestSeq);
            if (holder1 == null) {
                return;
            }
            holder1.callback.onEnumResult(holder1);
        }, timeout);
        return holder;
    }

    public static void runOnUiThread(@NonNull Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            post(r);
        }
    }

    public static void async(@NonNull Runnable r) {
        sExecutor.execute(r);
    }

    @SuppressLint("LambdaLast")
    public static void postDelayed(@NonNull Runnable r, long ms) {
        if (sHandler == null) {
            sHandler = new Handler(Looper.getMainLooper());
        }
        sHandler.postDelayed(r, ms);
    }

    //kotlin friendly?
    public static void postDelayed(long ms, @NonNull Runnable r) {
        postDelayed(r, ms);
    }

    public static void post(@NonNull Runnable r) {
        postDelayed(r, 0L);
    }

    public static void requiresUiThread() {
        requiresUiThread(null);
    }

    public static void requiresUiThread(@Nullable String msg) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException(msg == null ? "UI thread required" : msg);
        }
    }

    public static void requiresNonUiThread() {
        requiresNonUiThread(null);
    }

    public static void requiresNonUiThread(@Nullable String msg) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException(msg == null ? "non-UI thread required" : msg);
        }
    }

    public static void addOnFileChangedListener(OnFileChangedListener l) {
        if (l != null) {
            sFileChangedListeners.add(l);
        }
    }

    public static boolean removeBroadcastListener(BroadcastListener l) {
        return sBroadcastListeners.remove(l);
    }

    public static void addBroadcastListener(BroadcastListener l) {
        if (l != null) {
            sBroadcastListeners.add(l);
        }
    }

    public static boolean removeOnFileChangedListener(OnFileChangedListener l) {
        return sFileChangedListeners.remove(l);
    }

    public static void onRecvFileChanged(int file, long uin, int what) {
        for (SyncUtils.OnFileChangedListener l : sFileChangedListeners) {
            l.onFileChanged(file, uin, what);
        }
    }

    public static int randomInt32Bits() {
        return new Random().nextInt();
    }

    public static void initId() {
        if (myId == 0) {
            myId = (int) ((Math.random()) * (Integer.MAX_VALUE / 4));
        }
    }

    public static void setTlsFlag(String name) {
        long tid = Thread.currentThread().getId();
        Collection<String> tls;
        synchronized (sTlsLock) {
            tls = sTlsFlags.get(tid);
            if (tls == null) {
                tls = Collections.synchronizedCollection(new HashSet<>());
                sTlsFlags.put(tid, tls);
            }
        }
        tls.add(name);
    }

    public static boolean clearTlsFlag(String name) {
        long tid = Thread.currentThread().getId();
        Collection<String> tls;
        synchronized (sTlsLock) {
            tls = sTlsFlags.get(tid);
            if (tls == null) {
                return false;
            }
        }
        return tls.remove(name);
    }

    public static boolean hasTlsFlag(String name) {
        long tid = Thread.currentThread().getId();
        Collection<String> tls;
        synchronized (sTlsLock) {
            tls = sTlsFlags.get(tid);
            if (tls == null) {
                return false;
            }
        }
        return tls.contains(name);
    }

    public interface EnumCallback {

        void onResponse(EnumRequestHolder holder, ProcessInfo process);

        void onEnumResult(EnumRequestHolder holder);
    }

    public interface OnFileChangedListener {

        boolean onFileChanged(int type, long uin, int what);
    }

    public interface BroadcastListener {

        boolean onReceive(Context context, Intent intent);
    }

    private static class IpcReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context ctx, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            if (action.equals(GENERIC_WRAPPER) && intent.hasExtra(_REAL_INTENT)) {
                intent.setAction(intent.getStringExtra(_REAL_INTENT));
            }
            boolean done = false;
            for (BroadcastListener bl : sBroadcastListeners) {
                done = done || bl.onReceive(ctx, intent);
            }
            if (done) {
                return;
            }
            switch (action) {
                case SYNC_FILE_CHANGED:
                    int id = intent.getIntExtra("id", -1);
                    int file = intent.getIntExtra("file", 0);
                    long uin = intent.getLongExtra("uin", 0);
                    int what = intent.getIntExtra("file", 0);
                    if (id != -1 && id != myId) {
                        onRecvFileChanged(file, uin, what);
                    }
                    break;
                case HOOK_DO_INIT:
                    int myType = getProcessType();
                    int targetType = intent.getIntExtra("process", 0);
                    int hookId = intent.getIntExtra("hook", -1);
                    if (hookId != -1 && (myType & targetType) != 0) {
                        IDynamicHook hook = HookInstaller.getHookById(hookId);
                        if (hook != null && hook.isTargetProcess() && !hook.isPreparationRequired()) {
                            async(() -> {
                                DexDeobfsProvider.INSTANCE.enterDeobfsSection();
                                try {
                                    hook.initialize();
                                    try {
                                        OatInlineDeoptManager.getInstance().updateDeoptListForCurrentProcess();
                                    } finally {
                                        DexDeobfsProvider.INSTANCE.exitDeobfsSection();
                                    }
                                    OatInlineDeoptManager.performOatDeoptimizationForCache();
                                } catch (Throwable e) {
                                    Log.e(e);
                                }
                            });
                        }
                    }
                    break;
                case ENUM_PROC_REQ:
                    myType = getProcessType();
                    if (!intent.hasExtra("seq")) {
                        break;
                    }
                    int seq = intent.getIntExtra("seq", 0);
                    int mask = intent.getIntExtra("mask", 0);
                    if ((mask & myType) != 0) {
                        Intent resp = new Intent(ENUM_PROC_RESP);
                        resp.setPackage(ctx.getPackageName());
                        initId();
                        resp.putExtra("seq", seq);
                        resp.putExtra("pid", android.os.Process.myPid());
                        resp.putExtra("type", myType);
                        resp.putExtra("time", System.currentTimeMillis());
                        resp.putExtra("name", getProcessName());
                        ctx.sendBroadcast(resp);
                    }
                    break;
                case ENUM_PROC_RESP:
                    if (!intent.hasExtra("seq")) {
                        break;
                    }
                    seq = intent.getIntExtra("seq", 0);
                    EnumRequestHolder holder = sEnumProcCallbacks.get(seq);
                    if (holder == null) {
                        break;
                    }
                    String name = intent.getStringExtra("name");
                    int pid = intent.getIntExtra("pid", 0);
                    int type = intent.getIntExtra("type", 0);
                    long time = intent.getLongExtra("time", -1);
                    ProcessInfo pi = new ProcessInfo();
                    pi.name = name;
                    pi.pid = pid;
                    pi.time = time;
                    pi.type = type;
                    holder.result.add(pi);
                    holder.callback.onResponse(holder, pi);
                    break;
                default:
                    Log.e("Unknown action: " + action);
                    break;
            }
        }
    }

    public static class EnumRequestHolder {

        public EnumCallback callback;
        public ArrayList<ProcessInfo> result = new ArrayList<>();
        public long deadline;
        public int seq;
        public int mask;
    }

    public static class ProcessInfo {

        public int pid;
        public String name;
        public int type;
        public long time;
    }
}
