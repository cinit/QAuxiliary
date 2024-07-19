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
package cc.ioctl.util;

import static cc.ioctl.util.DateTimeUtil.getRelTimeStrSec;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import cc.ioctl.fragment.ExfriendListFragment;
import cc.ioctl.hook.DeletionObserver;
import cc.ioctl.util.data.EventRecord;
import cc.ioctl.util.data.FriendRecord;
import cc.ioctl.util.data.Table;
import io.github.qauxv.R;
import io.github.qauxv.activity.SettingsUiFragmentHostActivity;
import io.github.qauxv.bridge.AppRuntimeHelper;
import io.github.qauxv.bridge.FriendChunk;
import io.github.qauxv.bridge.ManagerHelper;
import io.github.qauxv.config.ConfigManager;
import io.github.qauxv.lifecycle.ActProxyMgr;
import io.github.qauxv.lifecycle.Parasitics;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.SyncUtils;
import io.github.qauxv.util.Toasts;
import io.github.qauxv.util.data.ContactDescriptor;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import kotlin.collections.ArraysKt;

public class ExfriendManager {

    public static final int ID_EX_NOTIFY = 65537;
    public static final int CHANGED_UNSPECIFIED = 0;
    public static final int CHANGED_GENERAL_SETTING = 16;
    public static final int CHANGED_PERSONS = 17;
    public static final int CHANGED_EX_EVENTS = 18;
    public static final int CHANGED_EVERYTHING = 64;
    private static final String KET_LEGACY_FRIENDS = "friends";
    private static final String KET_FRIENDS = "friends_impl";
    private static final String KET_LEGACY_EVENTS = "events";
    private static final String KET_EVENTS = "events_impl";
    private static final int FL_UPDATE_INT_MIN = 10 * 60;//sec
    private static final HashMap<Long, ExfriendManager> instances = new HashMap<>();
    private static ExecutorService tp;
    private volatile long lastUpdateTimeSec;
    private long mUin;
    private ConcurrentHashMap<Long, FriendRecord> persons;
    private ConcurrentHashMap<Integer, EventRecord> events;
    private ConfigManager mConfig;
    private ConcurrentHashMap mStdRemarks;
    private ArrayList<FriendChunk> cachedFriendChunks;
    private boolean dirtySerializedFlag = true;

    private static final long[] ROBOT_ENTERPRISE_UIN_ARRAY = new long[]{
            // 查询ROBOT信息 https://qun.qq.com/qunpro/robot/qunshare?robot_uin=66600000
            66600000L, // babyQ
            2854196925L, // QQ小店助手
            2854202683L, // 游戏助手
            2854204259L, // 赞噢机器人
            2854209338L, // 频道管理助手
            2854211892L, // 饭团团
            // 热门
            3889031420L, // 庆余年 | 庆帝
            3889019833L, // 鹅探长
            2854208500L, // 修仙之路
            2854202509L, // 饲养小猫
            2854197266L, // AL_1S
            2854214035L, // 心情复杂
            2854196310L, // Q群管家
            2854203763L, // 小YOYO
            // 游戏
            3889011373L, // 武林侠影
            3889017942L, // 快说喜欢我
            2854211478L, // 小念同学
            2854198976L, // 小德娱乐菌
            3889009909L, // 钓鱼达人
            2854196306L, // 小冰
            3889001741L, // 小小
            // 娱乐
            2854197548L, // 开心农场
            3889000472L, // 麦麦子MaiBot
            3889001044L, // 益智扫雷
            2854207033L, // 房东人生
            2854202692L, // 元梦甜橙喵
            // 工具
            2854203783L, // 阿罗娜小助手
            2854213832L, // 小虾米
            3889001607L, // 海兰德小助手
            3889000871L, // 战地1小电视
            2854196311L, // 王者荣耀小狐狸
            2854207085L, // DNF手游-赛丽亚
            2854196316L, // 和平精英-小几
            2854212997L, // 机器人66
            2854203945L, // 暗区突围老皮
    };

