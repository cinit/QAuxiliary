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
import android.os.Environment;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.base.IUiItemAgent;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.config.ConfigItems;
import io.github.qauxv.config.ConfigManager;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonConfigFunctionHook;
import io.github.qauxv.util.DexKit;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.QQVersion;
import io.github.qauxv.util.Toasts;
import java.lang.reflect.Field;
import kotlin.Unit;
import kotlin.jvm.functions.Function3;
import kotlinx.coroutines.flow.MutableStateFlow;

@FunctionHookEntry
@UiItemAgentEntry
public class FileRecvRedirect extends CommonConfigFunctionHook {

    public static final FileRecvRedirect INSTANCE = new FileRecvRedirect();
    private boolean inited = false;

    private Field TARGET_FIELD = null;

    private FileRecvRedirect() {
        super(new int[]{DexKit.C_APP_CONSTANTS});
    }

    @NonNull
    @Override
    public String getName() {
        return "下载文件重定向";
    }

    @Nullable
    @Override
    public String[] getExtraSearchKeywords() {
        return new String[]{"下载重定向"};
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.EXPERIMENTAL_CATEGORY;
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
            Toasts.show(activity,"坏了, 没修");
            return Unit.INSTANCE;
        };
    }

    @Override
    public boolean initOnce() throws Exception {
        String redirectPath = getRedirectPath();
        if (redirectPath != null) {
            inited = doSetPath(redirectPath);
            return inited;
        } else {
            return false;
        }
    }

    private boolean doSetPath(String str) {
        Field[] fields = DexKit.doFindClass(DexKit.C_APP_CONSTANTS).getFields();
        try {
            if (TARGET_FIELD == null) {
                for (Field field : fields) {
                    field.setAccessible(true);
                    Object value = field.get(null);
                    String path = String.valueOf(value);
                    if (path.toLowerCase().endsWith("file_recv/")) {
                        TARGET_FIELD = field;
                        break;
                    }
                }
            }
            TARGET_FIELD.setAccessible(true);
            TARGET_FIELD.set(null, str);
            return true;
        } catch (Exception e) {
            Log.e(e);
            return false;
        }
    }

    public String getDefaultPath() {
        if (HostInfo.isTim()) {
            return Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/Tencent/TIMfile_recv/";
        } else {
            if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_2_8)) {
                return HostInfo.getApplication()
                        .getExternalFilesDir(null) + "/Tencent/QQfile_recv/";
            } else {
                return Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/Tencent/QQfile_recv/";
            }
        }
    }

    @Nullable
    public String getRedirectPath() {
        return ConfigManager.getDefaultConfig().getString(ConfigItems.qn_file_recv_redirect_path);
    }

    public void setRedirectPathAndEnable(String path) {
        try {
            ConfigManager cfg = ConfigManager.getDefaultConfig();
            cfg.putString(ConfigItems.qn_file_recv_redirect_path, path);
            cfg.putBoolean(ConfigItems.qn_file_recv_redirect_enable, true);
            cfg.save();
            inited = doSetPath(path);
        } catch (Exception e) {
            Log.e(e);
        }
    }

    @Override
    public boolean isInitialized() {
        return inited;
    }

    @Override
    public boolean isEnabled() {
        return false && ConfigManager.getDefaultConfig().getBooleanOrFalse(ConfigItems.qn_file_recv_redirect_enable);
    }

    /**
     * Still follow the rule only apply if it is already inited.
     *
     * @param enabled if true set to config value, otherwise restore to default value
     */
    @Override
    public void setEnabled(boolean enabled) {
        ConfigManager cfg = ConfigManager.getDefaultConfig();
        cfg.putBoolean(ConfigItems.qn_file_recv_redirect_enable, enabled);
        cfg.save();
        if (inited) {
            if (enabled) {
                String path = getRedirectPath();
                if (path != null) {
                    inited = doSetPath(path);
                }
            } else {
                doSetPath(getDefaultPath());
            }
        }
    }
}
