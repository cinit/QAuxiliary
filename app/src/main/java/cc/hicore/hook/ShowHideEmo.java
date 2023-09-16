/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the qwq233 Universal License
 * as published on https://github.com/qwq233/license; either
 * version 2 of the License, or any later version and our EULA as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the qwq233 Universal License for more details.
 *
 * See
 * <https://github.com/qwq233/license>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package cc.hicore.hook;

import androidx.annotation.NonNull;
import cc.hicore.QApp.QAppUtils;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.IoUtils;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import io.github.qauxv.util.dexkit.NT_SysAndEmojiResInfo;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

@FunctionHookEntry
@UiItemAgentEntry
public class ShowHideEmo extends CommonSwitchFunctionHook {

    public static final ShowHideEmo INSTANCE = new ShowHideEmo();

    private ShowHideEmo() {
        super(new DexKitTarget[]{NT_SysAndEmojiResInfo.INSTANCE});
    }

    @NonNull
    @Override
    public String getName() {
        return "显示隐藏表情";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY;
    }

    @Override
    protected boolean initOnce() throws Exception {
        if (QAppUtils.isQQnt()) {
            Method[] ms = DexKit.requireClassFromCache(NT_SysAndEmojiResInfo.INSTANCE).getDeclaredMethods();
            for (Method m : ms) {
                if (m.getReturnType() == boolean.class && !Modifier.isAbstract(m.getModifiers())) {
                    HookUtils.hookBeforeIfEnabled(this, m, param -> param.setResult(false));
                    return true;
                }
            }
            return false;
        }
        HookUtils.hookBeforeIfEnabled(this,
                Initiator.loadClass("com.tencent.mobileqq.emoticon.QQSysAndEmojiResInfo")
                        .getDeclaredMethod("isEmoticonHide", Initiator.loadClass("com.tencent.mobileqq.emoticon.QQSysAndEmojiResInfo$QQEmoConfigItem")),
                param -> param.setResult(false)
        );
        Class<?> kQQSysFaceUtil = Initiator.load("com.tencent.mobileqq.emoticon.QQSysFaceUtil");
        if (kQQSysFaceUtil != null) {
            HookUtils.hookBeforeIfEnabled(this, kQQSysFaceUtil.getDeclaredMethod("isAniSticker", int.class),
                    param -> {
                        int check = (int) param.args[0];
                        param.setResult(isSticker(check));
                    });
        }
        return true;
    }

    public static boolean isSticker(int i) {
        try {
            File faceConfig = new File(HostInfo.getApplication().getDataDir(), "files/qq_emoticon_res/face_config.json");
            if (!faceConfig.exists()) {
                return false;
            }
            String s = new String(IoUtils.readFile(faceConfig), StandardCharsets.UTF_8);
            JSONObject j = new JSONObject(s);
            JSONArray arr = j.getJSONArray("sysface");
            for (int ix = 0; ix < arr.length(); ix++) {
                JSONObject obj = arr.getJSONObject(ix);
                if (obj.optString("AQLid").equals("" + i)) {
                    if (obj.has("AniStickerType")) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            Log.w(e);
            return false;
        }
    }
}
