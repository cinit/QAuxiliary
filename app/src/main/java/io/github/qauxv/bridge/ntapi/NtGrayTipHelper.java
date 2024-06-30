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

package io.github.qauxv.bridge.ntapi;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.tencent.qqnt.kernel.nativeinterface.IAddJsonGrayTipMsgCallback;
import com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService;
import io.github.qauxv.bridge.kernelcompat.ContactCompat;
import io.github.qauxv.bridge.kernelcompat.KernelMsgServiceCompat;
import io.github.qauxv.bridge.kernelcompat.KernelObjectHelper;
import java.util.ArrayList;
import java.util.Objects;
import mqq.app.AppRuntime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class NtGrayTipHelper {

    private NtGrayTipHelper() {
    }

    public static final int AIO_AV_C2C_NOTICE = 2021;
    public static final int AIO_AV_GROUP_NOTICE = 2022;

    public static void addLocalJsonGrayTipMsg(@NotNull AppRuntime app, @NotNull ContactCompat contact, @NotNull Object jsonGrayElement, boolean needStore,
            boolean needRecentContact, @Nullable IAddJsonGrayTipMsgCallback callback) throws ReflectiveOperationException, LinkageError, IllegalStateException {
        KernelMsgServiceCompat kmsgSvc = MsgServiceHelper.getKernelMsgService(app);
        if (kmsgSvc == null) {
            throw new IllegalStateException("IKernelMsgService is null");
        }
        kmsgSvc.addLocalJsonGrayTipMsg(contact, jsonGrayElement, needStore, needRecentContact, callback);
    }

    public static Object createLocalJsonElement(long busiId, @NonNull String jsonStr, @NonNull String recentAbstract) {
        try {
            Class.forName("com.tencent.qqnt.kernel.nativeinterface.JsonGrayElement");
            return new com.tencent.qqnt.kernel.nativeinterface.JsonGrayElement(busiId, jsonStr, recentAbstract, false, null);
        } catch (ClassNotFoundException ignored) {
        }
        try {
            Class.forName("com.tencent.qqnt.kernelpublic.nativeinterface.JsonGrayElement");
            return new com.tencent.qqnt.kernelpublic.nativeinterface.JsonGrayElement(busiId, jsonStr, recentAbstract, false, null);
        } catch (ClassNotFoundException ignored) {
        }
        KernelObjectHelper.throwKernelObjectNotSupported("JsonGrayElement");
        return null;
    }

    public static class NtGrayTipJsonBuilder {

        public interface Item {

            JSONObject toJson() throws JSONException;

        }

        public static class TextItem implements Item {

            private final String mText;

            public TextItem(@NonNull String text) {
                Objects.requireNonNull(text);
                mText = text;
            }

            public JSONObject toJson() throws JSONException {
                // {"txt":"$text","type":"nor"}
                JSONObject json = new JSONObject();
                json.put("txt", mText);
                json.put("type", "nor");
                return json;
            }

            @NonNull
            @Override
            public String toString() {
                try {
                    return toJson().toString();
                } catch (JSONException e) {
                    return super.toString();
                }
            }
        }

        public static class UserItem implements Item {

            // after testing, uin is not really required, but better to keep it
            private final String mUin;
            private final String mUid;
            private final String mNick;

            public UserItem(@Nullable String uin, @NonNull String uid, @NonNull String nick) {
                mUin = uin;
                mUid = requireValidUid(uid);
                mNick = Objects.requireNonNull(nick);
            }

            public JSONObject toJson() throws JSONException {
                // {"col":"3","jp":"$uid","nm":"$nick","tp":"0","type":"qq","uid":"$uid","uin":"$uin"}
                JSONObject json = new JSONObject();
                json.put("col", "3");
                json.put("jp", mUid);
                json.put("nm", mNick);
                json.put("tp", "0");
                json.put("type", "qq");
                json.put("uid", mUid);
                if (isValidateUin(mUin)) {
                    json.put("uin", mUin);
                }
                return json;
            }

            @NonNull
            @Override
            public String toString() {
                try {
                    return toJson().toString();
                } catch (JSONException e) {
                    return super.toString();
                }
            }
        }

        public static class MsgRefItem implements Item {

            private final String mText;
            private final long mMsgSeq;

            public MsgRefItem(@NonNull String text, long msgSeq) {
                mText = Objects.requireNonNull(text);
                mMsgSeq = msgSeq;
            }

            public JSONObject toJson() throws JSONException {
                // {"type":"url","txt":"$text","col":"3","local_jp":58,"param":{"seq":$seq}}
                JSONObject json = new JSONObject();
                json.put("type", "url");
                json.put("txt", mText);
                json.put("col", "3");
                json.put("local_jp", 58);
                JSONObject param = new JSONObject();
                param.put("seq", mMsgSeq);
                json.put("param", param);
                return json;
            }

            @NonNull
            @Override
            public String toString() {
                try {
                    return toJson().toString();
                } catch (JSONException e) {
                    return super.toString();
                }
            }
        }


        public NtGrayTipJsonBuilder() {
        }

        private ArrayList<Item> mItems = new ArrayList<>(4);

        public NtGrayTipJsonBuilder appendText(@NonNull String text) {
            mItems.add(new TextItem(text));
            return this;
        }

        public NtGrayTipJsonBuilder append(@NonNull Item item) {
            mItems.add(item);
            return this;
        }

        public JSONObject build() {
            try {
                // {"align":"center","items":[...]}
                JSONObject json = new JSONObject();
                json.put("align", "center");
                JSONArray items = new JSONArray();
                for (Item item : mItems) {
                    items.put(item.toJson());
                }
                json.put("items", items);
                return json;
            } catch (JSONException e) {
                // should not happen
                throw new RuntimeException(e);
            }
        }

    }

    @NonNull
    public static String requireValidUin(String uin) {
        if (TextUtils.isEmpty(uin)) {
            throw new IllegalArgumentException("uin is empty");
        }
        try {
            Long.parseLong(uin);
            return uin;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("uin is not a number, uin=" + uin);
        }
    }

    @NonNull
    public static String requireValidUid(String uid) {
        if (TextUtils.isEmpty(uid)) {
            throw new IllegalArgumentException("uid is empty");
        }
        if (uid.length() != 24 || !uid.startsWith("u_")) {
            throw new IllegalArgumentException("uid is not a valid uid, uid=" + uid);
        }
        return uid;
    }

    public static boolean isValidateUid(String uid) {
        if (TextUtils.isEmpty(uid)) {
            return false;
        }
        return uid.length() == 24 && uid.startsWith("u_");
    }

    public static boolean isValidateUin(String uin) {
        if (TextUtils.isEmpty(uin)) {
            return false;
        }
        try {
            // allow uid such as 9915
            return Long.parseLong(uin) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
