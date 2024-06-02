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

package top.linl.hook;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HookUtils;
import de.robv.android.xposed.XC_MethodHook;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ListIterator;
import top.linl.util.ScreenParamUtils;
import top.linl.util.reflect.ClassUtils;
import top.linl.util.reflect.FieldUtils;
import top.linl.util.reflect.MethodTool;

@FunctionHookEntry
@UiItemAgentEntry
public class SortTroopSettingAppListView extends CommonSwitchFunctionHook {

    public static final SortTroopSettingAppListView INSTANCE = new SortTroopSettingAppListView();

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Auxiliary.GROUP_CATEGORY;
    }

    @Nullable
    @Override
    public CharSequence getDescription() {
        return "让群设置的群文件在上面而不是在下面";
    }

    @Override
    protected boolean initOnce() throws Exception {
        try {
            //由于我记得是qq是在8996重构了群设置页面 但是网上并无此版本安装包记录 所以决定采用异常捕获的方法来适配已经重构了群设置页的QQ
            Class<?> troopSettingFragmentV2 = ClassUtils.getClass("com.tencent.mobileqq.troop.troopsetting.activity.TroopSettingFragmentV2");
            Method assemblePartsMethod = MethodTool.find(troopSettingFragmentV2)
                    .name("assembleParts")
                    .returnType(List.class)
                    .get();
            HookUtils.hookAfterIfEnabled(this,assemblePartsMethod,param -> {
                List<Object> partList = (List<Object>) param.getResult();
                Class<?> troopAppClass = ClassUtils.getClass("com.tencent.mobileqq.troop.troopsetting.part.TroopSettingAppPart");
                Class<?> memberInfoPartClass = ClassUtils.getClass("com.tencent.mobileqq.troop.troopsetting.part.TroopSettingMemberInfoPart");
                Object troopAppPart = null;
                ListIterator<Object> iterator = partList.listIterator();
                int memberInfoPartIndex = 0;
                boolean isStopSelfIncrementing = false;
                while (iterator.hasNext()) {
                    if (!isStopSelfIncrementing) memberInfoPartIndex++;
                    Object nextPart = iterator.next();
                    //定位群成员卡片(part)的位置索引
                    if (nextPart.getClass() == memberInfoPartClass) {
                        isStopSelfIncrementing = true;
                    }
                    //获取群应用的part
                    if (nextPart.getClass() == troopAppClass) {
                        troopAppPart = nextPart;
                        iterator.remove();
                        break;
                    }
                }
                if (troopAppPart == null) throw new RuntimeException("troop app list is null");
                partList.add(memberInfoPartIndex, troopAppPart);
            });
            return true;
        } catch (Exception e) {
            //此处可能会在9.0.0前捕获到异常ReflectException : 没有找到类: com.tencent.mobileqq.troop.troopsetting.activity.TroopSettingFragmentV2
        }
        //如果抛出异常则会进行下面的代码 以保持对旧版本NT的适配
        Method doOnCreateMethod = MethodTool.find("com.tencent.mobileqq.troop.troopsetting.activity.TroopSettingActivity")
                .returnType(boolean.class)
                .params(Bundle.class)
                .name("doOnCreate")
                .get();
        HookUtils.hookAfterIfEnabled(this, doOnCreateMethod, new HookUtils.AfterHookedMethod() {
            @Override
            public void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                LinearLayout rootView = FieldUtils.getFirstField(param.thisObject, LinearLayout.class);
                int troopInfoTextIndex = 0;
                View troopAppListView = null;
//                View[] views = FieIdUtils.getFirstField(param.thisObject, View[].class);//过于复杂 不如不用

                for (int i = 0; i < rootView.getChildCount(); i++) {
                    View child = rootView.getChildAt(i);
                    if (child instanceof TextView) {
                        TextView textView = (TextView) child;
                        String text = textView.getText().toString();
                        if (text.equals("群聊信息")) {
                            troopInfoTextIndex = i;
                        }
                    }
                    if (child instanceof LinearLayout) {
                        LinearLayout simpleFormItem = (LinearLayout) child;
                        if (simpleFormItem.getChildAt(0) instanceof RelativeLayout) {
                            RelativeLayout itemTitle = (RelativeLayout) simpleFormItem.getChildAt(0);
                            if (itemTitle.getChildAt(0) instanceof TextView) {
                                TextView titleTextView = (TextView) itemTitle.getChildAt(0);
                                String titleText = titleTextView.getText().toString();
                                if (titleText.equals("群应用")) {
                                    troopAppListView = child;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (troopAppListView != null && troopInfoTextIndex != 0) {
                    rootView.removeView(troopAppListView);
                    //顶部偏移 不然会和群聊成员卡片贴一起 (贴贴
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    layoutParams.topMargin += ScreenParamUtils.dpToPx(rootView.getContext(), 16);
                    rootView.addView(troopAppListView, troopInfoTextIndex, layoutParams);
                }
            }
        });
        return true;
    }

    @NonNull
    @Override
    public String getName() {
        return "将群应用卡片(群文件)移动到正常位置";
    }
}
