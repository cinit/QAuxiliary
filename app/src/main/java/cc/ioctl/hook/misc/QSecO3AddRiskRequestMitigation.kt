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

package cc.ioctl.hook.misc

import android.app.Activity
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.View
import androidx.appcompat.app.AlertDialog
import cc.ioctl.util.HookUtils
import cc.ioctl.util.Reflex
import io.github.qauxv.util.xpcompat.XposedBridge
import io.github.qauxv.R
import io.github.qauxv.base.annotation.ComponentHookEntry
import io.github.qauxv.hook.BaseComponentHook
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.UiThread
import xyz.nextalone.util.SystemServiceUtils
import java.lang.reflect.Field

/**
 * For informative purposes only.
 * Let users to know the serverside risk control mechanism of adding friends and groups.
 * I don't want to handle the nuisance authSig for AddGroupReq.
 * Warn users to do it manually.
 *
 * This hook is lazily initialized on demand.
 */
@ComponentHookEntry
object QSecO3AddRiskRequestMitigation : BaseComponentHook() {

    const val KEY_UIN_IS_FROM_VOID: String = "QSecO3AddRiskRequestMitigation.KEY_UIN_IS_FROM_VOID"

    override val targetProcesses = SyncUtils.PROC_MAIN

    @Volatile
    private var mLegacyOnClickListenerHookedClass: Class<*>? = null

    private val mLegacyOnClickListenerHookProcedure = HookUtils.beforeAlways(this, 51) { param ->
        val view = param.args[0] as View
        val dataTag = view.tag ?: return@beforeAlways
        val nType: Int = Reflex.getFirstByType(dataTag, Int::class.javaPrimitiveType)
        if (nType != 7) {
            return@beforeAlways
        }
        val listener = param.thisObject as View.OnClickListener
        val activity: Activity = Reflex.getFirstByType(
            listener,
            Initiator.loadClass("com.tencent.mobileqq.activity.FriendProfileCardActivity")
        ) as Activity
        val intent = activity.intent
        val isFromVoid = intent.getBooleanExtra(KEY_UIN_IS_FROM_VOID, false)
        if (!isFromVoid) {
            return@beforeAlways
        }
        val allInOne: Parcelable = intent.getParcelableExtra("AllInOne") ?: return@beforeAlways
        val uin = getUinFromAllInOne(allInOne) ?: return@beforeAlways
        onAddFriendButtonClick(activity, uin) {
            XposedBridge.invokeOriginalMethod(param.method, listener, param.args)
        }
        // done, control flow terminated
        param.result = null
    }

