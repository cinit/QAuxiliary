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

package cc.hicore.Utils;

import io.github.qauxv.util.Log;

public class XLog {
    public static void e(String TAG,Throwable msg){
        Log.e("[QAuxv]"+"("+TAG+")"+Log.getStackTraceString(msg));
    }
    public static void e(String TAG,String TAG2,Throwable msg){
        e(TAG+"."+TAG2,msg);
    }
    public static void e(String TAG,String msg){
        Log.e("[QAuxv]"+"("+TAG+")"+msg);
    }
    public static void d(String TAG,String msg){
        Log.d("[QAuxv]"+"("+TAG+")"+msg);
    }
}
