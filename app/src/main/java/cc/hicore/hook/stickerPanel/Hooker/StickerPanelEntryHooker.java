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

package cc.hicore.hook.stickerPanel.Hooker;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import cc.hicore.QApp.QAppUtils;
import cc.hicore.ReflectUtil.MMethod;
import cc.hicore.ReflectUtil.MRes;
import cc.hicore.Utils.XLog;
import cc.hicore.hook.stickerPanel.ICreator;
import cc.hicore.hook.stickerPanel.PanelUtils;
import cc.hicore.message.chat.SessionHooker;
import cc.hicore.message.chat.SessionUtils;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.Reflex;
import com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService;
import com.tencent.qqnt.kernel.nativeinterface.MsgElement;
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord;
import com.tencent.qqnt.kernel.nativeinterface.PicElement;
import com.xiaoniu.util.ContextUtils;
import io.github.qauxv.R;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.bridge.AppRuntimeHelper;
import io.github.qauxv.bridge.ntapi.MsgServiceHelper;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.ui.CommonContextWrapper;
import io.github.qauxv.util.CustomMenu;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.SyncUtils;
import io.github.qauxv.util.dexkit.AbstractQQCustomMenuItem;
import io.github.qauxv.util.dexkit.ChatPanel_InitPanel_QQNT;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import io.github.qauxv.util.dexkit.Guild_Emo_Btn_Create_QQNT;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import kotlin.Unit;

@FunctionHookEntry
@UiItemAgentEntry
public class StickerPanelEntryHooker extends CommonSwitchFunctionHook implements SessionHooker.IAIOParamUpdate {
    public static final StickerPanelEntryHooker INSTANCE = new StickerPanelEntryHooker();
    public static Object AIOParam;
    private StickerPanelEntryHooker() {
        super(new DexKitTarget[]{
                ChatPanel_InitPanel_QQNT.INSTANCE,
                AbstractQQCustomMenuItem.INSTANCE,
                Guild_Emo_Btn_Create_QQNT.INSTANCE
        });
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Auxiliary.EXPERIMENTAL_CATEGORY;
    }

    @Override
    protected boolean initOnce() throws Exception {
        HookUtils.hookAfterIfEnabled(this, DexKit.loadMethodFromCache(ChatPanel_InitPanel_QQNT.INSTANCE),param -> {
            View v = null;
            Field[] fs = param.thisObject.getClass().getDeclaredFields();
            for (Field f : fs){
                if (f.getType().equals(ImageButton.class)){
                    f.setAccessible(true);
                    if ("emo_btn".equals(MRes.getViewResName((View) f.get(param.thisObject)))){
                        v = (View) f.get(param.thisObject);
                    }
                }
            }
            if (v != null){
                v.setOnLongClickListener(v1 -> {
                    ICreator.createPanel(v1.getContext());
                    return true;
                });
            }else {
                XLog.e("Emo_Btn_Hooker","emo_btn field not found");
            }
        });

        HookUtils.hookAfterIfEnabled(this,DexKit.loadMethodFromCache(Guild_Emo_Btn_Create_QQNT.INSTANCE),param -> {
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
        });

        HookUtils.hookAfterIfEnabled(this, MMethod.FindMethod(Initiator.loadClass("com.tencent.qqnt.aio.shortcutbar.PanelIconLinearLayout"),
                null,
                ImageView.class,
                new Class[]{Initiator.load("com.tencent.qqnt.aio.shortcutbar.a")}),
                param -> {
                    ImageView imageView = (ImageView) param.getResult();
                    if ("表情".contentEquals(imageView.getContentDescription())){
                        imageView.setOnLongClickListener(view -> {
                            ICreator.createPanel(view.getContext());
                            return true;
                        });
                    }
                });

        //Hook for longClick msgItem
        {
            Class msgClass = Initiator.loadClass("com.tencent.mobileqq.aio.msg.AIOMsgItem");
            String[] component = new String[]{
                    "com.tencent.mobileqq.aio.msglist.holder.component.pic.AIOPicContentComponent",
                    "com.tencent.mobileqq.aio.msglist.holder.component.mix.AIOMixContentComponent",
            };


            Method getMsg = null;
            Method[] methods = Initiator.loadClass("com.tencent.mobileqq.aio.msglist.holder.component.BaseContentComponent").getDeclaredMethods();
            for (Method method : methods) {
                if (method.getReturnType() == msgClass && method.getParameterTypes().length == 0) {
                    getMsg = method;
                    getMsg.setAccessible(true);
                    break;
                }
            }
            for (String s : component) {
                Class componentClazz = Initiator.loadClass(s);
                Method listMethod = null;
                methods = componentClazz.getDeclaredMethods();
                for (Method method : methods) {
                    if (method.getReturnType() == List.class && method.getParameterTypes().length == 0) {
                        listMethod = method;
                        listMethod.setAccessible(true);
                        break;
                    }
                }
                Method finalGetMsg = getMsg;
                HookUtils.hookAfterIfEnabled(this, listMethod, param -> {
                    Object msg = finalGetMsg.invoke(param.thisObject);
                    Object item = CustomMenu.createItemNt(msg, "保存到面板", R.id.item_save_to_panel, () -> {
                        try {
                            long msgID = (long) Reflex.invokeVirtual(msg, "getMsgId");
                            IKernelMsgService service = MsgServiceHelper.getKernelMsgService(AppRuntimeHelper.getAppRuntime());
                            ArrayList<Long> msgIDs = new ArrayList<>();
                            msgIDs.add(msgID);
                            service.getMsgsByMsgId(SessionUtils.AIOParam2Contact(AIOParam), msgIDs, (result, errMsg, msgList) -> {
                                SyncUtils.runOnUiThread(()->{
                                    for (MsgRecord msgRecord : msgList) {
                                        ArrayList<String> md5s = new ArrayList<>();
                                        ArrayList<String> urls = new ArrayList<>();

                                        for (MsgElement element : msgRecord.getElements()){
                                            if (element.getPicElement() != null){
                                                PicElement picElement = element.getPicElement();
                                                //md5必须大写才能加载
                                                md5s.add(picElement.getMd5HexStr().toUpperCase());
                                                urls.add("https://gchat.qpic.cn/gchatpic_new/0/0-0-" + picElement.getMd5HexStr().toUpperCase() + "/0");
                                            }
                                        }
                                        if (!md5s.isEmpty()){
                                            if (md5s.size() > 1){
                                                PanelUtils.PreSaveMultiPicList(urls,md5s, CommonContextWrapper.createAppCompatContext(ContextUtils.getCurrentActivity()));
                                            }else {
                                                PanelUtils.PreSavePicToList(urls.get(0),md5s.get(0), CommonContextWrapper.createAppCompatContext(ContextUtils.getCurrentActivity()));
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
                    result.add(0,item);
                    result.addAll(list);
                    param.setResult(result);
                });
            }


        }

        return true;
    }

    @NonNull
    @Override
    public String getName() {
        return "表情面板";
    }

    @Override
    public boolean isAvailable() {
        return QAppUtils.isQQnt();
    }

    @Override
    public void onAIOParamUpdate(Object AIOParam) {
        StickerPanelEntryHooker.AIOParam = AIOParam;
    }
}
