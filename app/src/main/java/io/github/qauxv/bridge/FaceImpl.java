/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by ferredoxin.
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
package io.github.qauxv.bridge;

import static io.github.qauxv.util.Initiator.load;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import cc.ioctl.util.Reflex;
import io.github.qauxv.ui.ResUtils;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.data.ContactDescriptor;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;

public class FaceImpl implements InvocationHandler {

    public static final int TYPE_USER = 1;
    public static final int TYPE_TROOP = 4;
    static private WeakReference<FaceImpl> self;
    private static Class class_FaceDecoder;
    private static Class clz_DecodeTaskCompletionListener;
    private final HashMap<String, Bitmap> cachedUserFace;
    private final HashMap<String, Bitmap> cachedTroopFace;
    private final HashMap<String, WeakReference<ImageView>> registeredView;
    private final Object mFaceDecoder;

    private FaceImpl() throws Throwable {
        Object qqAppInterface = AppRuntimeHelper.getAppRuntime();
        class_FaceDecoder = load("com/tencent/mobileqq/util/FaceDecoder");
        if (class_FaceDecoder == null) {
            class_FaceDecoder = load("com/tencent/mobileqq/app/face/FaceDecoder");
        }
        if (class_FaceDecoder == null) {
            Class cl_rxMsg = load(
                "com/tencent/mobileqq/receipt/ReceiptMessageReadMemberListFragment");
            Field[] fs = cl_rxMsg.getDeclaredFields();
            for (Field f : fs) {
                if (f.getType().equals(View.class)) {
                    continue;
                }
                if (f.getType().equals(Initiator._QQAppInterface())) {
                    continue;
                }
                class_FaceDecoder = f.getType();
            }
        }
        mFaceDecoder = class_FaceDecoder.getConstructor(load("com/tencent/common/app/AppInterface"))
            .newInstance(qqAppInterface);
        Reflex.invokeVirtualAny(mFaceDecoder, createListener(), clz_DecodeTaskCompletionListener);
        cachedUserFace = new HashMap<>();
        cachedTroopFace = new HashMap<>();
        registeredView = new HashMap<>();
    }

    public static FaceImpl getInstance() throws Throwable {
        FaceImpl ret = null;
        if (self != null) {
            ret = self.get();
        }
        if (ret == null) {
            ret = new FaceImpl();
            self = new WeakReference(ret);
        }
        return ret;
    }

    private Object createListener() {
        clz_DecodeTaskCompletionListener = load("com/tencent/mobileqq/avatar/listener" +
            "/DecodeTaskCompletionListener");
        if (clz_DecodeTaskCompletionListener == null) {
            clz_DecodeTaskCompletionListener = load("com/tencent/mobileqq/util" +
                "/FaceDecoder$DecodeTaskCompletionListener");
        }
        if (clz_DecodeTaskCompletionListener == null) {
            clz_DecodeTaskCompletionListener = load("com/tencent/mobileqq/app/face" +
                "/FaceDecoder$DecodeTaskCompletionListener");
        }
        if (clz_DecodeTaskCompletionListener == null) {
            Class[] argt;
            Method[] ms = class_FaceDecoder.getDeclaredMethods();
            for (Method m : ms) {
                if (!m.getReturnType().equals(void.class)) {
                    continue;
                }
                argt = m.getParameterTypes();
                if (argt.length != 1) {
                    continue;
                }
                if (argt[0].equals(load("com/tencent/common/app/AppInterface"))) {
                    continue;
                }
                clz_DecodeTaskCompletionListener = argt[0];
            }
        }
        return Proxy.newProxyInstance(clz_DecodeTaskCompletionListener.getClassLoader(),
            new Class[]{clz_DecodeTaskCompletionListener}, this);
    }

    @Override
    public Object invoke(Object obj, Method method, Object[] args) throws Throwable {
        Class[] argt = method.getParameterTypes();
        if (argt.length != 4) {
            return null;
        }
        if (argt[0].equals(int.class) && argt[1].equals(int.class) && argt[2].equals(String.class)
            && argt[3].equals(Bitmap.class)) {
            onDecodeTaskCompleted((int) args[0], (int) args[1], (String) args[2], (Bitmap) args[3]);
        }
        return null;
    }

    public void onDecodeTaskCompleted(int code, int type, String uin, Bitmap bitmap) {
        if (bitmap != null) {
            if (type == TYPE_USER) {
                cachedUserFace.put(uin, bitmap);
            }
            if (type == TYPE_TROOP) {
                cachedTroopFace.put(uin, bitmap);
            }
            WeakReference<ImageView> ref;
            if ((ref = registeredView.remove(type + " " + uin)) != null) {
                ImageView v = ref.get();
                if (v != null) {
                    ((Activity) v.getContext()).runOnUiThread(() -> v.setImageBitmap(bitmap));
                }
            }
        }
    }

    @Nullable
    public Bitmap getBitmapFromCache(int type, String uin) {
        if (type == TYPE_TROOP) {
            return cachedTroopFace.get(uin);
        }
        if (type == TYPE_USER) {
            return cachedUserFace.get(uin);
        }
        return null;
    }

    public boolean requestDecodeFace(int type, String uin) {
        try {
            return (boolean) Reflex.invokeVirtualAny(mFaceDecoder, uin, type, true, (byte) 0,
                String.class, int.class, boolean.class, byte.class, boolean.class);
        } catch (Exception e) {
            Log.e(e);
            return false;
        }
    }

    public boolean registerView(int type, String uin, ImageView v) {
        boolean ret;
        if (ret = requestDecodeFace(type, uin)) {
            registeredView.put(type + " " + uin, new WeakReference<>(v));
        }
        return ret;
    }

    public boolean setImageOrRegister(ContactDescriptor cd, ImageView imgview) {
        return setImageOrRegister(cd.uinType == 1 ? TYPE_TROOP : TYPE_USER, cd.uin, imgview);
    }

    public boolean setImageOrRegister(int type, String uin, ImageView imgview) {
        Bitmap bm = getBitmapFromCache(type, uin);
        if (bm == null) {
            imgview
                .setImageDrawable(ResUtils.loadDrawableFromAsset("face.png", imgview.getContext()));
            return registerView(type, uin, imgview);
        } else {
            imgview.setImageBitmap(bm);
            return true;
        }
    }
}
