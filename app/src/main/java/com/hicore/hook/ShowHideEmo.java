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

package com.hicore.hook;

import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.IoUtils;
import io.github.qauxv.util.Log;
import java.io.File;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

@FunctionHookEntry
@UiItemAgentEntry
public class ShowHideEmo extends CommonSwitchFunctionHook {

    public static final ShowHideEmo INSTANCE = new ShowHideEmo();

    private ShowHideEmo() {
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
