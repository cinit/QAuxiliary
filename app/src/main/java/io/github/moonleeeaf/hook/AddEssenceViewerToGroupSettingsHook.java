package io.github.moonleeeaf.hook;

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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.dexkit.ChatSettingForTroop_InitUI_TIM;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import io.github.qauxv.util.dexkit.FormItem_TIM;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import kotlin.collections.ArraysKt;

@FunctionHookEntry
@UiItemAgentEntry
public class AddEssenceViewerToGroupSettingsHook extends CommonSwitchFunctionHook {

    public static final AddEssenceViewerToGroupSettingsHook INSTANCE = new AddEssenceViewerToGroupSettingsHook();

    private AddEssenceViewerToGroupSettingsHook() {
        super(new DexKitTarget[]{ChatSettingForTroop_InitUI_TIM.INSTANCE, FormItem_TIM.INSTANCE});
    }

    @NonNull
    @Override
    public String getName() {
        return "群设置页添加精华消息入口";
    }

    @NonNull
    @Override
    public String getDescription() {
        return "供 TIM 用户使用\n\n如果打开后的列表为空，可尝试打开群机器人页面，点击任一机器人资料卡以解决(想办法设置 Cookie p_skey 亦可)";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Auxiliary.GROUP_CATEGORY;
    }

    @Override
    public boolean isAvailable() {
        return HostInfo.isTim();
    }

    // 根据 Lcom/tencent/mobileqq/activity/ChatSettingForTroop; 进行编写
    // 感谢 klxiaoniu 大佬对本功能进行的修复和改善 :)
    @Override
    public boolean initOnce() throws Exception {
        // 第一次写这么复杂的功能
        // 直接给我看傻了
        // 但愿能运行吧（
        Method someMethod = DexKit.requireMethodFromCache(ChatSettingForTroop_InitUI_TIM.INSTANCE);

        HookUtils.hookAfterIfEnabled(this, someMethod, (param) -> {
            // arguments: ChatSettingForTroop self, String name
            Activity self = (Activity) param.thisObject;

            // 既然是执行 initUI 之后了，应该也能获取了吧
            // get Info
            Field fieldTroopInfoData = ArraysKt.single(self.getClass().getDeclaredFields(), (field) -> field.getType().getName().endsWith("TroopInfoData"));
            Object troopInfoData = fieldTroopInfoData.get(self);
            // get Uin
            Field fieldTroopUin = troopInfoData.getClass().getDeclaredField("troopUin");
            String troopUin = (String) fieldTroopUin.get(troopInfoData);

            // get the Class and create a new instance
            Class<?> itemClass = DexKit.requireClassFromCache(FormItem_TIM.INSTANCE); // 替换为aptt类的完整类名
            Constructor<?> constructor = itemClass.getConstructor(Context.class, String.class);
            Object item = constructor.newInstance(self, "查看精华消息"); // 替换为实际的字符串参数

            // 右侧红点隐藏
            Field fieldRedDot = ArraysKt.single(itemClass.getDeclaredFields(), (field) -> field.getType().equals(ImageView.class));
            fieldRedDot.setAccessible(true);
            ((ImageView) fieldRedDot.get(item)).setVisibility(View.GONE);

            Method getView = ArraysKt.single(itemClass.getDeclaredMethods(), (method) -> method.getReturnType().equals(View.class));
            getView.setAccessible(true);
            View newView = (View) getView.invoke(item);

            if (newView == null)
            // Give up and fix next time :)
            {
                throw new NullPointerException("Could not create View from class 'aptt'");
            }

            // get the Layout
            Field fieldLayout = ArraysKt.single(self.getClass().getDeclaredFields(), (field) -> field.getType().equals(LinearLayout.class));
            fieldLayout.setAccessible(true);
            LinearLayout layout = (LinearLayout) fieldLayout.get(self);

            // add this new View to this Page
            layout.addView(newView);

            // open in X5 WebView
            newView.setOnClickListener((v) -> {
                try {
                    Class<?> browser = Initiator.loadClass("com.tencent.mobileqq.activity.QQBrowserDelegationActivity");
                    Intent intent = new Intent(v.getContext(), browser);
                    intent.putExtra("fling_action_key", 2);
                    // 我就想不明白这个HashCode是怎么用的
                    intent.putExtra("fling_code_key", v.hashCode());
                    intent.putExtra("useDefBackText", true);
                    intent.putExtra("param_force_internal_browser", true);
                    intent.putExtra("url", "https://qun.qq.com/essence/index?gc=" + troopUin);
                    v.getContext().startActivity(intent);
                } catch (ClassNotFoundException e) {
                    Toast.makeText(v.getContext(), "无法启动内置浏览器，错误如下" + e, Toast.LENGTH_SHORT).show();
                }
            });
        });
        return true;
    }

}
