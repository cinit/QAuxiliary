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
package cc.ioctl.hook.chat;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.dialog.RikkaCustomMsgTimeFormatDialog;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.Reflex;
import io.github.qauxv.base.IUiItemAgent;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonConfigFunctionHook;
import io.github.qauxv.util.dexkit.CTimeFormatterUtils;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Locale;
import kotlin.Unit;
import kotlin.jvm.functions.Function3;
import kotlinx.coroutines.flow.MutableStateFlow;

/**
 * 自定义聊天页面时间格式
 * <p>
 * Peak frequency: ~94 invocations per second
 */
@FunctionHookEntry
@UiItemAgentEntry
public class CustomMsgTimeFormat extends CommonConfigFunctionHook {

    public static final CustomMsgTimeFormat INSTANCE = new CustomMsgTimeFormat();

    protected CustomMsgTimeFormat() {
        super(new DexKitTarget[]{CTimeFormatterUtils.INSTANCE});
    }

    @NonNull
    @Override
    public String getName() {
        return "自定义聊天页面时间格式";
    }

    @Nullable
    @Override
    public MutableStateFlow<String> getValueState() {
        return null;
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.CHAT_CATEGORY;
    }

    @NonNull
    @Override
    public Function3<IUiItemAgent, Activity, View, Unit> getOnUiItemClickListener() {
        return (agent, activity, view) -> {
            RikkaCustomMsgTimeFormatDialog dialog = new RikkaCustomMsgTimeFormatDialog();
            dialog.showDialog(activity);
            return Unit.INSTANCE;
        };
    }

    @Override
    public boolean initOnce() throws NoSuchMethodException {
        Method method;
        try {
            method = Reflex.findSingleMethod(DexKit.requireClassFromCache(CTimeFormatterUtils.INSTANCE),
                    CharSequence.class, false, Context.class, int.class, long.class, boolean.class, boolean.class);
        } catch (NoSuchMethodException ignored) {
            method = Reflex.findSingleMethod(DexKit.requireClassFromCache(CTimeFormatterUtils.INSTANCE),
                    CharSequence.class, false, Context.class, int.class, long.class);
        }
        HookUtils.hookBeforeIfEnabled(this, method, param -> {
            String result = formatTime((long) param.args[2]);
            if (result != null) {
                param.setResult(result);
            }
        });
        return true;
    }

    private String sCachedFormatString = null;
    private SimpleDateFormat sCachedFormatInstance = null;

    public String formatTime(long time) {
        String fmt = RikkaCustomMsgTimeFormatDialog.getCurrentMsgTimeFormat();
        if (!fmt.equals(sCachedFormatString) || sCachedFormatInstance == null) {
            sCachedFormatInstance = new SimpleDateFormat(fmt, Locale.ROOT);
            sCachedFormatString = fmt;
        }
        return sCachedFormatInstance.format(time);
    }

    @Override
    public boolean isEnabled() {
        return RikkaCustomMsgTimeFormatDialog.IsEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        //not supported.
    }
}