    private ExfriendManager(long uin) {
        persons = new ConcurrentHashMap<>();
        events = new ConcurrentHashMap<>();
        dirtySerializedFlag = true;
        if (tp == null) {
            int pt = SyncUtils.getProcessType();
            if (pt != 0 && (pt & (SyncUtils.PROC_MAIN | SyncUtils.PROC_MSF)) != 0) {
                tp = Executors.newCachedThreadPool();
            }
        }
        initForUin(uin);
    }

    public static ExfriendManager getCurrent() {
        return get(AppRuntimeHelper.getLongAccountUin());
    }

    public static ExfriendManager get(long uin) {
        if (uin < 10000) {
            throw new IllegalArgumentException("uin must >= 10000 ");
        }
        synchronized (instances) {
            ExfriendManager ret = instances.get(uin);
            if (ret != null) {
                return ret;
            }
            ret = new ExfriendManager(uin);
            instances.put(uin, ret);
            return ret;
        }
    }

    public static ExfriendManager getOrNull(long uin) {
        if (uin < 10000) {
            throw new IllegalArgumentException("uin must >= 10000 ");
        }
        synchronized (instances) {
            return instances.get(uin);
        }
    }

    public static Object getFriendsManager() throws Exception {
        Object qqAppInterface = AppRuntimeHelper.getAppRuntime();
        return Reflex.invokeVirtual(qqAppInterface, "getManager", 50, int.class);
    }

    public static ConcurrentHashMap getFriendsConcurrentHashMap(Object friendsManager)
            throws IllegalAccessException, NoSuchFieldException {
        for (Field field : Initiator.load("com.tencent.mobileqq.app.FriendsManager").getDeclaredFields()) {
            if (ConcurrentHashMap.class == field.getType()) {
                field.setAccessible(true);
                ConcurrentHashMap concurrentHashMap = (ConcurrentHashMap) field.get(friendsManager);
                if (concurrentHashMap != null && concurrentHashMap.size() > 0) {
                    if (concurrentHashMap.get(concurrentHashMap.keySet().toArray()[0]).getClass()
                            == Initiator.load("com.tencent.mobileqq.data.Friends")) {
                        return concurrentHashMap;
                    }
                }
            }
        }
        throw new NoSuchFieldException();
    }

    public static void onGetFriendListResp(FriendChunk fc) {
        get(fc.uin).recordFriendChunk(fc);
    }

    public long getUin() {
        return mUin;
    }

    public void reinit() {
        persons = new ConcurrentHashMap<>();
        events = new ConcurrentHashMap<>();
        dirtySerializedFlag = true;
        initForUin(mUin);
    }

    /**
     * Do not use this method for uin-isolated config anymore.<br/>
     * <p>
     * Use {@link ConfigManager#forCurrentAccount()} or {@link ConfigManager#forAccount(long)} directly instead.
     *
     * @return See {@link ConfigManager#forAccount(long)}
     * @deprecated use {@link ConfigManager#forCurrentAccount()} instead
     */
    @NonNull
    @Deprecated
    public ConfigManager getConfig() {
        return ConfigManager.forAccount(mUin);
    }

