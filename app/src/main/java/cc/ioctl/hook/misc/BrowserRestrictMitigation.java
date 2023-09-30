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
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.SyncUtils;
import io.github.qauxv.util.Toasts;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import io.github.qauxv.util.dexkit.NWebSecurityPluginV2_callback;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import kotlin.collections.ArraysKt;

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
        Class<?> kCommonJsPluginFactory = Initiator.load("com.tencent.mobileqq.webprocess.WebAccelerateHelper$CommonJsPluginFactory");
        if (kCommonJsPluginFactory == null) {
            Class<?> aClassTemp = Initiator.loadClass("com.tencent.mobileqq.webprocess.WebAccelerateHelper");
            Method[] methods = aClassTemp.getDeclaredMethods();
            for (Method m : methods) {
                Class<?>[] parameterTypes = m.getParameterTypes();
                for (Class<?> ct : parameterTypes) {
                    if (ct.getSimpleName().contains("CommonJsPluginFactory")) {
                        kCommonJsPluginFactory = ct;
                        break;
                    }
                }
            }
        }
        Objects.requireNonNull(kCommonJsPluginFactory, "kCommonJsPluginFactory is null");
        Method m1 = ArraysKt.single(kCommonJsPluginFactory.getDeclaredMethods(), m -> m.getReturnType() == List.class);
        HookUtils.hookAfterIfEnabled(this, m1, param -> {
            ArrayList array = (ArrayList) param.getResult();
            ArrayList arrayTemp = (ArrayList) array.clone();
            for (Object obj : arrayTemp) {
                Class<?> mPlugin = obj.getClass();
                String mPluginNameSpace = (String) mPlugin.getField("mPluginNameSpace").get(obj);
                if (mPluginNameSpace.equals("forceHttps") || mPluginNameSpace.contains("UrlSaveVerify")
                        || mPluginNameSpace.equals("Webso") || mPluginNameSpace.contains("Report")) {
                    array.remove(obj);
                    Log.d("WebSec: rm-" + mPlugin.getName());
                    continue;
                }
                Method[] methods = mPlugin.getDeclaredMethods();
                for (Method m : methods) {
                    Class<?>[] parameterTypes = m.getParameterTypes();
                    if (parameterTypes.length > 0) {
                        Class<?> parameterType = parameterTypes[0];
                        if (parameterType.getSimpleName().equals("MessageRecord")) {
                            array.remove(obj);
                        }
                    }
                }
            }
            param.setResult(array);
        });

        Class<?> AbsWebViewClass = Initiator.loadClass("com.tencent.mobileqq.webview.AbsWebView");
        Method method_bindAllJavaScript;
        try {
            method_bindAllJavaScript = AbsWebViewClass.getDeclaredMethod("bindAllJavaScript");
        } catch (NoSuchMethodException e) {
            try {
                method_bindAllJavaScript = AbsWebViewClass.getDeclaredMethod("bindBaseJavaScript");
            } catch (NoSuchMethodException ex) {
                e.addSuppressed(ex);
                throw e;
            }
        }
        Field mPluginListField = AbsWebViewClass.getField("mPluginList");
        HookUtils.hookAfterIfEnabled(this, method_bindAllJavaScript, param -> {
            ArrayList mPluginList = (ArrayList) mPluginListField.get(AbsWebViewClass);
            ArrayList arrayTemp = (ArrayList) mPluginList.clone();
            for (Object o : arrayTemp) {
                Class<?> mPlugin = o.getClass();
                String mPluginNameSpace = (String) mPlugin.getField("mPluginNameSpace").get(o);
                if (mPluginNameSpace.contains("UrlSaveVerify") || mPluginNameSpace.equals("Webso")
                        || mPluginNameSpace.contains("Report")) {
                    mPluginList.remove(o);
                    Log.d("WebSec: rm-" + mPlugin.getName());
                    continue;
                }
                Method[] methods = mPlugin.getMethods();
                for (Method m : methods) {
                    Class<?>[] parameterTypes = m.getParameterTypes();
                    if (parameterTypes.length > 0) {
                        Class<?> parameterType = parameterTypes[0];
                        if (parameterType.getSimpleName().equals("MessageRecord")) {
                            mPluginList.remove(o);
                            Log.d("HookWebSecurity: hooked!");
                        }
                    }
                }
            }
            mPluginListField.set(AbsWebViewClass, mPluginList);
        });
        return true;
    }
}
