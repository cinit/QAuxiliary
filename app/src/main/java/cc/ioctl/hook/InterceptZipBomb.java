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

import androidx.annotation.NonNull;
import cc.ioctl.util.BugUtils;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.DexKit;
import io.github.qauxv.util.Toasts;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@FunctionHookEntry
@UiItemAgentEntry
public class InterceptZipBomb extends CommonSwitchFunctionHook {

    public static final InterceptZipBomb INSTANCE = new InterceptZipBomb();

    private InterceptZipBomb() {
        super(new int[]{DexKit.C_ZipUtils_biz});
    }

    @Override
    protected boolean initOnce() throws Exception {
        Class<?> zipUtil = DexKit.doFindClass(DexKit.C_ZipUtils_biz);
        Method m;
        try {
            m = zipUtil.getMethod("a", File.class, String.class);
        } catch (NoSuchMethodException e) {
            m = zipUtil.getMethod("unZipFile", File.class, String.class);
        }
        HookUtils.hookBeforeIfEnabled(this, m, 51, param -> {
            File file = (File) param.args[0];
            ZipFile zipFile = new ZipFile(file);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            long sizeSum = 0;
            while (entries.hasMoreElements()) {
                sizeSum += entries.nextElement().getSize();
            }
            zipFile.close();
            if (sizeSum >= 104550400) {
                param.setResult(null);
                Toasts.show(HostInfo.getApplication(),
                        String.format("已拦截 %s ,解压后大小异常: %s",
                                file.getPath(), BugUtils.getSizeString(sizeSum)));
            }
        });
        return true;
    }

    @NonNull
    @Override
    public String getName() {
        return "拦截异常体积图片加载";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.MESSAGE_CATEGORY;
    }
}
