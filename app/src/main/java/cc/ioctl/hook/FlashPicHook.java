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

import static cc.ioctl.util.Reflex.getShortClassName;
import static io.github.qauxv.util.Initiator._MessageRecord;
import static io.github.qauxv.util.Initiator._PicItemBuilder;
import static io.github.qauxv.util.Initiator.load;

import android.text.TextUtils;
import android.view.View;
import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.Reflex;
import de.robv.android.xposed.XposedHelpers;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.DexKit;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@UiItemAgentEntry
@FunctionHookEntry
public class FlashPicHook extends CommonSwitchFunctionHook {

    public static final FlashPicHook INSTANCE = new FlashPicHook();
    private static Field MsgRecord_msgtype = null;
    private static Method MsgRecord_getExtInfoFromExtStr = null;
    private static Field fBaseChatItemLayout = null;
    private static Method setTailMessage = null;

    private FlashPicHook() {
        super(new int[]{DexKit.C_FLASH_PIC_HELPER,
                DexKit.C_BASE_PIC_DL_PROC,
                DexKit.C_ITEM_BUILDER_FAC});
    }

    public static boolean isFlashPic(Object msgRecord) {
        try {
            if (MsgRecord_msgtype == null) {
                MsgRecord_msgtype = _MessageRecord().getField("msgtype");
                MsgRecord_msgtype.setAccessible(true);
            }
            if (MsgRecord_getExtInfoFromExtStr == null) {
                MsgRecord_getExtInfoFromExtStr = _MessageRecord()
                        .getMethod("getExtInfoFromExtStr", String.class);
                MsgRecord_getExtInfoFromExtStr.setAccessible(true);
            }
            int msgtype = (int) MsgRecord_msgtype.get(msgRecord);
            return (msgtype == -2000 || msgtype == -2006)
                    && !TextUtils.isEmpty(
                    (String) MsgRecord_getExtInfoFromExtStr.invoke(msgRecord, "commen_flash_pic"));
        } catch (Exception e) {
            INSTANCE.traceError(e);
            return false;
        }
    }

    static String sn_ItemBuilderFactory = null;
    static String sn_BasePicDownloadProcessor = null;

    @Override
    public boolean initOnce() throws Exception {
        Class clz = DexKit.loadClassFromCache(DexKit.C_FLASH_PIC_HELPER);
        Method isFlashPic = null;
        for (Method mi : clz.getDeclaredMethods()) {
            if (mi.getReturnType().equals(boolean.class)
                    && mi.getParameterTypes().length == 1) {
                String name = mi.getName();
                if (name.equals("a") || name.equals("z") || name.equals("W")) {
                    isFlashPic = mi;
                    break;
                }
            }
        }
        HookUtils.hookBeforeIfEnabled(this, isFlashPic, 52, param -> {
            if (sn_BasePicDownloadProcessor == null) {
                sn_BasePicDownloadProcessor = getShortClassName(
                        DexKit.doFindClass(DexKit.C_BASE_PIC_DL_PROC));
            }
            if (sn_ItemBuilderFactory == null) {
                sn_ItemBuilderFactory = getShortClassName(
                        DexKit.doFindClass(DexKit.C_ITEM_BUILDER_FAC));
            }
            if (Reflex.isCallingFromEither(sn_ItemBuilderFactory,
                    sn_BasePicDownloadProcessor, "FlashPicItemBuilder")) {
                param.setResult(false);
            }
        });
        Class tmp;
        Class mBaseBubbleBuilder$ViewHolder = load(
                "com.tencent.mobileqq.activity.aio.BaseBubbleBuilder$ViewHolder");
        if (mBaseBubbleBuilder$ViewHolder == null) {
            tmp = load("com.tencent.mobileqq.activity.aio.BaseBubbleBuilder");
            for (Method mi : tmp.getDeclaredMethods()) {
                if (Modifier.isAbstract(mi.getModifiers())
                        && mi.getParameterTypes().length == 0) {
                    mBaseBubbleBuilder$ViewHolder = mi.getReturnType();
                    break;
                }
            }
        }
        Method m = null;
        for (Method mi : _PicItemBuilder().getDeclaredMethods()) {
            if (mi.getReturnType().equals(View.class) && mi.getParameterTypes().length == 5) {
                m = mi;
                break;
            }
        }
        final Method __tmnp_isF = isFlashPic;
        final Class<?> __tmp_mBaseBubbleBuilder$ViewHolder = mBaseBubbleBuilder$ViewHolder;
        HookUtils.hookAfterIfEnabled(this, m, param -> {
            Object viewHolder = param.args[1];
            if (viewHolder == null) {
                return;
            }
            if (fBaseChatItemLayout == null) {
                // should only have on field with name with given type BaseChatItemLayout
                fBaseChatItemLayout = Reflex.getFirstNSFFieldByType(viewHolder.getClass(),
                        load("com.tencent.mobileqq.activity.aio.BaseChatItemLayout"));
                fBaseChatItemLayout.setAccessible(true);
            }
            if (setTailMessage == null) {
                setTailMessage = XposedHelpers.findMethodExact(
                        load("com.tencent.mobileqq.activity.aio.BaseChatItemLayout"),
                        "setTailMessage", boolean.class, CharSequence.class,
                        View.OnClickListener.class);
                setTailMessage.setAccessible(true);
            }
            if (setTailMessage != null) {
                Object baseChatItemLayout = fBaseChatItemLayout.get(viewHolder);
                setTailMessage.invoke(baseChatItemLayout, isFlashPic(param.args[0]), "闪照", null);
            }
        });
        return true;
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.MESSAGE_CATEGORY;
    }

    @NonNull
    @Override
    public String getName() {
        return "以图片方式打开闪照";
    }
}