    private void initForUin(long uin) {
        cachedFriendChunks = new ArrayList<>();
        synchronized (this) {
            mUin = uin;
            try {
                loadAndParseConfigData();
                try {
                    mStdRemarks = getFriendsConcurrentHashMap(getFriendsManager());
                } catch (Throwable e) {
                }
                if (persons.size() == 0 && mStdRemarks != null) {
                    Log.w("WARNING:INIT FROM THE INTERNAL");
                    try {
                        //Here we try to copy friendlist
                        Object fr;
                        Field fuin, fremark, fnick;
                        Class clz_fr = Initiator.load("com/tencent/mobileqq/data/Friends");
                        fuin = clz_fr.getField("uin");//long!!!
                        fuin.setAccessible(true);
                        fremark = clz_fr.getField("remark");
                        fremark.setAccessible(true);
                        fnick = clz_fr.getField("name");
                        fnick.setAccessible(true);
                        for (var entry : (Iterable<Map.Entry>) mStdRemarks.entrySet()) {
                            long t = System.currentTimeMillis() / 1000;
                            fr = entry.getValue();
                            if (fr == null) {
                                continue;
                            }
                            FriendRecord f = new FriendRecord();
                            f.uin = Long.parseLong((String) fuin.get(fr));
                            f.remark = (String) fremark.get(fr);
                            f.nick = (String) fnick.get(fr);
                            f.friendStatus = FriendRecord.STATUS_RESERVED;
                            f.serverTime = t;
                            if (!persons.containsKey(f.uin)) {
                                persons.put(f.uin, f);
                                dirtySerializedFlag = true;
                            }
                        }
                        saveConfigure();
                    } catch (Exception e) {
                        Log.e(e);
                    }
                }
            } catch (Exception e) {
                Log.e(e);
            }
        }
    }

    //TODO: Rename it
    private void loadAndParseConfigData() {
        synchronized (this) {
            try {
                if (mConfig == null) {
                    mConfig = ConfigManager.forAccount(mUin);
                }
                loadFriendsData();
                loadEventsData();
                lastUpdateTimeSec = mConfig.getLong("lastUpdateFl", 0L);
            } catch (Exception e) {
                Log.e(e);
            }
        }
    }

    private void loadFriendsData() {
        // step.1 load table
        Table<Long> fr = null;
        byte[] friendsDat = mConfig.getBytes(KET_FRIENDS);
        if (friendsDat != null) {
            try {
                fr = Table.fromBytes(friendsDat);
            } catch (IOException e) {
                Log.e(e);
            }
        }
        if (fr == null) {
            // try to load from legacy
            fr = (Table<Long>) mConfig.getObject(KET_LEGACY_FRIENDS);
        }
        if (fr == null) {
            Log.e("E/loadFriendsData table is null");
            fr = new Table<>();
        }
        /* uin+"" is key */
        fr.keyName = "uin";
        fr.keyType = Table.TYPE_LONG;
        fr.addField("nick", Table.TYPE_IUTF8);
        fr.addField("remark", Table.TYPE_IUTF8);
        fr.addField("friendStatus", Table.TYPE_INT);
        fr.addField("serverTime", Table.TYPE_LONG);
        // step.2 fill map
        Table<Long> t = fr;
        if (persons == null) {
            persons = new ConcurrentHashMap<>();
        }
        dirtySerializedFlag = true;
        Iterator<Map.Entry<Long, Object[]>> it = t.records.entrySet().iterator();
        Map.Entry<Long, Object[]> entry;
        int _nick, _remark, _fs, _time;
        _nick = t.getFieldId("nick");
        _remark = t.getFieldId("remark");
        _fs = t.getFieldId("friendStatus");
        _time = t.getFieldId("serverTime");
        Object[] rec;
        while (it.hasNext()) {
            entry = it.next();
            FriendRecord f = new FriendRecord();
            f.uin = entry.getKey();
            rec = entry.getValue();
            f.remark = (String) rec[_remark];
            f.nick = (String) rec[_nick];
            f.friendStatus = (Integer) rec[_fs];
            f.serverTime = (Long) rec[_time];
            persons.put(f.uin, f);
            dirtySerializedFlag = true;
        }
    }

