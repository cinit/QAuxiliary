/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the qwq233 Universal License
 * as published on https://github.com/qwq233/license; either
 * version 2 of the License, or any later version and our EULA as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the qwq233 Universal License for more details.
 *
 * See
 * <https://github.com/qwq233/license>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package cc.hicore.QApp;

import cc.hicore.ReflectUtil.XClass;
import cc.hicore.ReflectUtil.XMethod;
import io.github.qauxv.util.Initiator;

public class QAppUtils {
    public static long getServiceTime(){
        try {
            return XMethod.clz("com.tencent.mobileqq.msf.core.NetConnInfoCenter").name("getServerTimeMillis").ret(long.class).invoke();
        } catch (Exception e) {
            return 0;
        }
    }
    public static String UserUinToPeerID(String UserUin){
        try {
            Object convertHelper = XClass.newInstance(Initiator.loadClass("com.tencent.qqnt.kernel.api.impl.UixConvertAdapterApiImpl"));
            return XMethod.obj(convertHelper).name("getUidFromUin").ret(String.class).param(long.class).invoke(Long.parseLong(UserUin));
        }catch (Exception e){
            return "";
        }
    }
    public static boolean isQQnt(){
        try {
            return Initiator.load("com.tencent.qqnt.base.BaseActivity") != null;
        }catch (Exception e){
            return false;
        }

    }
    public static String getCurrentUin(){
        try {
            Object AppRuntime = getAppRuntime();
            return XMethod.obj(AppRuntime).name("getCurrentAccountUin").ret(String.class).invoke();
        } catch (Exception e) {
            return "";
        }
    }
    public static Object getAppRuntime() throws Exception {
        Object sApplication = XMethod.clz("com.tencent.common.app.BaseApplicationImpl").name("getApplication").ret(Initiator.load("com.tencent.common.app.BaseApplicationImpl")).invoke();
        return XMethod.obj(sApplication).name("getRuntime").ret(Initiator.loadClass("mqq.app.AppRuntime")).invoke();
    }
}
