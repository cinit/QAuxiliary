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

import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.SyncUtils;
import io.github.qauxv.util.Toasts;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import io.github.qauxv.util.dexkit.NWebSecurityPluginV2_callback;
import java.lang.reflect.Method;

@FunctionHookEntry
@UiItemAgentEntry
public class BrowserRestrictMitigation extends CommonSwitchFunctionHook {

    public static final BrowserRestrictMitigation INSTANCE = new BrowserRestrictMitigation();

    private BrowserRestrictMitigation() {
        super(SyncUtils.PROC_TOOL, new DexKitTarget[]{NWebSecurityPluginV2_callback.INSTANCE});
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.MISC_CATEGORY;
    }

    @NonNull
    @Override
    public String getName() {
        return "禁用内置浏览器网页拦截";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "允许在内置浏览器访问非官方页面";
    }

    @Override
    protected boolean initOnce() throws Exception {
        // com.tencent.mobileqq.webview.WebSecurityPluginV2$1.callback(Bundle)V
        Method callback = DexKit.requireMethodFromCache(NWebSecurityPluginV2_callback.INSTANCE);
        HookUtils.hookBeforeIfEnabled(this, callback, param -> {
            Bundle bundle = (Bundle) param.args[0];
            if (bundle != null && bundle.getInt("result", -1) == 0) {
                int jumpResult = bundle.getInt("jumpResult");
                int level = bundle.getInt("level");
                long operationBit = bundle.getLong("operationBit");
                String jumpUrl = bundle.getString("jumpUrl");
                if (jumpResult != 0 && !TextUtils.isEmpty(jumpUrl)) {
                    // disable jump
                    bundle.putInt("jumpResult", 0);
                    bundle.putString("jumpUrl", "");
                    String msg = "阻止跳转, jumpResult: " + jumpResult + ", level: " + level
                            + ", operationBit: " + operationBit + ", jumpUrl: " + jumpUrl;
                    Toasts.show(HostInfo.getApplication(), msg);
                }
            }
        });
        return true;
    }
}
