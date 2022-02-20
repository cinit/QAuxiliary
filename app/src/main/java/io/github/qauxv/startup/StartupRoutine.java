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
package io.github.qauxv.startup;

import android.app.Application;
import android.content.Context;
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit;
import io.github.qauxv.core.MainHook;
import io.github.qauxv.util.HostInfo;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.Natives;

public class StartupRoutine {

    private StartupRoutine() {
        throw new AssertionError("No instance for you!");
    }

    /**
     * From now on, kotlin, androidx or third party libraries may be accessed without crashing the ART.
     * <p>
     * Kotlin and androidx are dangerous, and should be invoked only after the class loader is ready.
     *
     * @param ctx         Application context for host
     * @param step        Step instance
     * @param lpwReserved null, not used
     * @param bReserved   false, not used
     */
    public static void execPostStartupInit(Context ctx, Object step, String lpwReserved, boolean bReserved) {
        // init all kotlin utils here
        EzXHelperInit.INSTANCE.initZygote(HookEntry.getInitZygoteStartupParam());
        EzXHelperInit.INSTANCE.initHandleLoadPackage(HookEntry.getLoadPackageParam());
        // resource injection is done somewhere else, do not init it here
        EzXHelperInit.INSTANCE.initAppContext(ctx, false, false);
        EzXHelperInit.INSTANCE.setLogTag("QAuxv");
        HostInfo.init((Application) ctx);
        Initiator.init(ctx.getClassLoader());
        try {
            Natives.load(ctx);
        } catch (Exception | LinkageError e3) {
            Log.e(e3);
        }
        MainHook.getInstance().performHook(ctx, step);
    }
}
