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

import androidx.annotation.Nullable;
import cc.ioctl.hook.notification.MessageInterception;
import com.google.common.collect.Lists;
import io.github.qauxv.base.ITraceableDynamicHook;
import io.github.qauxv.base.RuntimeErrorTracer;
import java.util.List;
import me.singleneuron.data.MsgRecordData;

public interface MessageReceiver extends ITraceableDynamicHook {

    /**
     * 当拦截到消息时会调用此方法
     *
     * @param data 拦截到的数据
     * @return 如果返回为真将不会向下传递拦截到的消息
     */
    boolean onReceive(MsgRecordData data);

    @Nullable
    @Override
    default List<RuntimeErrorTracer> getRuntimeErrorDependentComponents() {
        return Lists.asList(MessageInterception.INSTANCE, RuntimeErrorTracer.EMPTY_ARRAY);
    }

}
