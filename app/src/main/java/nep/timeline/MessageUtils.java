/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2025 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is an opensource software: you can redistribute it
 * and/or modify it under the terms of the General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the General Public License for more details.
 *
 * You should have received a copy of the General Public License
 * along with this software.
 * If not, see
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package nep.timeline;

import android.media.MediaMetadataRetriever;
import androidx.annotation.NonNull;
import io.github.qauxv.util.HostInfo;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.QQVersion;
import io.github.qauxv.util.data.ContactDescriptor;
import io.github.qauxv.util.xpcompat.XposedBridge;
import io.github.qauxv.util.xpcompat.XposedHelpers;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class MessageUtils {
    private static boolean kernelPublic = false;

    private static long getDuration(@NonNull String pttPath) {
        Objects.requireNonNull(pttPath, "pttPath == null");
        long duration = 0;
        try (MediaMetadataRetriever mmr = new MediaMetadataRetriever()) {
            mmr.setDataSource(pttPath);
            String time = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (time != null)
                duration = Long.parseLong(time);
        } catch (Exception ignored) {
        }
        return duration;
    }

    public static Object api(Class<?> clazz) throws ClassNotFoundException {
        Class<?> route = Initiator.loadClass("com.tencent.mobileqq.qroute.QRoute");
        return XposedHelpers.callStaticMethod(route, "api", new Class[] { Class.class }, clazz);
    }

    public static boolean sendVoice(@NonNull String pttPath, @NonNull ContactDescriptor descriptor) {
        if (!HostInfo.requireMinQQVersion(QQVersion.QQ_9_0_8))
            return false;

        try {
            Object msgUtilApi = api(Initiator.loadClass("com.tencent.qqnt.msg.api.IMsgUtilApi"));
            Object msgService = api(Initiator.loadClass("com.tencent.qqnt.msg.api.IMsgService"));

            Object uinAndUidApi = api(Initiator.loadClass("com.tencent.relation.common.api.IRelationNTUinAndUidApi"));
            int type = descriptor.uinType + 1;
            String uin = descriptor.uin;
            String uid = (type != 2 && type != 4 && uin.chars().allMatch(Character::isDigit)) ? (String) XposedHelpers.callMethod(uinAndUidApi, "getUidFromUin", uin) : uin;

            Class<?> contactClass;
            if (kernelPublic) {
                contactClass = Initiator.loadClass("com.tencent.qqnt.kernelpublic.nativeinterface.Contact");
            } else {
                try {
                    contactClass = Initiator.loadClass("com.tencent.qqnt.kernel.nativeinterface.Contact");
                } catch (ClassNotFoundException ignored) {
                    contactClass = Initiator.loadClass("com.tencent.qqnt.kernelpublic.nativeinterface.Contact");
                    kernelPublic = true;
                }
            }

            Object contact = XposedHelpers.newInstance(contactClass, type, uid, "");

            Object callbackProxy = Proxy.newProxyInstance(Initiator.getHostClassLoader(), new Class[] { Initiator.loadClass("com.tencent.qqnt.kernel.nativeinterface.IOperateCallback") }, (proxy, method, methodArgs) -> null);
            ArrayList<Object> arrayList = new ArrayList<>();
            arrayList.add(XposedHelpers.callMethod(msgUtilApi, "createPttElement", pttPath, (int) getDuration(pttPath), new ArrayList<>(Arrays.asList(new Byte[] { 28, 26, 43, 29, 31, 61, 34, 49, 51, 56, 52, 74, 41, 62, 66, 46, 25, 57, 51, 70, 33, 45, 39, 27, 68, 58, 46, 59, 59, 63 }))));

            XposedHelpers.callMethod(msgService, "sendMsg", contact, arrayList, callbackProxy);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodError throwable) {
            return false;
        } catch (Throwable throwable) {
            Log.e(throwable.toString(), throwable);
            XposedBridge.log(throwable);
            return false;
        }
    }
}