    private void saveFriendsData() {
        if (persons == null) {
            return;
        }
        // step.1 create table
        Table<Long> fr = new Table<>();
        /* uin+"" is key */
        fr.keyName = "uin";
        fr.keyType = Table.TYPE_LONG;
        fr.addField("nick", Table.TYPE_IUTF8);
        fr.addField("remark", Table.TYPE_IUTF8);
        fr.addField("friendStatus", Table.TYPE_INT);
        fr.addField("serverTime", Table.TYPE_LONG);
        // step.2 fill table
        Iterator<Map.Entry<Long, FriendRecord>> it = persons.entrySet().iterator();
        Map.Entry<Long, FriendRecord> ent;
        FriendRecord f;
        Long k;
        while (it.hasNext()) {
            ent = it.next();
            f = ent.getValue();
            fr.insert(ent.getKey());
            k = ent.getKey();
            try {
                fr.set(k, "nick", f.nick);
                fr.set(k, "remark", f.remark);
                fr.set(k, "serverTime", f.serverTime);
                fr.set(k, "friendStatus", f.friendStatus);
            } catch (NoSuchFieldException e) {
                Log.e(e);
                //shouldn't happen
            }
        }
        // step.3 write out table
        try {
            mConfig.putBytes(KET_FRIENDS, fr.toBytes());
        } catch (IOException e) {
            Log.e(e);
            //shouldn't happen
        }
    }

    private void loadEventsData() {
        // step.1 load table
        Table<Integer> t = null;
        byte[] eventDat = mConfig.getBytes(KET_EVENTS);
        if (eventDat != null) {
            try {
                t = Table.fromBytes(eventDat);
            } catch (IOException e) {
                Log.e(e);
            }
        }
        if (t == null) {
            // try to load from legacy
            t = (Table<Integer>) mConfig.getObject(KET_LEGACY_EVENTS);
        }
        if (t == null) {
            Log.d("damn! initEvT in null");
            return;
        }
        /* `uin as string` is key */
        t.keyName = "id";
        t.keyType = Table.TYPE_INT;
        t.addField("timeRangeEnd", Table.TYPE_LONG);
        t.addField("timeRangeBegin", Table.TYPE_LONG);
        t.addField("event", Table.TYPE_INT);
        t.addField("operand", Table.TYPE_LONG);
        t.addField("operator", Table.TYPE_LONG);
        t.addField("executor", Table.TYPE_LONG);
        t.addField("before", Table.TYPE_IUTF8);
        t.addField("after", Table.TYPE_IUTF8);
        t.addField("extra", Table.TYPE_IUTF8);
        t.addField("_nick", Table.TYPE_IUTF8);
        t.addField("_remark", Table.TYPE_IUTF8);
        t.addField("_friendStatus", Table.TYPE_INT);
        // step.2 fill map
        Iterator<Map.Entry<Integer, Object[]>> it = t.records.entrySet().iterator();
        Map.Entry<Integer, Object[]> entry;
        int __nick, __remark, __fs, _te, _tb, _ev, _op, _b, _a, _extra, _op_old, _exec;
        __nick = t.getFieldId("_nick");
        __remark = t.getFieldId("_remark");
        __fs = t.getFieldId("_friendStatus");
        _te = t.getFieldId("timeRangeEnd");
        _tb = t.getFieldId("timeRangeBegin");
        _ev = t.getFieldId("event");
        _op = t.getFieldId("operand");
        _exec = t.getFieldId("executor");
        _op_old = t.getFieldId("operator");
        _b = t.getFieldId("before");
        _a = t.getFieldId("after");
        _extra = t.getFieldId("extra");
        Object[] rec;
        long tmp;
        while (it.hasNext()) {
            try {
                entry = it.next();
                EventRecord ev = new EventRecord();
                rec = entry.getValue();
                ev._nick = (String) rec[__nick];
                ev._remark = (String) rec[__remark];
                ev._friendStatus = (Integer) rec[__fs];
                ev.timeRangeBegin = (Long) rec[_tb];
                ev.timeRangeEnd = (Long) rec[_te];
                ev.event = (Integer) rec[_ev];
                if (_op == -1) {
                    // all don't have
                    ev.operand = (Long) rec[_op_old];
                } else {
                    try {
                        tmp = (Long) rec[_op];
                    } catch (NullPointerException e) {
                        tmp = -1;
                    }
                    if (tmp > 9999) {
                        ev.operand = tmp;
                    } else {
                        ev.operand = (Long) rec[_op_old];
                    }
                }
                if (_exec != -1) {
                    try {
                        ev.executor = (Long) rec[_exec];
                    } catch (NullPointerException e) {
                        ev.executor = -1;
                    }
                } else {
                    ev.executor = -1;
                }
                ev.before = (String) rec[_b];
                ev.after = (String) rec[_a];
                ev.extra = (String) rec[_extra];
                events.put(entry.getKey(), ev);
                dirtySerializedFlag = true;
            } catch (Exception e) {
                Log.e(e);
            }
        }
    }

