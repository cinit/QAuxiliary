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

import static io.github.qauxv.util.Initiator._BaseQQAppInterface;
import static io.github.qauxv.util.Initiator._BaseSessionInfo;
import static io.github.qauxv.util.Initiator._Emoticon;
import static io.github.qauxv.util.Initiator._QQAppInterface;
import static io.github.qauxv.util.Initiator._SessionInfo;
import static io.github.qauxv.util.Initiator._StickerInfo;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import cc.hicore.QApp.QAppUtils;
import cc.ioctl.util.HostInfo;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.ui.CommonContextWrapper;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.LicenseStatus;
import io.github.qauxv.util.QQVersion;
import io.github.qauxv.util.dexkit.CPicEmoticonInfo;
import io.github.qauxv.util.dexkit.CPngFrameUtil;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import java.util.Random;

@FunctionHookEntry
@UiItemAgentEntry
public class CheatHook extends CommonSwitchFunctionHook {

    public static final CheatHook INSTANCE = new CheatHook();
    private final String[] diceItem = {"1", "2", "3", "4", "5", "6"};
    private final String[] morraItem = {"石头", "剪刀", "布"};

    private int diceNum = -1;
    private int morraNum = -1;

    private CheatHook() {
        super(new DexKitTarget[]{CPngFrameUtil.INSTANCE, CPicEmoticonInfo.INSTANCE});
    }

    @Override
    protected boolean initOnce() throws Exception {
        var method = HostInfo.requireMinQQVersion(QQVersion.QQ_8_8_93) ? "d" : "a";
        XposedHelpers.findAndHookMethod(DexKit.requireClassFromCache(CPngFrameUtil.INSTANCE), method, int.class,
                new XC_MethodHook(43) {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (LicenseStatus.sDisableCommonHooks && !isEnabled()) {
                            return;
                        }
                        int num = (int) param.args[0];
                        if (num == 6 && diceNum != -1) {
                            param.setResult(diceNum);
                        } else if (num == 3 && morraNum != -1) {
                            param.setResult(morraNum);
                        }
                    }
                });

        XC_MethodHook hook = new XC_MethodHook(43) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (LicenseStatus.sDisableCommonHooks && !isEnabled()) {
                    return;
                }
                Context context = (Context) param.args[1];
                Object emoticon = param.args[3];
                String name = (String) XposedHelpers.getObjectField(emoticon, "name");
                if ("随机骰子".equals(name) || "骰子".equals(name)) {
                    param.setResult(null);
                    showDiceDialog(context, param);
                } else if ("猜拳".equals(name)) {
                    param.setResult(null);
                    showMorraDialog(context, param);
                }
            }
        };
        String Method = "a";

        if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_4_8)) {
            Method = "sendMagicEmoticon";
        }
        if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_5_0)) {
            XposedHelpers.findAndHookMethod(Initiator.loadClass("com.tencent.mobileqq.emoticonview.sender.PicEmoticonInfoSender"),
                    Method, _BaseQQAppInterface(), Context.class, _BaseSessionInfo(), _Emoticon(), _StickerInfo(), hook);
        } else {
            XposedHelpers.findAndHookMethod(DexKit.requireClassFromCache(CPicEmoticonInfo.INSTANCE),
                    Method, _QQAppInterface(), Context.class, _SessionInfo(), _Emoticon(), _StickerInfo(), hook);
        }
        return true;
    }

    private static final Random RNG = new Random();

    private void showDiceDialog(Context context, XC_MethodHook.MethodHookParam param) {
        AlertDialog alertDialog = new AlertDialog.Builder(CommonContextWrapper.createAppCompatContext(context))
                .setTitle("自定义骰子")
                .setSingleChoiceItems(diceItem, diceNum, (dialog, which) -> diceNum = which)
                .setNegativeButton("取消", null)
                .setNeutralButton("随机", (dialog, which) -> {
                    diceNum = Math.abs(RNG.nextInt(6));
                    try {
                        XposedBridge.invokeOriginalMethod(param.method, param.thisObject,
                                param.args);
                    } catch (Exception e) {
                        traceError(e);
                    }
                })
                .setPositiveButton("确定", (dialog, which) -> {
                    try {
                        XposedBridge.invokeOriginalMethod(param.method, param.thisObject,
                                param.args);
                    } catch (Exception e) {
                        traceError(e);
                    }
                })
                .create();
        alertDialog.show();
    }

    private void showMorraDialog(Context context, XC_MethodHook.MethodHookParam param) {
        AlertDialog alertDialog = new AlertDialog.Builder(CommonContextWrapper.createAppCompatContext(context))
                .setTitle("自定义猜拳")
                .setSingleChoiceItems(morraItem, morraNum, (dialog, which) -> morraNum = which)
                .setNegativeButton("取消", null)
                .setNeutralButton("随机", (dialog, which) -> {
                    morraNum = Math.abs(RNG.nextInt(3));
                    try {
                        XposedBridge.invokeOriginalMethod(param.method, param.thisObject,
                                param.args);
                    } catch (Exception e) {
                        traceError(e);
                    }
                })
                .setPositiveButton("确定", (dialog, which) -> {
                    try {
                        XposedBridge.invokeOriginalMethod(param.method, param.thisObject,
                                param.args);
                    } catch (Exception e) {
                        traceError(e);
                    }
                })
                .create();
        alertDialog.show();
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.CHAT_CATEGORY;
    }

    @NonNull
    @Override
    public String getName() {
        return "自定义猜拳骰子";
    }

    @Nullable
    @Override
    public CharSequence getDescription() {
        return "不支持NT版本，已停止维护";
    }

    @Override
    public boolean isAvailable() {
        return !QAppUtils.isQQnt();
    }
}
