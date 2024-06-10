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

package cc.ioctl.hook.ui.main;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.Reflex;
import io.github.qauxv.base.ISwitchCellAgent;
import io.github.qauxv.base.IUiItemAgent;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.config.ConfigManager;
import io.github.qauxv.core.HookInstaller;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.BaseFunctionHook;
import io.github.qauxv.util.HostInfo;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.IoUtils;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.QQVersion;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;
import kotlinx.coroutines.flow.MutableStateFlow;

@FunctionHookEntry
@UiItemAgentEntry
public class ContactListSortHook extends BaseFunctionHook implements IUiItemAgent {

    public static final ContactListSortHook INSTANCE = new ContactListSortHook();

    private static final String KEY_IGNORE_VIP_STATUS = "ContactListSortHook.ignore_vip_status";
    private static final String KEY_IGNORE_ONLINE_STATUS = "ContactListSortHook.ignore_online_status";

    private ContactListSortHook() {
    }

    private Field fFriendHolder_friends = null;
    private Field fFriendHolder_cachedResult = null;
    private Field fFriends_detalStatusFlag = null;
    private Field fFriends_iTermType = null;
    private Field fFriends_lastLoginType = null;
    private Method mFriends_getFriendNick = null;
    private Field fBuddyListItem_entry = null;