    private void saveEventsData() {
        if (events == null) {
            return;
        }
        // 1. create table
        Table<Integer> t = new Table<>();
        t.keyName = "id";
        t.keyType = Table.TYPE_INT;
        t.addField("timeRangeEnd", Table.TYPE_LONG);
        t.addField("timeRangeBegin", Table.TYPE_LONG);
        t.addField("event", Table.TYPE_INT);
        t.addField("operand", Table.TYPE_LONG);
        t.addField("operator", Table.TYPE_LONG);
        t.addField("executor", Table.TYPE_LONG);
        t.addField("before", Table.TYPE_IUTF8);
        t.addField("after", Table.TYPE_IUTF8);
        t.addField("extra", Table.TYPE_IUTF8);
        t.addField("_nick", Table.TYPE_IUTF8);
        t.addField("_remark", Table.TYPE_IUTF8);
        t.addField("_friendStatus", Table.TYPE_INT);
        // 2. fill table
        Iterator<Map.Entry<Integer, EventRecord>> it =/*(Iterator<Map.Entry<Long, FriendRecord>>)*/events
                .entrySet().iterator();
        Map.Entry<Integer, EventRecord> ent;
        EventRecord ev;
        int k;
        while (it.hasNext()) {
            ent = it.next();
            ev = ent.getValue();
            t.insert(ent.getKey());
            k = ent.getKey();
            try {
                t.set(k, "timeRangeEnd", ev.timeRangeEnd);
                t.set(k, "timeRangeBegin", ev.timeRangeBegin);
                t.set(k, "event", ev.event);
                t.set(k, "operand", ev.operand);
                //fallback
                t.set(k, "operator", ev.operand);
                t.set(k, "executor", ev.executor);
                t.set(k, "before", ev.before);
                t.set(k, "after", ev.after);
                t.set(k, "extra", ev.extra);
                t.set(k, "_nick", ev._nick);
                t.set(k, "_remark", ev._remark);
                t.set(k, "_friendStatus", ev._friendStatus);
            } catch (Exception e) {
                Log.e(e);
                //shouldn't happen
            }
        }
        // 3. write out
        try {
            mConfig.putBytes(KET_EVENTS, t.toBytes());
        } catch (IOException e) {
            Log.e(e);
        }
    }

    public void saveConfigure() {
        synchronized (this) {
            if (persons == null) {
                persons = new ConcurrentHashMap<>();
            }
            if (dirtySerializedFlag) {
                saveEventsData();
                saveFriendsData();
                dirtySerializedFlag = false;
            }
            mConfig.putLong("uin", mUin);
            mConfig.save();
        }
    }

    public ArrayList<ContactDescriptor> getFriendsRemark() {
        ArrayList<ContactDescriptor> ret = new ArrayList<>();
        if (persons != null) {
            for (Map.Entry<Long, FriendRecord> f : persons.entrySet()) {
                if (f.getValue().friendStatus == FriendRecord.STATUS_EXFRIEND) {
                    continue;
                }
                ContactDescriptor cd = new ContactDescriptor();
                cd.uinType = 0;
                cd.uin = f.getKey() + "";
                cd.nick = f.getValue().remark;
                if (cd.nick == null) {
                    cd.nick = f.getValue().remark;
                }
                ret.add(cd);
            }
        }
        return ret;
    }

    /**
     * @hide
     */
    //@Deprecated
    public ConcurrentHashMap<Long, FriendRecord> getPersons() {
        dirtySerializedFlag = true;
        return persons;
    }

