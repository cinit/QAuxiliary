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

import androidx.annotation.NonNull;
import cc.ioctl.util.BugUtils;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.Reflex;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.Toasts;
import io.github.qauxv.util.dexkit.CHttpDownloader;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.Objects;

@FunctionHookEntry
@UiItemAgentEntry
public class BlockFluxThief extends CommonSwitchFunctionHook {

    public static final BlockFluxThief INSTANCE = new BlockFluxThief();

    private BlockFluxThief() {
        super(new DexKitTarget[]{CHttpDownloader.INSTANCE});
    }

    static long requestUrlSizeBlocked(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        int code = conn.getResponseCode();
        String lenStr = conn.getHeaderField("Content-Length");
        conn.getInputStream().close();
        conn.disconnect();
        if (lenStr == null) {
            return -1L;
        } else {
            try {
                return Long.parseLong(lenStr);
            } catch (NumberFormatException th) {
                Log.d(String.format(Locale.ROOT, "BlockFluxThief/W [%d] %s %s", code, lenStr, url));
                return -1;
            }
        }
    }

    @Override
    protected boolean initOnce() throws Exception {
        Method downloadImage = null;
        for (Method m : DexKit.requireClassFromCache(CHttpDownloader.INSTANCE).getDeclaredMethods()) {
            if (m.getReturnType() != File.class || Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            Class<?>[] argt = m.getParameterTypes();
            if (argt.length != 5 || argt[0] != OutputStream.class || argt[3] != int.class
                    || argt[4] != URL.class) {
                continue;
            }
            downloadImage = m;
            break;
        }
        Objects.requireNonNull(downloadImage,
                "unable to find CHttpDownloader.downloadImage");
        HookUtils.hookAfterIfEnabled(this, downloadImage, param -> {
            long maxSize = 32 * 1024 * 1024;//32MiB
            String url = (String) Reflex.getInstanceObjectOrNull(param.args[1], "urlStr");
            Class<?> cHttpDownloader = param.method.getDeclaringClass();
            Method mGetFilePath = null;
            try {
                mGetFilePath = cHttpDownloader.getMethod("getFilePath", String.class);
            } catch (NoSuchMethodException ignored) {
            }
            if (mGetFilePath == null) {
                mGetFilePath = Reflex.findMethod(cHttpDownloader, String.class, "d", String.class);
            }
            String savePath = (String) mGetFilePath.invoke(null, url);
            if (!new File(savePath).exists()) {
                try {
                    long size = requestUrlSizeBlocked(url);
                    if (size != -1) {
                        if (size > maxSize) {
                            param.setResult(null);
                            Toasts.show(null, String.format(Locale.ROOT, "已拦截异常图片加载, 大小: %s",
                                    BugUtils.getSizeString(size)));
                        }
                    } else {
                        // TODO: 2021-1-9 Unknown size, do nothing?
                    }
                } catch (IOException e) {
                    Log.d("BlockFluxThief/Req " + e + " URL=" + filterUrlForLog(url));
                }
            }
        });
        return true;
    }

    private static String filterUrlForLog(String url) {
        // keep scheme and host and port
        URL u;
        try {
            u = new URL(url);
        } catch (MalformedURLException e) {
            return url;
        }
        return u.getProtocol() + "://" + u.getHost() + (u.getPort() == -1 ? "" : ":" + u.getPort()) + "/...";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.CHAT_CATEGORY;
    }

    @NonNull
    @Override
    public String getName() {
        return "拦截异常体积图片加载";
    }
}
