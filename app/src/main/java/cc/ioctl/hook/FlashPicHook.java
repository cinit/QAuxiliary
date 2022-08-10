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
import java.util.Objects;

/**
 * Peak frequency: ~40 invocations per second
 */
@UiItemAgentEntry
@FunctionHookEntry
public class FlashPicHook extends CommonSwitchFunctionHook {

    public static final FlashPicHook INSTANCE = new FlashPicHook();
    private Field MsgRecord_msgtype = null;
    private Method MsgRecord_getExtInfoFromExtStr = null;
    private Field fBaseChatItemLayout = null;
    private Method setTailMessage = null;

    private FlashPicHook() {
        super(new int[]{DexKit.C_FLASH_PIC_HELPER,
                DexKit.C_BASE_PIC_DL_PROC,
                DexKit.C_ItemBuilderFactory});
    }

    public boolean isFlashPic(Object msgRecord) {
        try {
            int msgtype = MsgRecord_msgtype.getInt(msgRecord);
            return (msgtype == -2000 || msgtype == -2006)
                    && !TextUtils.isEmpty(
                    (String) MsgRecord_getExtInfoFromExtStr.invoke(msgRecord, "commen_flash_pic"));
        } catch (Exception e) {
            INSTANCE.traceError(e);
            return false;
        }
    }

    private String sn_ItemBuilderFactory = null;
    private String sn_BasePicDownloadProcessor = null;
    private String[] snarray_CheckStackClasses = null;

    @Override
    public boolean initOnce() throws Exception {
        Class<?> clz = DexKit.loadClassFromCache(DexKit.C_FLASH_PIC_HELPER);
        Objects.requireNonNull(clz, "DexKit.C_FLASH_PIC_HELPER");
        Method isFlashPic = null;
        for (Method mi : clz.getDeclaredMethods()) {
            if (mi.getReturnType().equals(boolean.class) && mi.getParameterTypes().length == 1) {
                String name = mi.getName();
                if (name.equals("c") || name.equals("a") || name.equals("z") || name.equals("W")) {
                    isFlashPic = mi;
                    break;
                }
            }
        }
        setTailMessage = XposedHelpers.findMethodExact(
                load("com.tencent.mobileqq.activity.aio.BaseChatItemLayout"),
                "setTailMessage", boolean.class, CharSequence.class,
                View.OnClickListener.class);
        Objects.requireNonNull(setTailMessage, "setTailMessage not found");
        setTailMessage.setAccessible(true);
        Class<?> kItemBuilderFactory = DexKit.loadClassFromCache(DexKit.C_ItemBuilderFactory);
        Objects.requireNonNull(kItemBuilderFactory, "DexKit.C_ItemBuilderFactory");
        sn_ItemBuilderFactory = kItemBuilderFactory.getName();
        Objects.requireNonNull(sn_ItemBuilderFactory, "sn_ItemBuilderFactory not found");
        sn_BasePicDownloadProcessor =
                Objects.requireNonNull(DexKit.loadClassFromCache(DexKit.C_BASE_PIC_DL_PROC), "DexKit.C_BASE_PIC_DL_PROC").getName();
        Objects.requireNonNull(sn_BasePicDownloadProcessor, "sn_BasePicDownloadProcessor not found");
        snarray_CheckStackClasses = new String[]{
                Objects.requireNonNull(sn_ItemBuilderFactory, "sn_ItemBuilderFactory not found"),
                Objects.requireNonNull(sn_BasePicDownloadProcessor, "sn_BasePicDownloadProcessor not found"),
                "FlashPicItemBuilder"
        };
        MsgRecord_msgtype = _MessageRecord().getField("msgtype");
        Objects.requireNonNull(MsgRecord_msgtype, "MsgRecord_msgtype not found");
        MsgRecord_msgtype.setAccessible(true);
        MsgRecord_getExtInfoFromExtStr = _MessageRecord()
                .getMethod("getExtInfoFromExtStr", String.class);
        Objects.requireNonNull(MsgRecord_getExtInfoFromExtStr, "MsgRecord_getExtInfoFromExtStr not found");
        MsgRecord_getExtInfoFromExtStr.setAccessible(true);
        HookUtils.hookBeforeIfEnabled(this, isFlashPic, 52, param -> {
            // TODO 2022-03-10 find a better way instead of checking the stack to improve performance
            if (checkIsCallingFromClass(snarray_CheckStackClasses, 4, 10)) {
                // possible caller: (wrong)
                // getItemViewType
                // RecentBaseData.buildMessageBody
                // EmoticonFromGroupManager
                // com.tencent.mobileqq.activity.aio.core.msglist.a.a.b
                // com.tencent.mobileqq.transfile.ChatImageDownloader.decodeByGif
                param.setResult(false);
            }
        });
        Method m = null;
        for (Method mi : _PicItemBuilder().getDeclaredMethods()) {
            if (mi.getReturnType().equals(View.class) && mi.getParameterTypes().length == 5) {
                m = mi;
                break;
            }
        }
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
            Object baseChatItemLayout = fBaseChatItemLayout.get(viewHolder);
            setTailMessage.invoke(baseChatItemLayout, isFlashPic(param.args[0]), "闪照", null);
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

    private static boolean checkIsCallingFromClass(@NonNull String[] className, int start, int maxDepth) {
        int end;
        if (maxDepth < 0) {
            end = Integer.MAX_VALUE;
        } else {
            end = maxDepth + start;
        }
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        end = Math.min(end, stackTrace.length);
        for (int i = start; i < end; i++) {
            String cn = stackTrace[i].getClassName();
            for (String name : className) {
                if (cn.contains(name)) {
                    return true;
                }
            }
        }
        return false;
    }
}