    /**
     * @hide
     */
    //@Deprecated
    public ConcurrentHashMap<Integer, EventRecord> getEvents() {
        dirtySerializedFlag = true;
        return events;
    }

    /**
     * @method getRemark: return remark if it's a friend,or one's nickname if not
     */
    public String getRemark(long uin) {
        return (String) mStdRemarks.get("" + uin);
    }

    public synchronized void recordFriendChunk(FriendChunk fc) {
        if (fc.getfriendCount == 0) {
            //ignore it
        } else {
            if (fc.startIndex == 0) {
                cachedFriendChunks.clear();
            }
            cachedFriendChunks.add(fc);
            if (fc.friend_count + fc.startIndex == fc.totoal_friend_count) {
                final FriendChunk[] update = new FriendChunk[cachedFriendChunks.size()];
                cachedFriendChunks.toArray(update);
                cachedFriendChunks.clear();
                tp.execute(() -> asyncUpdateFriendListTask(update));
            }
        }
    }

    public void setRedDot() {
        WeakReference redDotRef = DeletionObserver.INSTANCE.redDotRef;
        if (redDotRef == null) {
            return;
        }
        final TextView rd = (TextView) redDotRef.get();
        if (rd == null) {
            Log.i("Red dot missing!");
            return;
        }
        int m = mConfig.getInt("unread", 0);
        final int n = m;
        ((Activity) rd.getContext()).runOnUiThread(() -> {
            if (n < 1) {
                rd.setVisibility(View.INVISIBLE);
            } else {
                rd.setText("" + n);
                rd.setVisibility(View.VISIBLE);
            }
        });
    }

    public void reportEventWithoutSave(EventRecord ev, Object[] out) {
        int k = events.size();
        while (events.containsKey(k)) {
            k++;
        }
        events.put(k, ev);
        dirtySerializedFlag = true;
        if (out == null) {
            return;
        }
        int unread = mConfig.getInt("unread", 0);
        unread++;
        mConfig.putInt("unread", unread);
        String title, ticker, tag, c;
        if (ev._remark != null && ev._remark.length() > 0) {
            tag = ev._remark + "(" + ev.operand + ")";
        } else if (ev._nick != null && ev._nick.length() > 0) {
            tag = ev._nick + "(" + ev.operand + ")";
        } else {
            tag = "" + ev.operand;
        }
        out[0] = unread;
        ticker = "检测到" + unread + "位好友删除了你";
        if (unread > 1) {
            title = "你被" + unread + "位好友删除";
            c = tag + "等" + unread + "位好友";
        } else {
            title = tag;
            c = "在约 " + getRelTimeStrSec(ev.timeRangeBegin) + " 删除了你";
        }
        out[1] = ticker;
        out[2] = title;
        out[3] = c;
    }

    public void clearUnreadFlag() {
        mConfig.putInt("unread", 0);
        try {
            NotificationManager nm = (NotificationManager) HostInfo.getApplication()
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(ID_EX_NOTIFY);
        } catch (Exception e) {
            Log.e(e);
        }
        dirtySerializedFlag = true;
        setRedDot();
        saveConfigure();
    }

