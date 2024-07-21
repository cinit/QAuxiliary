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

import static cc.ioctl.util.Reflex.findField;
import static io.github.qauxv.util.Initiator._PicItemBuilder;

import android.view.View;
import androidx.annotation.NonNull;
import cc.hicore.QApp.QAppUtils;
import cc.hicore.ReflectUtil.XMethod;
import cc.ioctl.util.HookUtils;
import io.github.qauxv.util.xpcompat.XC_MethodHook;
import io.github.qauxv.util.xpcompat.XposedHelpers;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.bridge.AIOUtilsImpl;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.LicenseStatus;
import io.github.qauxv.util.dexkit.CAIOUtils;
import io.github.qauxv.util.dexkit.DexKitTarget;
import java.lang.reflect.Field;

@UiItemAgentEntry
@FunctionHookEntry
public class EmoPicHook extends CommonSwitchFunctionHook {

    public static final EmoPicHook INSTANCE = new EmoPicHook();

    private EmoPicHook() {
        super(new DexKitTarget[]{CAIOUtils.INSTANCE});
    }

    @Override
    public boolean initOnce() throws Exception {
        if (QAppUtils.isQQnt()){
            HookUtils.hookBeforeIfEnabled(this, XMethod.clz("com.tencent.qqnt.aio.adapter.api.impl.RichMediaBrowserApiImpl").name("checkIsFavPicAndShowPreview").ignoreParam().get(),
                    param -> param.setResult(false));
            return true;
        }
        XposedHelpers.findAndHookMethod(_PicItemBuilder(),
                "onClick", View.class, new XC_MethodHook(51) {

                    Field f_picExtraData = null;
                    Field f_imageBizType = null;
                    Field f_imageType = null;

                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (LicenseStatus.sDisableCommonHooks || !isEnabled()) {
                            return;
                        }
                        try {
                            Object chatMsg = AIOUtilsImpl.getChatMessage((View) param.args[0]);
                            if (chatMsg == null) {
                                return;
                            }
                            if (f_picExtraData == null) {
                                f_picExtraData = findField(chatMsg.getClass(), null, "picExtraData");
                                f_picExtraData.setAccessible(true);
                            }
                            Object picMessageExtraData = f_picExtraData.get(chatMsg);
                            if (f_imageType == null) {
                                f_imageType = findField(chatMsg.getClass(), null, "imageType");
                                f_imageType.setAccessible(true);
                            }
                            f_imageType.setInt(chatMsg, 0);
                            if (picMessageExtraData != null) {
                                if (f_imageBizType == null) {
                                    f_imageBizType = findField(picMessageExtraData.getClass(), null, "imageBizType");
                                    f_imageBizType.setAccessible(true);
                                }
                                f_imageBizType.setInt(picMessageExtraData, 0);
                            }
                        } catch (Throwable e) {
                            traceError(e);
                            throw e;
                        }
                    }
                });
        return true;
    }

    @NonNull
    @Override
    public String getName() {
        return "以图片方式打开表情";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.CHAT_CATEGORY;
    }
}
