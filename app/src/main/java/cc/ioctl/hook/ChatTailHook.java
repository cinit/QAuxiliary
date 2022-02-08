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
package cc.ioctl.hook;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.dialog.RikkaCustomMsgTimeFormatDialog;
import cc.ioctl.fragment.ChatTailFragment;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.SyncUtils;
import io.github.qauxv.activity.SettingsUiFragmentHostActivity;
import io.github.qauxv.base.IUiItemAgent;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.config.ConfigItems;
import io.github.qauxv.config.ConfigManager;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonConfigFunctionHook;
import io.github.qauxv.router.decorator.IInputButtonDecorator;
import io.github.qauxv.router.dispacher.InputButtonHookDispatcher;
import io.github.qauxv.util.DexKit;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.Toasts;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import kotlin.Unit;
import kotlin.jvm.functions.Function3;
import kotlinx.coroutines.flow.MutableStateFlow;
import me.kyuubiran.util.UtilsKt;
import mqq.app.AppRuntime;

@FunctionHookEntry
@UiItemAgentEntry
public class ChatTailHook extends CommonConfigFunctionHook implements IInputButtonDecorator {

    public static final String qn_chat_tail_enable = "qn_chat_tail_enable";
    public static final ChatTailHook INSTANCE = new ChatTailHook();
    private static final String ACTION_UPDATE_CHAT_TAIL = "io.github.qauxv.ACTION_UPDATE_CHAT_TAIL";

    @NonNull
    @Override
    public String getName() {
        return "聊天小尾巴";
    }

    @Nullable
    @Override
    public MutableStateFlow<String> getValueState() {
        return null;
    }

    @NonNull
    @Override
    public Function3<IUiItemAgent, Activity, View, Unit> getOnUiItemClickListener() {
        return (agent, activity, view) -> {
            ChatTailFragment fragment = new ChatTailFragment();
            SettingsUiFragmentHostActivity settingActivity = (SettingsUiFragmentHostActivity) activity;
            settingActivity.presentFragment(fragment);
            return Unit.INSTANCE;
        };
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.CHAT_CATEGORY;
    }

    @Override
    public boolean doDecorate(@NonNull String text, @NonNull Parcelable session,
            @NonNull EditText input, @NonNull View sendBtn,
            @NonNull Context ctx1, @NonNull AppRuntime qqApp) {
        if (!isEnabled()) {
            return false;
        }
        if (!TextUtils.isEmpty(ChatTailHook.INSTANCE.getTailCapacity())) {
            int battery = FakeBatteryHook.INSTANCE.isEnabled() ?
                    FakeBatteryHook.INSTANCE.getFakeBatteryStatus() < 1
                            ? ChatTailFragment.getBattery()
                            : FakeBatteryHook.INSTANCE
                                    .getFakeBatteryCapacity()
                    : ChatTailFragment.getBattery();
            String tc = ChatTailHook.INSTANCE.getTailCapacity().
                    replace(ChatTailFragment.delimiter, input.getText())
                    .replace("#model#", Build.MODEL)
                    .replace("#brand#", Build.BRAND)
                    .replace("#battery#", battery + "")
                    .replace("#power#", ChatTailFragment.getPower())
                    .replace("#time#", new SimpleDateFormat(
                            RikkaCustomMsgTimeFormatDialog.getTimeFormat(), Locale.getDefault())
                            .format(new Date()));
            input.setText(tc);
            sendBtn.callOnClick();
            return true;
        } else {
            Toasts.error(ctx1, "你还没有设置小尾巴");
            return false;
        }
    }

    private ChatTailHook() {
        super(qn_chat_tail_enable);
    }

    public static boolean isRegex() {
        return ConfigManager.forCurrentAccount()
                .getBooleanOrFalse(ConfigItems.qn_chat_tail_regex);
    }

    /**
     * 通过正则表达式的消息不会携带小尾巴（主要是考虑到用户可能写错表达式）
     *
     * @param msg 原始聊天消息文本
     * @return 该消息是否通过指定的正则表达式
     */
    public static boolean isPassRegex(String msg) {
        try {
            return Pattern.compile(getTailRegex()).matcher(msg).find();
        } catch (PatternSyntaxException e) {
            Log.e(e);
            return false;
        }
    }

    public static String getTailRegex() {
        // (?:(?![A-Za-z0-9])(?:[\x21-\x7e？！]))$
        return ConfigManager.forCurrentAccount()
                .getStringOrDefault(ConfigItems.qn_chat_tail_regex_text, "");
    }

    public static void setTailRegex(String regex) {
        try {
            ConfigManager cfg = ConfigManager.forCurrentAccount();
            cfg.putString(ConfigItems.qn_chat_tail_regex_text, regex);
            cfg.save();
        } catch (NullPointerException e) {
            Log.e(e);
        }
    }