    override fun initOnce(): Boolean {
        if (!SyncUtils.isMainProcess()) {
            return false
        }
        // old FriendProfileCardActivity
        val kLegacyFriendProfileCardActivity: Class<*>? = Initiator.load("com.tencent.mobileqq.activity.FriendProfileCardActivity")
        if (kLegacyFriendProfileCardActivity != null) {
            // well, old version of QQ
            val mOnClickListenerCandidates = kLegacyFriendProfileCardActivity.declaredFields.filter {
                it.type == View.OnClickListener::class.java
            }
            val mOnClickListenerField: Field = if (mOnClickListenerCandidates.size == 1) {
                mOnClickListenerCandidates[0]
            } else {
                // expect 2
                mOnClickListenerCandidates.single {
                    it.name == "a" || it.name == "mOnClickListener"
                }
            }
            mOnClickListenerField.isAccessible = true
            // now we don't know the actual type of the OnClickListener, but we will know when doOnCreate
            val doOnCreate = kLegacyFriendProfileCardActivity.getDeclaredMethod("doOnCreate", Bundle::class.java)
            HookUtils.hookAfterAlways(this, doOnCreate, 51) { param ->
                // we are on UI thread
                if (mLegacyOnClickListenerHookedClass != null) {
                    return@hookAfterAlways
                }
                val activity = param.thisObject as Activity
                val listener = mOnClickListenerField.get(activity) as View.OnClickListener
                val klass = listener.javaClass
                val onClick = klass.getDeclaredMethod("onClick", View::class.java)
                XposedBridge.hookMethod(onClick, mLegacyOnClickListenerHookProcedure)
                mLegacyOnClickListenerHookedClass = klass
            }
        }
        // new FriendProfileCardActivity
        val kRefactoredFriendProfileCardActivity: Class<*>? = Initiator.load("com.tencent.mobileqq.profilecard.activity.FriendProfileCardActivity")
        if (kRefactoredFriendProfileCardActivity != null) {
            val kAbsComponent = Initiator.loadClass("com/tencent/mobileqq/profilecard/base/framework/impl/AbsComponent")
            val mActivityField = kAbsComponent.getDeclaredField("mActivity").apply {
                isAccessible = true
            }
            val hookProcedure = HookUtils.beforeAlways(this, 51) { param ->
                val component = kAbsComponent.cast(param.thisObject)
                val activity = mActivityField.get(component) as Activity
                val intent = activity.intent
                val isFromVoid = intent.getBooleanExtra(KEY_UIN_IS_FROM_VOID, false)
                if (!isFromVoid) {
                    return@beforeAlways
                }
                val allInOne: Parcelable = intent.getParcelableExtra("AllInOne") ?: return@beforeAlways
                val uin = getUinFromAllInOne(allInOne) ?: return@beforeAlways
                onAddFriendButtonClick(activity, uin) {
                    XposedBridge.invokeOriginalMethod(param.method, component, param.args)
                }
                // done, control flow terminated
                param.result = null
            }
            Initiator.load("com/tencent/mobileqq/profilecard/container/bottomcontainer/BaseProfileBottomContainer")
                ?.let { kBaseProfileBottomContainer ->
                    val addFriendInner = kBaseProfileBottomContainer.getDeclaredMethod("addFriendInner")
                    XposedBridge.hookMethod(addFriendInner, hookProcedure)
                }
            Initiator.load("com/tencent/mobileqq/profilecard/base/container/ProfileBottomContainer")
                ?.let { kProfileBottomContainer ->
                    val addFriendInner = kProfileBottomContainer.getDeclaredMethod("addFriendInner")
                    XposedBridge.hookMethod(addFriendInner, hookProcedure)
                }
        }
        // VisitorTroopCardFragment
        val kVisitorTroopCardFragment: Class<*>? =
            Initiator.load("com.tencent.mobileqq.troop.troopcard.ui.VisitorTroopCardFragment")
                ?: Initiator.load("com.tencent.mobileqq.troop.troopCard.VisitorTroopCardFragment")
        if (kVisitorTroopCardFragment != null) {
            val onClick = kVisitorTroopCardFragment.getDeclaredMethod("onClick", View::class.java)
            HookUtils.hookBeforeAlways(this, onClick, 51) { param ->
                val fragment = param.thisObject as Any
                val view = param.args[0] as View
                val tag: Any = view.tag ?: return@hookBeforeAlways
                if (tag.javaClass != java.lang.Integer::class.java) {
                    return@hookBeforeAlways
                }
                val tagValue = tag as Int
                if (tagValue != 6) {
                    return@hookBeforeAlways
                }
                val activity = fragment.javaClass.getMethod("getActivity").invoke(fragment) as Activity?
                    ?: return@hookBeforeAlways
                val intent = activity.intent
                val isFromVoid = intent.getBooleanExtra(KEY_UIN_IS_FROM_VOID, false)
                if (!isFromVoid) {
                    return@hookBeforeAlways
                }
                val troopUin = intent.getStringExtra("troop_uin") ?: return@hookBeforeAlways
                if (troopUin.isEmpty()) {
                    return@hookBeforeAlways
                }
                onAddGroupButtonClick(activity, troopUin) {
                    XposedBridge.invokeOriginalMethod(param.method, fragment, param.args)
                }
                // done, control flow terminated
                param.result = null
            }
        }
        return true
    }

    @JvmStatic
    fun getUinFromAllInOne(allInOne: Parcelable): String? {
        val parcel = Parcel.obtain()
        allInOne.writeToParcel(parcel, 0)
        // uin is the first field
        parcel.setDataPosition(0)
        val uin = parcel.readString()
        parcel.recycle()
        return uin
    }

    @UiThread
    fun onAddFriendButtonClick(activity: Activity, uin: String, originProcedure: Runnable) {
        val ctx = CommonContextWrapper.createAppCompatContext(activity)
        AlertDialog.Builder(ctx)
            .setTitle(R.string.dialog_tx_risk_control_general_title)
            .setMessage(R.string.dialog_tx_risk_control_msg_for_add_friend_from_void)
            .setPositiveButton(R.string.dialog_tx_risk_control_btn_copy_user_uin) { dialog, _ ->
                dialog.dismiss()
                SystemServiceUtils.copyToClipboard(ctx, uin)
            }
            .setNegativeButton(R.string.dialog_btn_cancel, null)
            .setNeutralButton(R.string.dialog_tx_risk_control_btn_ignore_risk_warning) { dialog, _ ->
                dialog.dismiss()
                originProcedure.run()
            }
            .setCancelable(true)
            .show()
    }

    @UiThread
    fun onAddGroupButtonClick(activity: Activity, uin: String, originProcedure: Runnable) {
        val ctx = CommonContextWrapper.createAppCompatContext(activity)
        AlertDialog.Builder(ctx)
            .setTitle(R.string.dialog_tx_risk_control_general_title)
            .setMessage(R.string.dialog_tx_risk_control_msg_for_add_group_from_void)
            .setPositiveButton(R.string.dialog_tx_risk_control_btn_copy_troop_uin) { dialog, _ ->
                dialog.dismiss()
                SystemServiceUtils.copyToClipboard(ctx, uin)
            }
            .setNegativeButton(R.string.dialog_btn_cancel, null)
            .setNeutralButton(R.string.dialog_tx_risk_control_btn_ignore_risk_warning) { dialog, _ ->
                dialog.dismiss()
                originProcedure.run()
            }
            .setCancelable(true)
            .show()
    }

}
