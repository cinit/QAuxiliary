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

package cc.hicore.QApp;

import cc.hicore.Env;
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
