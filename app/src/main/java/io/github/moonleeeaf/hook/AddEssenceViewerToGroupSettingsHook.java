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

import android.content.Intent;
import android.view.View;
import android.widget.Toast;
import cc.ioctl.util.HookUtils;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class AddEssenceViewerToGroupSettingsHook extends CommonSwitchFunctionHook {

	public static final AddEssenceViewerToGroupSettingsHook INSTANCE = new AddEssenceViewerToGroupSettingsHook();

	@Override
	public String getName() {
		return "在群设置页添加精华消息入口 (TIM)";
	}

	@Override
	public String getDescription() {
		return "本功能仅在 TIM 3.5.1 测试通过";
	}

	@Override
	public String[] getUiItemLocation() {
		// 请不要学我这样硬编码，我是没法编译才这样的
		// 还请将来发现问题或提交时帮我把它软编码
		// pls :)
		return new String[] { "ui-group-chat-other" };
	}

	// 根据 Lcom/tencent/mobileqq/activity/ChatSettingForTroop; 进行编写
	@Override
	public boolean initOnce() throws Exception {
		// 第一次写这么复杂的功能
		// 直接给我看傻了
		// 但愿能运行吧（
		Class<?> klass = Initiator.loadClass("com.tencent.mobileqq.activity.ChatSettingForTroop");
		Method someMethod = klass.getDeclaredMethod("initUI", new Class<?>[0]);

		HookUtils.hookAfterIfEnabled(this, someMethod, (param) -> {
			// arguments: ChatSettingForTroop self, String name
			Object self = param.thisObject;

			// 既然是执行 initUI 之后了，应该也能获取了吧
			String groupUin;
			// get Info
			Field gF = self.getClass().getDeclaredField("a");
			gF.setAccessible(true);
			Object groupInfo = gF.get(self);
			// get Uin
			Field giF = groupInfo.getClass().getDeclaredField("troopUin");
			giF.setAccessible(true);
			groupUin = (String) giF.get(groupInfo);

			// get the Class and create a new instance
			Class<?> apttClass = Initiator.loadClass("aptt"); // 替换为aptt类的完整类名
			Constructor<?> constructor = apttClass.getConstructor(self.getClass(), String.class);
			Object apttObject = constructor.newInstance(self, "查看精华消息"); // 替换为实际的字符串参数

			Method gv = apttObject.getClass().getDeclaredMethod("getView", new Class<?>[0]);
			gv.setAccessible(true);
			View newView = (View) gv.invoke(apttObject);

			if (newView == null)
				// Give up and fix next time :)
				throw new NullPointerException("Could not create View from class 'aptt'");

			// get the Layout
			Field djField = self.getClass().getDeclaredField("dj");
			djField.setAccessible(true);
			Object djObject = djField.get(self);

			// add this new View to this Page
			Method addViewMethod = djObject.getClass().getDeclaredMethod("addView", View.class);
			addViewMethod.setAccessible(true);
			addViewMethod.invoke(djObject, newView);

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
					intent.putExtra("url", "https://qun.qq.com/essence/index?gc=" + groupUin);
					v.getContext().startActivity(intent);
				} catch (ClassNotFoundException e) {
					Toast.makeText(v.getContext(), "无法启动内置浏览器，错误如下" + e, Toast.LENGTH_SHORT).show();
				}
			});
		});
		return true;
	}

}
