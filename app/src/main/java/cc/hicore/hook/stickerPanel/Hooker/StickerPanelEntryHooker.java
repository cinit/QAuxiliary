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

package cc.hicore.hook.stickerPanel.Hooker;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import cc.hicore.QApp.QAppUtils;
import cc.hicore.ReflectUtil.MRes;
import cc.hicore.ReflectUtil.XField;
import cc.hicore.ReflectUtil.XMethod;
import cc.hicore.Utils.FunConf;
import cc.hicore.Utils.FunProtoData;
import cc.hicore.Utils.XLog;
import cc.hicore.hook.stickerPanel.ICreator;
import cc.hicore.hook.stickerPanel.PanelUtils;
import cc.hicore.message.chat.SessionHooker;
import cc.hicore.message.chat.SessionUtils;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.Reflex;
import com.tencent.qphone.base.remote.FromServiceMsg;
import com.tencent.qphone.base.remote.ToServiceMsg;
import com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService;
import com.tencent.qqnt.kernel.nativeinterface.MsgElement;
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord;
import com.tencent.qqnt.kernel.nativeinterface.PicElement;
import com.xiaoniu.dispatcher.OnMenuBuilder;
import com.xiaoniu.util.ContextUtils;
import de.robv.android.xposed.XC_MethodHook;
import io.github.qauxv.R;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.bridge.AppRuntimeHelper;
import io.github.qauxv.bridge.kernelcompat.KernelMsgServiceCompat;
import io.github.qauxv.bridge.ntapi.MsgServiceHelper;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.ui.CommonContextWrapper;
import io.github.qauxv.util.CustomMenu;
import io.github.qauxv.util.SyncUtils;
import io.github.qauxv.util.dexkit.AbstractQQCustomMenuItem;
import io.github.qauxv.util.dexkit.ChatPanel_InitPanel_QQNT;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import io.github.qauxv.util.dexkit.Guild_Emo_Btn_Create_QQNT;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import kotlin.Unit;
import org.json.JSONObject;

@FunctionHookEntry
@UiItemAgentEntry
public class StickerPanelEntryHooker extends CommonSwitchFunctionHook implements SessionHooker.IAIOParamUpdate, OnMenuBuilder {

    public static final StickerPanelEntryHooker INSTANCE = new StickerPanelEntryHooker();
    public static Object AIOParam;

    private StickerPanelEntryHooker() {
        super(new DexKitTarget[]{
                ChatPanel_InitPanel_QQNT.INSTANCE,
                AbstractQQCustomMenuItem.INSTANCE,
                Guild_Emo_Btn_Create_QQNT.INSTANCE
        });
    }

