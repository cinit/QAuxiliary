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

import static io.github.qauxv.util.Initiator._EmoAddedAuthCallback;
import static io.github.qauxv.util.Initiator._FavEmoRoamingHandler;
import static io.github.qauxv.util.QQVersion.QQ_8_2_0;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HostInfo;
import cc.ioctl.util.Reflex;
import io.github.qauxv.util.xpcompat.XC_MethodHook;
import io.github.qauxv.util.xpcompat.XposedHelpers;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.dexkit.CFavEmoConst;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import java.lang.reflect.Method;
import java.util.List;

@FunctionHookEntry
@UiItemAgentEntry
public class FavMoreEmo extends CommonSwitchFunctionHook {

    public static final FavMoreEmo INSTANCE = new FavMoreEmo();

    private FavMoreEmo() {
        super(new DexKitTarget[]{ CFavEmoConst.INSTANCE });
    }

    @Override
    public boolean initOnce() throws Exception {
        final Class<?> mEmoAddedAuthCallback = _EmoAddedAuthCallback();
        final Class<?> mFavEmoRoamingHandler = _FavEmoRoamingHandler();
        if (mEmoAddedAuthCallback == null) {
            if (mFavEmoRoamingHandler == null) {
                setEmoNum();
            } else {
                XposedHelpers.findAndHookMethod(mFavEmoRoamingHandler, "a", List.class,
                        List.class,
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param)
                                    throws Throwable {
                                try {
                                    setEmoNum();
                                } catch (Throwable e) {
                                    traceError(e);
                                    throw e;
                                }
                            }
                        });
            }
        } else {
            Class<?> mUpCallBack$SendResult = null;
            for (Method m : mEmoAddedAuthCallback.getDeclaredMethods()) {
                if (m.getName().equals("b") && m.getReturnType().equals(void.class)
                        && m.getParameterTypes().length == 1) {
                    mUpCallBack$SendResult = m.getParameterTypes()[0];
                    break;
                }
            }
            XposedHelpers.findAndHookMethod(mEmoAddedAuthCallback, "b", mUpCallBack$SendResult,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            try {
                                Object msg = param.args[0];
                                Reflex.setInstanceObject(msg, "a", int.class, 0);
                            } catch (Throwable e) {
                                traceError(e);
                                throw e;
                            }
                        }
                    });
            XposedHelpers.findAndHookMethod(mFavEmoRoamingHandler, "a", List.class, List.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            try {
                                setEmoNum();
                            } catch (Throwable e) {
                                traceError(e);
                                throw e;
                            }
                        }
                    });
        }
        return true;
    }

    private void setEmoNum() throws NoSuchFieldException {
        Class<?> mFavEmoConstant = DexKit.requireClassFromCache(CFavEmoConst.INSTANCE);
        Reflex.setStaticObject(mFavEmoConstant, "a", 800);
        Reflex.setStaticObject(mFavEmoConstant, "b", 800);
    }

    @Override
    public boolean isAvailable() {
        return !HostInfo.isTim() && HostInfo.getVersionCode() < QQ_8_2_0;
    }

    @NonNull
    @Override
    public String getName() {
        return "收藏更多表情";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "[暂不支持>=8.2.0]保存在本地";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.CHAT_CATEGORY;
    }
}
