/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
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

package cc.hicore.message.chat;

import androidx.annotation.NonNull;
import cc.hicore.QApp.QAppUtils;
import cc.hicore.ReflectUtil.MField;
import cc.hicore.hook.stickerPanel.Hooker.Emo_Btn_Hooker;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import io.github.qauxv.base.IDynamicHook;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.hook.BaseHookDispatcher;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.dexkit.AIO_Create_QQNT;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import me.ketal.hook.MultiActionHook;

@FunctionHookEntry
public class SessionHooker extends BaseHookDispatcher<SessionHooker.IAIOParamUpdate> {
    public static final SessionHooker INSTANCE = new SessionHooker();
    private SessionHooker() {
        super(new DexKitTarget[]{
                AIO_Create_QQNT.INSTANCE
        });
    }

    private static final SessionHooker.IAIOParamUpdate[] DECORATORS = {
            Emo_Btn_Hooker.INSTANCE,
            MultiActionHook.INSTANCE
    };

    @NonNull
    @Override
    public SessionHooker.IAIOParamUpdate[] getDecorators() {
        return DECORATORS;
    }

    @Override
    protected boolean initOnce() throws Exception {
        XposedBridge.hookMethod(DexKit.loadMethodFromCache(AIO_Create_QQNT.INSTANCE), new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object pie = param.thisObject;
                Object AIOParam = MField.GetFirstField(pie,Initiator.loadClass("com.tencent.aio.data.AIOParam"));
                for (SessionHooker.IAIOParamUpdate decorator : getDecorators()) {
                    decorator.onAIOParamUpdate(AIOParam);
                }
            }
        });
        return true;
    }

    @Override
    public boolean isAvailable() {
        return QAppUtils.isQQnt();
    }

    public interface IAIOParamUpdate extends IDynamicHook {
        void onAIOParamUpdate(Object AIOParam);
    }
}
