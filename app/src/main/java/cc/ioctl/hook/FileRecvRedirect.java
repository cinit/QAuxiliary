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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import com.afollestad.materialdialogs.MaterialDialog;
import de.robv.android.xposed.XposedHelpers;
import io.github.qauxv.SyncUtils;
import io.github.qauxv.base.IUiItemAgent;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.config.ConfigItems;
import io.github.qauxv.config.ConfigManager;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonConfigFunctionHook;
import io.github.qauxv.util.DexKit;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.QQVersion;
import io.github.qauxv.util.Toasts;
import java.io.File;
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
        super(SyncUtils.PROC_ANY & ~(SyncUtils.PROC_MSF | SyncUtils.PROC_UNITY | SyncUtils.PROC_MINI), new int[]{DexKit.C_APP_CONSTANTS});
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
            LinearLayout mRoot = new LinearLayout(activity);
            mRoot.setOrientation(LinearLayout.VERTICAL);

            CheckBox start = new CheckBox(activity);
            start.setText("开启下载重定向");
            start.setChecked(isEnabled());

            mRoot.addView(start);

            EditText PathSet = new EditText(activity);
            PathSet.setText(getRedirectPath());
            mRoot.addView(PathSet);
            PathSet.setVisibility(start.isChecked() ? View.VISIBLE :View.GONE);

            start.setOnCheckedChangeListener((buttonView, isChecked) -> PathSet.setVisibility(isChecked ? View.VISIBLE :View.GONE));

            MaterialDialog dialog = new MaterialDialog(activity,MaterialDialog.getDEFAULT_BEHAVIOR());
            dialog.title(null,"下载文件重定向");
            dialog.positiveButton(null, "保存", materialDialog -> {
                String Path = PathSet.getText().toString();
                if (!CheckPathIsAvailable(Path)){
                    Toasts.show(activity,"目录可能无效");
                    return null;
                }
                setEnabled(start.isChecked());
                if (start.isChecked()){
                    setRedirectPathAndEnable(PathSet.getText().toString());
                }
                Toasts.show(activity,"已保存,请重启QQ");
                return null;
            });
            dialog.negativeButton(null, "取消", materialDialog -> null);
            dialog.getView().contentLayout.setCustomView(null);
            dialog.getView().contentLayout.addCustomView(null,mRoot,false,false,false);

            dialog.show();

            return Unit.INSTANCE;
        };
    }
    private static String CacheDefPath;
    @Override
    public boolean initOnce() throws Exception {
        CacheDefPath = getDefaultPath();
        String redirectPath = getRedirectPath();
        if (redirectPath != null) {
            inited = doSetPath(redirectPath);
            return inited;
        } else {
            return false;
        }
    }

    private boolean doSetPath(String str) {

        try {
            if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_2_8)){
                if(!inited){
                    HookUtils.hookAfterIfEnabled(this, XposedHelpers.findMethodBestMatch(Initiator.load("com.tencent.mobileqq.vfs.VFSAssistantUtils"), "getSDKPrivatePath", String.class), param -> {
                        String getResult = (String) param.getResult();
                        File checkAvailable = new File(getResult);
                        if (checkAvailable.exists() && checkAvailable.isFile())return;//如果文件存在则不处理,防止已下载的文件出现异常
                        if (getResult.startsWith(CacheDefPath)){
                            //在QQ尝试获取文件目录时进行替换
                            param.setResult(getRedirectPath()+getResult.substring(CacheDefPath.length()));
                        }
                    });
                }
            }else {
                Field[] fields = DexKit.doFindClass(DexKit.C_APP_CONSTANTS).getFields();
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
            }
            return true;
        } catch (Exception e) {
            Log.e(e);
            return false;
        }
    }
    public boolean CheckPathIsAvailable(String Path){
        File f = new File(Path);
        f = f.getParentFile();
        return f != null && f.listFiles() != null;
    }

    public String getDefaultPath() {
        if (HostInfo.isTim()) {
            return Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/Tencent/TIMfile_recv/";
        } else {
            if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_2_8)) {
                return HostInfo.getApplication()
                        .getExternalFilesDir(null).getParent() + "/Tencent/QQfile_recv";
            } else {
                return Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/Tencent/QQfile_recv";
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
    public boolean isInitializationSuccessful() {
        return isInitialized();
    }

    @Override
    public boolean isEnabled() {
        return ConfigManager.getDefaultConfig().getBooleanOrFalse(ConfigItems.qn_file_recv_redirect_enable);
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
