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
package me.kyuubiran.hook

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.TextView
import cc.ioctl.util.Reflex
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.util.LicenseStatus
import kotlinx.coroutines.flow.MutableStateFlow
import me.kyuubiran.dialog.AutoRenewFireDialog
import me.kyuubiran.util.AutoRenewFireMgr
import me.kyuubiran.util.getExFriendCfg
import me.kyuubiran.util.showToastByTencent
import xyz.nextalone.util.*

//自动续火
@UiItemAgentEntry
@FunctionHookEntry
object AutoRenewFire : CommonConfigFunctionHook("kr_auto_renew_fire") {
    var autoRenewFireStarted = false

    override val name = "自动续火"
    override val valueState: MutableStateFlow<String?>? = null
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.EXPERIMENTAL_CATEGORY

    override val onUiItemClickListener: (IUiItemAgent, Activity, View) -> Unit = { _, activity, _ ->
        AutoRenewFireDialog.showMainDialog(activity)
    }

    override fun initOnce(): Boolean = throwOrTrue {
        if (!autoRenewFireStarted) {
            AutoRenewFireMgr.doAutoSend()
            autoRenewFireStarted = true
        }
        val FormSimpleItem = "com.tencent.mobileqq.widget.FormSwitchItem".clazz

        "com.tencent.mobileqq.activity.ChatSettingActivity".clazz?.method("doOnCreate")?.hookAfter(this) {

            //如果未启用 不显示按钮
            if (!getExFriendCfg()!!.getBooleanOrFalse("kr_auto_renew_fire")) return@hookAfter
            //获取 设为置顶 SwitchItem
            val setToTopItem = it.thisObject.getAll(FormSimpleItem)?.first { item ->
                item.get(TextView::class.java)?.text?.contains("置顶") ?: false
            }
            //如果SwitchItem不为空 说明为好友
            if (setToTopItem != null) {
                //创建SwitchItem对象
                val autoRenewFireItem =
                    Reflex.newInstance(
                        FormSimpleItem,
                        it.thisObject,
                        Context::class.java
                    )
                //拿到ViewGroup
                val listView = (setToTopItem as View).parent as ViewGroup
                //设置开关文本
                Reflex.invokeVirtual(
                    autoRenewFireItem,
                    "setText",
                    "自动续火",
                    CharSequence::class.java
                )
                //添加View
                listView.addView(autoRenewFireItem as View, 7)
                //拿到好友相关信息
                val intent = it.thisObject.get(Intent::class.java)
                //QQ
                val uin = intent?.getStringExtra("uin")
                //昵称
                val uinName = intent?.getStringExtra("uinname")
                //设置按钮是否启用
                Reflex.invokeVirtual(
                    autoRenewFireItem,
                    "setChecked",
                    AutoRenewFireMgr.hasEnabled(uin),
                    Boolean::class.java
                )
                //设置监听事件
                Reflex.invokeVirtual(
                    autoRenewFireItem,
                    "setOnCheckedChangeListener",
                    object : CompoundButton.OnCheckedChangeListener {
                        override fun onCheckedChanged(
                            p0: CompoundButton?,
                            p1: Boolean
                        ) {
                            if (p1) {
                                AutoRenewFireMgr.add(uin)
                                (it.thisObject as Context).showToastByTencent("已开启与${uinName}的自动续火")
                            } else {
                                AutoRenewFireMgr.remove(uin)
                                (it.thisObject as Context).showToastByTencent("已关闭与${uinName}的自动续火")
                            }
                        }
                    },
                    CompoundButton.OnCheckedChangeListener::class.java
                )
                if (LicenseStatus.isInsider()) {
                    autoRenewFireItem.setOnLongClickListener { _ ->
                        AutoRenewFireDialog.showSetMsgDialog(
                            it.thisObject as Context,
                            uin
                        )
                        true
                    }
                }
            }
        }
    }

    override var isEnabled: Boolean
        get() = getExFriendCfg()?.getBooleanOrDefault("kr_auto_renew_fire", false) ?: false
        set(value) {
            getExFriendCfg()?.putBoolean("kr_auto_renew_fire", value)
        }
}
