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

import cc.hicore.ReflectUtil.MClass;
import cc.hicore.ReflectUtil.MMethod;
import io.github.qauxv.util.Initiator;

public class QAppUtils {
    public static long getServiceTime(){
        try {
            return MMethod.CallStaticMethod(MClass.loadClass("com.tencent.mobileqq.msf.core.NetConnInfoCenter"),"getServerTimeMillis",long.class);
        } catch (Exception e) {
            return 0;
        }
    }
    public static String UserUinToPeerID(String UserUin){
        try {
            Object convertHelper = MClass.NewInstance(MClass.loadClass("com.tencent.qqnt.kernel.api.impl.UixConvertAdapterApiImpl"));
            return MMethod.CallMethod(convertHelper,"getUidFromUin",String.class,new Class[]{long.class},Long.parseLong(UserUin));
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
            return MMethod.CallMethodNoParam(AppRuntime, "getCurrentAccountUin", String.class);
        } catch (Exception e) {
            return "";
        }
    }
    public static Object getAppRuntime() throws Exception {
        Object sApplication = MMethod.CallStaticMethod(MClass.loadClass("com.tencent.common.app.BaseApplicationImpl"),
                "getApplication", MClass.loadClass("com.tencent.common.app.BaseApplicationImpl"));

        return MMethod.CallMethodNoParam(sApplication, "getRuntime", MClass.loadClass("mqq.app.AppRuntime"));
    }
}
