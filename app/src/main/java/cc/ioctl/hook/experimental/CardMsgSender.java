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
package cc.ioctl.hook.experimental;

import static cc.hicore.message.bridge.Nt_kernel_bridge.send_msg;
import static io.github.qauxv.router.dispacher.InputButtonHookDispatcher.AIOParam;

import android.content.Context;
import android.os.Parcelable;
import android.view.View;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.hicore.QApp.QAppUtils;
import cc.hicore.message.chat.SessionUtils;
import com.tencent.qqnt.kernel.nativeinterface.ArkElement;
import com.tencent.qqnt.kernel.nativeinterface.MsgElement;
import io.github.qauxv.base.IDynamicHook;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.bridge.AppRuntimeHelper;
import io.github.qauxv.bridge.kernelcompat.ContactCompat;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.remote.TransactionHelper;
import io.github.qauxv.router.decorator.BaseSwitchFunctionDecorator;
import io.github.qauxv.router.decorator.IInputButtonDecorator;
import io.github.qauxv.router.dispacher.InputButtonHookDispatcher;
import io.github.qauxv.util.SyncUtils;
import io.github.qauxv.util.Toasts;
import io.github.qauxv.util.dexkit.CArkAppItemBubbleBuilder;
import io.github.qauxv.util.dexkit.CFaceDe;
import io.github.qauxv.util.dexkit.CTestStructMsg;
import io.github.qauxv.util.dexkit.DexKitTarget;
import io.github.qauxv.util.dexkit.NBaseChatPie_init;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import me.singleneuron.data.CardMsgCheckResult;
import me.singleneuron.util.KotlinUtilsKt;
import mqq.app.AppRuntime;
import org.json.JSONException;
import org.json.JSONObject;

@UiItemAgentEntry
@FunctionHookEntry
public class CardMsgSender extends BaseSwitchFunctionDecorator implements IInputButtonDecorator {

    public static final CardMsgSender INSTANCE = new CardMsgSender();

    private CardMsgSender() {
        super("qn_send_card_msg", false, new DexKitTarget[]{
                CArkAppItemBubbleBuilder.INSTANCE,
                CFaceDe.INSTANCE,
                NBaseChatPie_init.INSTANCE,
                CTestStructMsg.INSTANCE
        });
    }

    @NonNull
    @Override
    protected IDynamicHook getDispatcher() {
        return InputButtonHookDispatcher.INSTANCE;
    }

    @NonNull
    @Override
    public String getName() {
        return "发送卡片消息";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "小心使用，可能会导致自己被封号。"
                + "注意 ‘发送卡片消息’ 和 ‘复制卡片消息’ 是两个不同的功能，两者无关。"
                + "为防止本功能被滥用，在您使用代码发送卡片消息时，本软件会*不匿名地*向服务器报告您的 QQ 号以及发送的卡片消息，"
                + "如果您介意，请关闭 ‘发送卡片消息’ 功能。";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.EXPERIMENTAL_CATEGORY;
    }

    @Override
    public boolean onFunBtnLongClick(@NonNull String text,
            @NonNull Parcelable session,
            @NonNull EditText input,
            @NonNull View sendBtn,
            @NonNull Context ctx1,
            @NonNull AppRuntime qqApp) throws Exception {
        if (!isEnabled()) {
            return false;
        }
        if ((text.contains("<?xml") || text.contains("{\""))) {
            long uin = AppRuntimeHelper.getLongAccountUin();
            if (uin < 10000) {
                Toasts.error(ctx1, "Invalid account uin");
                return false;
            }
            SyncUtils.async(() -> {
                if (text.contains("<?xml")) {
                    try {
                        String errorMsg = TransactionHelper.postCardMsg(uin, text);
                        if (errorMsg != null) {
                            Toasts.error(ctx1, errorMsg);
                            return;
                        }
                        if (CardMsgSender.ntSendCardMsg(qqApp, session, text)) {
                            SyncUtils.runOnUiThread(() -> input.setText(""));
                        } else {
                            Toasts.error(ctx1, "XML语法错误(代码有误)");
                        }
                    } catch (Throwable e) {
                        if (e instanceof InvocationTargetException) {
                            e = e.getCause();
                        }
                        traceError(e);
                        Toasts.error(ctx1, e.toString().replace("java.lang.", ""));
                    }
                } else if (text.contains("{\"")) {
                    try {
                        String errorMsg = TransactionHelper.postCardMsg(uin, text);
                        if (errorMsg != null) {
                            Toasts.error(ctx1, errorMsg);
                            return;
                        }

                        sendCard(text, sendBtn, input, ctx1, qqApp, session);
                        // Object arkMsg = load("com.tencent.mobileqq.data.ArkAppMessage").newInstance();

                    } catch (Throwable e) {
                        traceError(e);
                        Toasts.error(ctx1, e.toString().replace("java.lang.", ""));
                    }
                }
            });
            return true;
        }
        return false;
    }

    @NonNull
    private static MsgElement getArkMsgElement(@NonNull String text) {
        MsgElement msgElement = new MsgElement();
        ArkElement arkElement = new ArkElement(text,null,null);
        msgElement.setArkElement(arkElement);
        msgElement.setElementType(10);
        return msgElement;
    }

    private void sendCard(String text, View sendBtn, EditText input, Context ctx1, AppRuntime qqApp, Parcelable session) throws Exception {
        if (QAppUtils.isQQnt()){
            try {
                new JSONObject(text);
                CardMsgCheckResult check = KotlinUtilsKt.checkCardMsg(text);
                if (check.getAccept()) {
                    ArrayList<MsgElement> elem = new ArrayList<>();
                    MsgElement msgElement = getArkMsgElement(text);
                    elem.add(msgElement);
                    ContactCompat contact = SessionUtils.AIOParam2Contact(AIOParam);
                    send_msg(contact, elem);
                    SyncUtils.runOnUiThread(() -> {
                        input.setText("");
                        sendBtn.setClickable(false);
                    });
                } else {
                    Toasts.error(ctx1, check.getReason());
                }

            } catch (JSONException e) {
                Toasts.error(ctx1, "JSON语法错误(代码有误)");
            }

            return;
        }

        if (CardMsgSender.ntSendCardMsg(qqApp, session, text)) {
            SyncUtils.runOnUiThread(() -> {
                input.setText("");
                sendBtn.setClickable(false);
            });
        } else {
            Toasts.error(ctx1, "JSON语法错误(代码有误)");
        }

    }



    @SuppressWarnings("JavaJniMissingFunction")
    static native boolean ntSendCardMsg(AppRuntime rt, Parcelable session, String msg) throws Exception;

}