    @Override
    protected boolean initOnce() throws ReflectiveOperationException {
        Class<?> kFriends = Initiator.loadClass("com.tencent.mobileqq.data.Friends");
        fFriends_detalStatusFlag = Reflex.findField(kFriends, byte.class, "detalStatusFlag");
        fFriends_iTermType = Reflex.findField(kFriends, int.class, "iTermType");
        fFriends_lastLoginType = Reflex.findField(kFriends, long.class, "lastLoginType");
        mFriends_getFriendNick = Reflex.findMethod(kFriends, String.class, "getFriendNick");
        Class<?> kBuddyListItem = Initiator.load("com.tencent.mobileqq.activity.contacts.base.BuddyListItem");
        if (kBuddyListItem == null) {
            kBuddyListItem = Initiator.load("com.tencent.mobileqq.activity.contacts.base.c");
        }
        if (kBuddyListItem == null) {
            kBuddyListItem = Initiator.loadClass("com.tencent.mobileqq.adapter.contacts.BuddyListItem");
        }
        if (HostInfo.requireMinQQVersion(QQVersion.QQ_9_0_20)) {
            kBuddyListItem = Initiator.load("com.tencent.mobileqq.activity.contacts.base.f");
        } else if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_9_10)) {
            kBuddyListItem = Initiator.load("com.tencent.mobileqq.activity.contacts.base.e");
        }
        fBuddyListItem_entry = Reflex.findFirstDeclaredInstanceFieldByType(kBuddyListItem, Initiator.loadClass("com.tencent.mobileqq.persistence.Entity"));
        fBuddyListItem_entry.setAccessible(true);
        // recent version
        Class<?> kBuddyListAdapter = Initiator.loadClass("com.tencent.mobileqq.activity.contacts.friend.BuddyListAdapter");
        Method sort = Reflex.findSingleMethod(kBuddyListAdapter, void.class, false, List.class);
        HookUtils.hookBeforeIfEnabled(this, sort, 51, param -> {
            List<Object> list = (List<Object>) param.args[0];
            if (list == null || list.isEmpty()) {
                return;
            }
            sortContactList(list);
            param.setResult(null);
        });

        var kGroupFragment = Initiator.load("com.tencent.mobileqq.friend.group.GroupFragment");
        if (kGroupFragment == null) return true;
        var fields = kGroupFragment.getDeclaredFields();
        Class<?> kAdapter = null;
        for (Field f : fields) {
            f.setAccessible(true);
            if (BaseExpandableListAdapter.class.isAssignableFrom(f.getType())) {
                kAdapter = f.getType();
                break;
            }
        }
        if (kAdapter == null) return true;
        var mSetData = Reflex.findSingleMethod(kAdapter, void.class, false, ArrayList.class);
        HookUtils.hookBeforeIfEnabled(this, mSetData, 51, param -> {
            List<Object> list = (List<Object>) param.args[0];
            if (list == null || list.isEmpty()) {
                return;
            }
            var newList = new ArrayList<>(list);
            newList.forEach(this::sortNtContactList);
            param.args[0] = newList;
        });
        // for ancient version?
        // Class<?> kContactListAdapter = Initiator.loadClass("com.tencent.mobileqq.troop.createNewTroop.ContactListAdapter");
        // Method onSortContactList = Reflex.findSingleMethod(kContactListAdapter, void.class, false, List.class);
        // HookUtils.hookBeforeIfEnabled(this, onSortContactList, 51, param -> {
        //     List<Object> list = (List<Object>) param.args[0];
        //     if (list == null || list.isEmpty()) {
        //         return;
        //     }
        //     sortContactListLegacy(list);
        //     param.setResult(null);
        // });
        return true;
    }

    private void sortContactList(List<Object> list) {
        list.sort(mFriendHolderComparator);
    }

    private void sortNtContactList(Object categoryInfo) {
        try {
            var list = Reflex.getFirstByType(categoryInfo, ArrayList.class);
            list.sort(mNtFriendHolderComparator);
        } catch (ReflectiveOperationException e) {
            Log.e(e);
        }
    }

    // private void sortContactListLegacy(List<Object> list) throws ReflectiveOperationException {
    //     if (false) {
    //         throw new ReflectiveOperationException();
    //     }
    //     list.sort(mFriendHolderComparatorLegacy);
    // }
    // private final Comparator<Object> mFriendHolderComparatorLegacy = (o1, o2) -> {
    //     throw new UnsupportedOperationException("TODO: check whether this is still used in QQ Lite and QQ HD");
    // };

    private final Comparator<Object> mFriendHolderComparator = (o1, o2) -> {
        try {
            Object f1 = fBuddyListItem_entry.get(o1);
            Object f2 = fBuddyListItem_entry.get(o2);
            String nick1 = (String) mFriends_getFriendNick.invoke(f1);
            String nick2 = (String) mFriends_getFriendNick.invoke(f2);
            // TODO: 2022-06-05 implement online status
            return nick1.compareTo(nick2);
        } catch (ReflectiveOperationException e) {
            IoUtils.unsafeThrow(e);
            // unreachable
            return 0;
        }
    };

    private final Comparator<Object> mNtFriendHolderComparator = (o1, o2) -> {
        String nick1 = getNickFromNTSimpleInfo(o1);
        String nick2 = getNickFromNTSimpleInfo(o2);
        return nick1.compareTo(nick2);
    };

    private String getNickFromNTSimpleInfo(Object nTSimpleInfo) {
        var str = nTSimpleInfo.toString();
        var start = str.indexOf("remark:");
        var end = str.indexOf(", sign:");
        return str.substring(start, end);
    }

    private static int compareFriendImpl(@NonNull String nick1, boolean online1, @NonNull String nick2, boolean online2) {
        // TODO: 2022-06-05 Support online status and sort nick by 拼音
        if (INSTANCE.isIgnoreOnlineStatus()) {
            return nick1.compareTo(nick2);
        } else {
            if (online1 && !online2) {
                return -1;
            } else if (!online1 && online2) {
                return 1;
            } else {
                return nick1.compareTo(nick2);
            }
        }
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Simplify.MAIN_UI_CONTACT;
    }

    @NonNull
    @Override
    public IUiItemAgent getUiItemAgent() {
        return this;
    }

    private void showConfigDialog(@NonNull Activity ctx) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        CheckBox ignoreVipStatus = new CheckBox(ctx);
        ignoreVipStatus.setText("排序忽略 VIP 状态\n（不要把会员放在最前面）");
        ignoreVipStatus.setChecked(isIgnoreVipStatus());
        root.addView(ignoreVipStatus);
        CheckBox ignoreOnlineStatus = new CheckBox(ctx);
        ignoreOnlineStatus.setText("排序忽略在线状态\n（目前还没有实现获取在线状态）");
        ignoreOnlineStatus.setChecked(true);
        ignoreOnlineStatus.setEnabled(false);
        root.addView(ignoreOnlineStatus);
        builder.setView(root)
                .setTitle("联系人排序设置")
                .setPositiveButton("确定", (dialog, which) -> {
                    ConfigManager cfg = ConfigManager.getDefaultConfig();
                    cfg.putBoolean(KEY_IGNORE_VIP_STATUS, ignoreVipStatus.isChecked());
                    cfg.putBoolean(KEY_IGNORE_ONLINE_STATUS, false);
                    cfg.save();
                    if (isEnabled() && !isInitialized()) {
                        HookInstaller.initializeHookForeground(ctx, ContactListSortHook.INSTANCE);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @NonNull
    @Override
    public Function1<IUiItemAgent, String> getTitleProvider() {
        return agent -> "联系人排序";
    }

    @Nullable
    @Override
    public Function2<IUiItemAgent, Context, CharSequence> getSummaryProvider() {
        return null;
    }

    @Nullable
    @Override
    public MutableStateFlow<String> getValueState() {
        return null;
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

    @Nullable
    @Override
    public Function3<IUiItemAgent, Activity, View, Unit> getOnClickListener() {
        return (agent, ctx, view) -> {
            showConfigDialog(ctx);
            return Unit.INSTANCE;
        };
    }

    @Nullable
    @Override
    public Function2<IUiItemAgent, Context, String[]> getExtraSearchKeywordProvider() {
        return null;
    }

    @Override
    public boolean isApplicationRestartRequired() {
        return true;
    }

    public boolean isIgnoreVipStatus() {
        return ConfigManager.getDefaultConfig().getBooleanOrDefault(KEY_IGNORE_VIP_STATUS, false);
    }

    public boolean isIgnoreOnlineStatus() {
        return ConfigManager.getDefaultConfig().getBooleanOrDefault(KEY_IGNORE_ONLINE_STATUS, false);
    }

    @Override
    public boolean isEnabled() {
        return isIgnoreOnlineStatus() || isIgnoreVipStatus();
    }

    @Override
    public void setEnabled(boolean value) {
        // unsupported
    }
}