    @Override
    public boolean initOnce() {
        Class<?> facade = DexKit.doFindClass(DexKit.C_FACADE);
        Method m = null;
        for (Method mi : facade.getDeclaredMethods()) {
            if (!mi.getReturnType().equals(long[].class)) {
                continue;
            }
            Class<?>[] argt = mi.getParameterTypes();
            if (argt.length != 6) {
                continue;
            }
            if (argt[1].equals(Context.class)
                    && argt[3].equals(String.class) && argt[4].equals(ArrayList.class)) {
                m = mi;
                m.setAccessible(true);
                break;
            }
        }
        HookUtils.hookBeforeIfEnabled(this, m, param -> {
            String msg = (String) param.args[3];
            String text = msg;
            final Parcelable session = (Parcelable) param.args[2];
            String uin = "10000";
            if (!isGlobal()) {
                Field field = null;
                for (Field f : session.getClass().getDeclaredFields()) {
                    // 因为有多个同名变量，所以要判断返回类型
                    if (f.getName().equalsIgnoreCase("a") && f.getType() == String.class) {
                        field = f;
                    }
                }
                if (null == field) {
                    field = session.getClass().getDeclaredField("curFriendUin");
                }
                uin = (String) field.get(session);
            }
            ChatTailHook ct = ChatTailHook.INSTANCE;
            Log.i("isRegex:" + ChatTailHook.isRegex());
            Log.i("isPassRegex:" + ChatTailHook.isPassRegex(msg));
            Log.i("getTailRegex:" + ChatTailHook.getTailRegex());
            if ((ct.isGlobal() || ct.containsTroop(uin) || ct.containsFriend(uin))
                    && (!isRegex() || !isPassRegex(msg))) {
                int battery = FakeBatteryHook.INSTANCE.isEnabled() ?
                        FakeBatteryHook.INSTANCE.getFakeBatteryStatus() < 1
                                ? ChatTailFragment.getBattery()
                                : FakeBatteryHook.INSTANCE.getFakeBatteryCapacity()
                        : ChatTailFragment.getBattery();
                text = ct.getTailCapacity()
                        .replace(ChatTailFragment.delimiter, msg)
                        .replace("#model#", Build.MODEL)
                        .replace("#brand#", Build.BRAND)
                        .replace("#battery#", battery + "")
                        .replace("#power#", ChatTailFragment.getPower())
                        .replace("#time#", new SimpleDateFormat(
                                RikkaCustomMsgTimeFormatDialog.getTimeFormat(),
                                Locale.getDefault()).format(new Date()));
                if (ct.getTailCapacity().contains("#Spacemsg#")) {
                    text = text.replace("#Spacemsg#", "");
                    text = UtilsKt.makeSpaceMsg(text);
                }
            }
            param.args[3] = text;
        });
        return true;
    }

    private boolean containsFriend(String uin) {
        String muted = "," + ConfigManager.forCurrentAccount()
                .getString(ConfigItems.qn_chat_tail_friends) + ",";
        return muted.contains("," + uin + ",");
    }

    private boolean isGlobal() {
        return ConfigManager.forCurrentAccount()
                .getBooleanOrFalse(ConfigItems.qn_chat_tail_global);
    }

    private boolean containsTroop(String uin) {
        String muted = "," + ConfigManager.forCurrentAccount()
                .getString(ConfigItems.qn_chat_tail_troops) + ",";
        return muted.contains("," + uin + ",");
    }

    public void setTail(String tail) {
        try {
            ConfigManager cfg = ConfigManager.forCurrentAccount();
            cfg.putString(ConfigItems.qn_chat_tail, tail);
            cfg.save();
        } catch (NullPointerException e) {
            traceError(e);
        }
    }

    public String getTailStatus() {
        return ConfigManager.forCurrentAccount()
                .getStringOrDefault(ConfigItems.qn_chat_tail, "");
    }

    public String getTailCapacity() {
        return getTailStatus().replace("\\n", "\n");
    }

    @Override
    public boolean isEnabled() {
        try {
            ConfigManager cfg = ConfigManager.forCurrentAccount();
            if (cfg != null) {
                return cfg.getBoolean(qn_chat_tail_enable, false);
            }
            return false;
        } catch (Exception e) {
            traceError(e);
            return false;
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        try {
            ConfigManager cfg = ConfigManager.forCurrentAccount();
            cfg.putBoolean(qn_chat_tail_enable, enabled);
            cfg.save();
            InputButtonHookDispatcher.INSTANCE.setEnabled(enabled);
        } catch (final Exception e) {
            traceError(e);
            if (Looper.myLooper() == Looper.getMainLooper()) {
                Toasts.error(HostInfo.getApplication(), e + "");
            } else {
                SyncUtils.post(() -> Toasts
                        .error(HostInfo.getApplication(), e + ""));
            }
        }
    }
}
