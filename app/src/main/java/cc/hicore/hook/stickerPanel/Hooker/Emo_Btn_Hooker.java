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
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import cc.hicore.Env;
import cc.hicore.QApp.QAppUtils;
import cc.hicore.ReflectUtil.MField;
import cc.hicore.ReflectUtil.MMethod;
import cc.hicore.ReflectUtil.MRes;
import cc.hicore.Utils.XLog;
import cc.hicore.hook.stickerPanel.ICreator;
import cc.hicore.message.ServiceHook;
import cc.ioctl.util.HookUtils;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.dexkit.AIO_Create_QQNT;
import io.github.qauxv.util.dexkit.ChatPanel_InitPanel_QQNT;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import java.lang.reflect.Field;

@FunctionHookEntry
@UiItemAgentEntry
public class Emo_Btn_Hooker extends CommonSwitchFunctionHook {
    public static final Emo_Btn_Hooker INSTANCE = new Emo_Btn_Hooker();
    private Emo_Btn_Hooker() {
        super(new DexKitTarget[]{
                ChatPanel_InitPanel_QQNT.INSTANCE,
                AIO_Create_QQNT.INSTANCE
        });
        if (QAppUtils.isQQnt()){
            ServiceHook.requireHook();
        }
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

        HookUtils.hookAfterIfEnabled(this, MMethod.FindMethod(Initiator.loadClass("com.tencent.qqnt.aio.shortcutbar.PanelIconLinearLayout"),
                null,
                ImageView.class,
                new Class[]{Initiator.load("com.tencent.qqnt.aio.shortcutbar.a")}),
                param -> {
                    ImageView imageView = (ImageView) param.getResult();
                    imageView.setOnLongClickListener(view -> {
                        ICreator.createPanel(view.getContext());
                        return true;
                    });
                });

        HookUtils.hookAfterIfEnabled(this,DexKit.loadMethodFromCache(AIO_Create_QQNT.INSTANCE),param -> {
            Object pie = param.thisObject;
            Env.AIOParam = MField.GetFirstField(pie,Initiator.loadClass("com.tencent.aio.data.AIOParam"));
        });
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
}
