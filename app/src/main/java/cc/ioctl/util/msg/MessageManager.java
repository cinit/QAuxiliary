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
package cc.ioctl.util.msg;

import androidx.annotation.NonNull;
import cc.ioctl.hook.notification.MessageInterception;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import me.singleneuron.data.MsgRecordData;

public class MessageManager {

    private static final Map<Long, Long> MSG = new ConcurrentHashMap<>();

    public static Class<?> intType = int.class;
    public static Class<?> booleanType = boolean.class;

    /**
     * 通过这里广播拦截的消息
     *
     * @param data 传入的消息
     */
    public static void call(@NonNull MsgRecordData data) {
        long uid = data.getMsgUid();
        if (MSG.containsKey(uid)) {
            return;
        }

        // 以上是为了防止消息重复广播
        for (MessageReceiver receiver : MessageInterception.INSTANCE.getDecorators()) {
            if (receiver.onReceive(data)) {
                // 返回true退出遍历
                break;
            }
        }
        MSG.put(uid, System.currentTimeMillis());
        for (Long msgUid : MSG.keySet()) {
            Long time = MSG.get(msgUid);
            if (System.currentTimeMillis() - time > 2000L) {
                MSG.remove(msgUid);
            }
        }
    }
}