    private void asyncUpdateFriendListTask(FriendChunk[] fcs) {
        Object[] ptr = new Object[4];
        synchronized (this) {
            //check integrity
            boolean integrity;
            int tmp = fcs[fcs.length - 1].totoal_friend_count;
            int len = fcs.length;
            if (tmp < 2) {
                return;
            }
            for (int i = 0; i < fcs.length; i++) {
                tmp -= fcs[len - i - 1].friend_count;
            }
            integrity = tmp == 0;
            if (!integrity) {
                Log.i("Inconsistent friendlist chunk data!Aborting!total=" + tmp);
                return;
            }
            HashMap<Long, FriendRecord> del = new HashMap<>(persons);
            FriendRecord fr;
            for (FriendChunk fc : fcs) {
                for (int ii = 0; ii < fc.friend_count; ii++) {
                    fr = del.remove(fc.arrUin[ii]);
                    if (fr != null) {
                        fr.friendStatus = FriendRecord.STATUS_FRIEND_MUTUAL;
                        fr.nick = fc.arrNick[ii];
                        fr.remark = fc.arrRemark[ii];
                        fr.serverTime = fc.serverTime;
                    } else {
                        fr = new FriendRecord();
                        fr.uin = fc.arrUin[ii];
                        fr.friendStatus = FriendRecord.STATUS_FRIEND_MUTUAL;
                        fr.nick = fc.arrNick[ii];
                        fr.remark = fc.arrRemark[ii];
                        fr.serverTime = fc.serverTime;
                        persons.put(fc.arrUin[ii], fr);
                        dirtySerializedFlag = true;
                    }
                }
            }
            Iterator<Map.Entry<Long, FriendRecord>> it = del.entrySet().iterator();
            ptr[0] = 0;//num,ticker,title,content
            while (it.hasNext()) {
                Map.Entry<Long, FriendRecord> ent = it.next();
                fr = ent.getValue();
                if (fr.friendStatus == FriendRecord.STATUS_FRIEND_MUTUAL) {
                    if (shouldIgnoreDeletionEvent(fr.uin)) {
                        // for enterprise robots we don't report because they are not real friends
                        fr.friendStatus = FriendRecord.STATUS_EXFRIEND;
                        continue;
                    }
                    EventRecord ev = new EventRecord();
                    ev._friendStatus = fr.friendStatus;
                    ev._nick = fr.nick;
                    ev._remark = fr.remark;
                    ev.event = EventRecord.EVENT_FRIEND_DELETE;
                    ev.operand = fr.uin;
                    ev.executor = -1;
                    ev.timeRangeBegin = fr.serverTime;
                    ev.timeRangeEnd = fcs[fcs.length - 1].serverTime;
                    reportEventWithoutSave(ev, ptr);
                    fr.friendStatus = FriendRecord.STATUS_EXFRIEND;
                }
            }
        }
        lastUpdateTimeSec = fcs[0].serverTime;
        if (lastUpdateTimeSec == 0) {
            lastUpdateTimeSec = System.currentTimeMillis() / 1000L;
        }
        doNotifyDelFlAndSave(ptr);
    }

    public void markActiveDelete(long uin) {
        synchronized (this) {
            FriendRecord fr = persons.get(uin);
            if (fr == null) {
                Toasts.error(null, "onActDelResp: get(" + uin + ")==null");
                return;
            }
            EventRecord ev = new EventRecord();
            ev._friendStatus = fr.friendStatus;
            ev._nick = fr.nick;
            ev._remark = fr.remark;
            ev.timeRangeBegin = fr.serverTime;
            ev.timeRangeEnd = fr.serverTime = System.currentTimeMillis() / 1000;
            fr.friendStatus = FriendRecord.STATUS_EXFRIEND;
            ev.executor = this.getUin();
            ev.operand = uin;
            ev.event = EventRecord.EVENT_FRIEND_DELETE;
            reportEventWithoutSave(ev, null);
            saveConfigure();
        }
    }

    @SuppressLint({"MissingPermission", "NotificationPermission"})
    public void doNotifyDelFlAndSave(Object[] ptr) {
        dirtySerializedFlag = true;
        mConfig.putLong("lastUpdateFl", lastUpdateTimeSec);
        saveConfigure();
        try {
            if (isNotifyWhenDeleted() && ((int) ptr[0]) > 0) {
                Context app = HostInfo.getApplication();
                Intent inner = SettingsUiFragmentHostActivity
                        .createStartActivityForFragmentIntent(app, ExfriendListFragment.class, null);
                Intent wrapper = new Intent();
                wrapper.setClassName(HostInfo.getApplication().getPackageName(), ActProxyMgr.STUB_DEFAULT_ACTIVITY);
                wrapper.putExtra(ActProxyMgr.ACTIVITY_PROXY_INTENT, inner);
                PendingIntent pi = PendingIntent.getActivity(HostInfo.getApplication(), 0, wrapper, PendingIntent.FLAG_IMMUTABLE);
                NotificationManager nm = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
                Notification n = createNotiComp(nm, (String) ptr[1], (String) ptr[2], (String) ptr[3],
                        new long[]{100, 200, 200, 100}, pi);
                nm.notify(ID_EX_NOTIFY, n);
                setRedDot();
            }
        } catch (Exception e) {
            Log.e(e);
        }
    }

