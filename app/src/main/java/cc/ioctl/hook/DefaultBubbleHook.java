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
import android.app.Application;
import android.content.Context;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.SyncUtils;
import io.github.qauxv.base.IDynamicHook;
import io.github.qauxv.base.ISwitchCellAgent;
import io.github.qauxv.base.IUiItemAgent;
import io.github.qauxv.base.IUiItemAgentProvider;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.step.Step;
import java.io.File;
import java.util.Collections;
import java.util.List;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;
import kotlinx.coroutines.flow.MutableStateFlow;

@FunctionHookEntry
@UiItemAgentEntry
public class DefaultBubbleHook implements IDynamicHook, IUiItemAgentProvider, IUiItemAgent {

    public static final DefaultBubbleHook INSTANCE = new DefaultBubbleHook();

    private DefaultBubbleHook() {
    }

    private static final String[] paths = {"/bubble_info", "/files/bubble_info", "/files/bubble_paster"};

    @Override
    public boolean isAvailable() {
        return !HostInfo.isTim();
    }

    @Override
    public boolean isEnabled() {
        if (HostInfo.isTim()) {
            return false;
        }
        Application app = HostInfo.getApplication();
        for (String path : paths) {
            File dir = new File(app.getFilesDir().getAbsolutePath() + path);
            if (dir.exists()) {
                return !dir.canRead();
            }
        }
        return false;
    }

    @Override
    public void setEnabled(boolean enabled) {
        for (String path : paths) {
            File dir = new File(HostInfo.getApplication().getFilesDir().getAbsolutePath() + path);
            boolean curr = !dir.exists() || !dir.canRead();
            if (dir.exists()) {
                if (enabled && !curr) {
                    dir.setWritable(false);
                    dir.setReadable(false);
                    dir.setExecutable(false);
                }
                if (!enabled && curr) {
                    dir.setWritable(true);
                    dir.setReadable(true);
                    dir.setExecutable(true);
                }
            }
        }

    }

    private final ISwitchCellAgent switchCellAgent = new ISwitchCellAgent() {
        @Override
        public boolean isChecked() {
            return isEnabled();
        }

        @Override
        public void setChecked(boolean isChecked) {
            setEnabled(isChecked);
        }

        @Override
        public boolean isCheckable() {
            return isAvailable();
        }
    };

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public boolean isInitializationSuccessful() {
        return isInitialized();
    }

    @Override
    public boolean initialize() {
        return true;
    }

    @NonNull
    @Override
    public List<Throwable> getRuntimeErrors() {
        return Collections.emptyList();
    }

    @Override
    public int getTargetProcesses() {
        return SyncUtils.PROC_MAIN;
    }

    @Override
    public boolean isTargetProcess() {
        return SyncUtils.isMainProcess();
    }

    @Override
    public boolean isPreparationRequired() {
        return false;
    }

    @Nullable
    @Override
    public Step[] makePreparationSteps() {
        return null;
    }

    @Override
    public boolean isApplicationRestartRequired() {
        return false;
    }

    @NonNull
    @Override
    public Function1<IUiItemAgent, String> getTitleProvider() {
        return agent -> "强制使用默认消息气泡";
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
        return switchCellAgent;
    }

    @Nullable
    @Override
    public Function3<IUiItemAgent, Activity, View, Unit> getOnClickListener() {
        return null;
    }

    @Nullable
    @Override
    public Function2<IUiItemAgent, Context, String[]> getExtraSearchKeywordProvider() {
        return null;
    }

    @NonNull
    @Override
    public IUiItemAgent getUiItemAgent() {
        return this;
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Simplify.CHAT_DECORATION;
    }

    @NonNull
    @Override
    public String getItemAgentProviderUniqueIdentifier() {
        return getClass().getName();
    }
}