    public static String rkey_group;
    public static String rkey_private;

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Auxiliary.EXPERIMENTAL_CATEGORY;
    }

    @Override
    protected boolean initOnce() throws Exception {
        HookUtils.hookAfterIfEnabled(this, DexKit.loadMethodFromCache(ChatPanel_InitPanel_QQNT.INSTANCE), param -> {
            View v = null;
            Field[] fs = param.thisObject.getClass().getDeclaredFields();
            for (Field f : fs) {
                if (f.getType().equals(ImageButton.class)) {
                    f.setAccessible(true);
                    if ("emo_btn".equals(MRes.getViewResName((View) f.get(param.thisObject)))) {
                        v = (View) f.get(param.thisObject);
                    }
                }
            }
            if (v != null) {
                v.setOnLongClickListener(v1 -> {
                    ICreator.createPanel(v1.getContext());
                    return true;
                });
            } else {
                XLog.e("Emo_Btn_Hooker", "emo_btn field not found");
            }
        });

        HookUtils.hookAfterIfEnabled(
                this,
                DexKit.loadMethodFromCache(Guild_Emo_Btn_Create_QQNT.INSTANCE),
                param -> {
                    ViewGroup vg = (ViewGroup) param.getResult();
                    for (int i = 0; i < vg.getChildCount(); i++) {
                        View v = vg.getChildAt(i);
                        if (v instanceof ImageView) {
                            v.setOnLongClickListener(v1 -> {
                                ICreator.createPanel(v1.getContext());
                                return true;
                            });
                        }
                    }
                }
        );
        HookUtils.hookAfterIfEnabled(
                this,
                XMethod.clz("com.tencent.qqnt.aio.shortcutbar.PanelIconLinearLayout").ret(ImageView.class).ignoreParam().get(),
                param -> {
                    ImageView imageView = (ImageView) param.getResult();
                    if ("表情".contentEquals(imageView.getContentDescription())) {
                        imageView.setOnLongClickListener(view -> {
                            ICreator.createPanel(view.getContext());
                            return true;
                        });
                    }
                }
        );

        //Hook for change title

        Method sendMsgMethod = XMethod
                .clz("com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService$CppProxy")
                .name("sendMsg")
                .ignoreParam().get();

        HookUtils.hookBeforeIfEnabled(
                this,
                sendMsgMethod,
                param -> {
                    if (isEnabled()) {
                        ArrayList<MsgElement> elements = (ArrayList<MsgElement>) param.args[2];
                        if (FunConf.getBoolean("global", "sticker_panel_set_ch_change_title", false)) {
                            String text = FunConf.getString("global", "sticker_panel_set_ed_change_title", "");
                            if (!TextUtils.isEmpty(text)) {
                                for (MsgElement element : elements) {
                                    if (element.getPicElement() != null) {
                                        PicElement picElement = element.getPicElement();
                                        picElement.setSummary(text);
                                    }
                                }
                            }
                        }
                    }
                }
        );

        //Hook for rkey

        HookUtils.hookBeforeIfEnabled(this, XMethod.clz("mqq.app.msghandle.MsgRespHandler").name("dispatchRespMsg").ignoreParam().get(), param -> {
            ToServiceMsg serviceMsg = XField.obj(param.args[1]).name("toServiceMsg").get();
            FromServiceMsg fromServiceMsg = XField.obj(param.args[1]).name("fromServiceMsg").get();

            if ("OidbSvcTrpcTcp.0x9067_202".equals(fromServiceMsg.getServiceCmd())) {
                FunProtoData data = new FunProtoData();
                data.fromBytes(getUnpPackage(fromServiceMsg.getWupBuffer()));

                JSONObject obj = data.toJSON();
                rkey_group = obj.getJSONObject("4")
                        .getJSONObject("4")
                        .getJSONArray("1")
                        .getJSONObject(0).getString("1");

                rkey_private = obj.getJSONObject("4")
                        .getJSONObject("4")
                        .getJSONArray("1")
                        .getJSONObject(1).getString("1");
            }
        });

        return true;
    }

    private static byte[] getUnpPackage(byte[] b) {
        if (b == null) {
            return null;
        }
        if (b.length < 4) {
            return b;
        }
        if (b[0] == 0) {
            return Arrays.copyOfRange(b, 4, b.length);
        } else {
            return b;
        }
    }

    @NonNull
    @Override
    public String getName() {
        return "表情面板";
    }

    @Override
    public String getDescription() {
        return "长按表情按钮打开，仅支持QQNT";
    }

    @Override
    public boolean isAvailable() {
        return QAppUtils.isQQnt();
    }

    @Override
    public void onAIOParamUpdate(Object AIOParam) {
        StickerPanelEntryHooker.AIOParam = AIOParam;
    }

    @NonNull
    @Override
    public String[] getTargetComponentTypes() {
        return new String[]{
                "com.tencent.mobileqq.aio.msglist.holder.component.pic.AIOPicContentComponent",
                "com.tencent.mobileqq.aio.msglist.holder.component.mix.AIOMixContentComponent",
        };
    }

    @Override
    public void onGetMenuNt(@NonNull Object msg, @NonNull String componentType, @NonNull XC_MethodHook.MethodHookParam param) throws Exception {
        if (!isEnabled()) {
            return;
        }
        //Hook for longClick msgItem
        Object item = CustomMenu.createItemIconNt(msg, "保存面板", R.drawable.ic_item_save_72dp, R.id.item_save_to_panel, () -> {
            try {
                long msgID = (long) Reflex.invokeVirtual(msg, "getMsgId");
                KernelMsgServiceCompat service = MsgServiceHelper.getKernelMsgService(AppRuntimeHelper.getAppRuntime());
                ArrayList<Long> msgIDs = new ArrayList<>();
                msgIDs.add(msgID);
                service.getMsgsByMsgId(SessionUtils.AIOParam2Contact(AIOParam), msgIDs, (result, errMsg, msgList) -> {
                    SyncUtils.runOnUiThread(() -> {
                        for (MsgRecord msgRecord : msgList) {
                            ArrayList<String> md5s = new ArrayList<>();
                            ArrayList<String> urls = new ArrayList<>();

                            for (MsgElement element : msgRecord.getElements()) {
                                if (element.getPicElement() != null) {
                                    PicElement picElement = element.getPicElement();
                                    //md5必须大写才能加载
                                    md5s.add(picElement.getMd5HexStr().toUpperCase());
                                    String originUrl = picElement.getOriginImageUrl();
                                    if (TextUtils.isEmpty(originUrl)) {
                                        urls.add("https://gchat.qpic.cn/gchatpic_new/0/0-0-" + picElement.getMd5HexStr().toUpperCase() + "/0");
                                    } else {
                                        if (originUrl.startsWith("/download")) {
                                            if (originUrl.contains("appid=1406")) {
                                                urls.add("https://multimedia.nt.qq.com.cn" + originUrl + rkey_group);
                                            } else {
                                                urls.add("https://multimedia.nt.qq.com.cn" + originUrl + rkey_private);
                                            }
                                        } else {
                                            urls.add("https://gchat.qpic.cn" + picElement.getOriginImageUrl());
                                        }
                                    }


                                }
                            }
                            if (!md5s.isEmpty()) {
                                if (md5s.size() > 1) {
                                    PanelUtils.PreSaveMultiPicList(urls, md5s, CommonContextWrapper.createAppCompatContext(ContextUtils.getCurrentActivity()));
                                } else {
                                    PanelUtils.PreSavePicToList(urls.get(0), md5s.get(0),
                                            CommonContextWrapper.createAppCompatContext(ContextUtils.getCurrentActivity()));
                                }
                            }
                        }
                    });
                });

            } catch (Exception e) {
                XLog.e("StickerPanelEntryHooker.msgLongClickSaveToLocal", e);
            }
            return Unit.INSTANCE;
        });
        List list = (List) param.getResult();
        List result = new ArrayList<>();
        result.add(0, item);
        result.addAll(list);
        param.setResult(result);
    }
}
