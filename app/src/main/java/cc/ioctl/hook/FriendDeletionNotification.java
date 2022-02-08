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
import androidx.annotation.Nullable;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import java.util.List;

@FunctionHookEntry
@UiItemAgentEntry
public class FriendDeletionNotification extends CommonSwitchFunctionHook {

    public static final FriendDeletionNotification INSTANCE = new FriendDeletionNotification();

    private FriendDeletionNotification() {
        super(true);
    }

    @Override
    protected boolean initOnce() throws Exception {
        return DeletionObserver.INSTANCE.initialize();
    }

    @NonNull
    @Override
    public String getName() {
        return "被删好友检测通知";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "不可以做舔狗";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.FRIEND_CATEGORY;
    }

    @NonNull
    @Override
    public List<Throwable> getRuntimeErrors() {
        return DeletionObserver.INSTANCE.getRuntimeErrors();
    }
}