    //TODO: IPC notify
    public boolean isNotifyWhenDeleted() {
        return mConfig.getBoolean("qn_notify_when_del", true);
    }

    public void setNotifyWhenDeleted(boolean z) {
        mConfig.putBoolean("qn_notify_when_del", z);
        saveConfigure();
    }

    public Notification createNotiComp(NotificationManager nm, String ticker, String title,
            String content, long[] vibration, PendingIntent pi) {
        Application app = HostInfo.getApplication();
        //Do not use NotificationCompat, NotificationCompat does NOT support setSmallIcon with Bitmap.
        Notification.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("qn_del_notify", "删好友通知",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setSound(null, null);
            channel.setVibrationPattern(vibration);
            nm.createNotificationChannel(channel);
            builder = new Notification.Builder(app, channel.getId());
        } else {
            builder = new Notification.Builder(app);
        }
        Parasitics.injectModuleResources(app.getResources());
        // We have to createWithBitmap rather than with a ResId, otherwise RemoteServiceException
        builder.setSmallIcon(Icon.createWithBitmap(BitmapFactory.decodeResource(app.getResources(), R.drawable.ic_del_friend_top)));
        builder.setTicker(ticker);
        builder.setContentTitle(title);
        builder.setContentText(content);
        builder.setContentIntent(pi);
        builder.setVibrate(vibration);
        return builder.build();
    }

    public void doRequestFlRefresh() {
        boolean inLogin;
        inLogin = (AppRuntimeHelper.getLongAccountUin() == mUin);
        if (!inLogin) {
            Log.i("doRequestFlRefresh but uin(" + mUin + ") isn't logged in.");
            return;
        }
        try {
            Reflex.invokeVirtualAny(ManagerHelper.getFriendListHandler(), true, true, boolean.class,
                    boolean.class, void.class);
        } catch (Exception e) {
            Log.e(e);
        }
    }

    public long getLastUpdateTimeSec() {
        return lastUpdateTimeSec;
    }

    public void timeToUpdateFl() {
        long t = System.currentTimeMillis() / 1000;
        if (t - lastUpdateTimeSec > FL_UPDATE_INT_MIN) {
            tp.execute(this::doRequestFlRefresh);
        }
    }

    private static final String KEY_DELETION_DETECTION_EXCLUSION_LIST = "exfl_del_exclusion_list";

    public String[] getDeletionDetectionExclusionList() {
        String uinList = mConfig.getString(KEY_DELETION_DETECTION_EXCLUSION_LIST, "");
        if (uinList.isEmpty()) {
            return new String[0];
        }
        return uinList.split(",");
    }

    public void setDeletionDetectionExclusionList(String[] uinList) {
        if (uinList == null || uinList.length == 0) {
            mConfig.remove(KEY_DELETION_DETECTION_EXCLUSION_LIST);
        } else {
            mConfig.putString(KEY_DELETION_DETECTION_EXCLUSION_LIST, TextUtils.join(",", uinList));
        }
        // no need to call saveConfigure() because it's MMKV
    }

    private boolean shouldIgnoreDeletionEvent(long uin) {
        if (ArraysKt.contains(ROBOT_ENTERPRISE_UIN_ARRAY, uin)) {
            return true;
        }
        String[] exclusionList = getDeletionDetectionExclusionList();
        if (exclusionList == null || exclusionList.length == 0) {
            return false;
        }
        return ArraysKt.contains(exclusionList, String.valueOf(uin));
    }
}
