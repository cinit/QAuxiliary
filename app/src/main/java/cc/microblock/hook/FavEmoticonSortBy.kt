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

package cc.microblock.hook

import android.app.Activity
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.view.View
import androidx.core.text.buildSpannedString
import cc.hicore.QApp.QAppUtils
import cc.ioctl.util.HookUtils
import cc.ioctl.util.Reflex
import com.github.kyuubiran.ezxhelper.utils.hookAllConstructorAfter
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.dexkit.AIOPicElementType
import io.github.qauxv.util.dexkit.DexKit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import xyz.nextalone.util.get


enum class SortOptions(private val description: String) {
    None("无"),
    SortByLastUse("上次使用时间"),
    SortByTotalUse("总使用次数");

    companion object {
        fun fromInt(value: Int) = values()[value]
    }

    fun next(): SortOptions {
        return when (this) {
            None -> SortByLastUse
            SortByLastUse -> SortByTotalUse
            SortByTotalUse -> None
        }
    }

    fun toInt() = ordinal

    fun text() = description
}


@FunctionHookEntry
@UiItemAgentEntry
object FavEmoticonSortBy : CommonConfigFunctionHook("FavEmoticonSortBy",arrayOf(AIOPicElementType)) {
    override val name = "收藏表情包排序"
    var state: SortOptions
        get() {
            return SortOptions.fromInt(ConfigManager.getDefaultConfig().getInt("favEmotionSortMode", 0))
        }
        set(v) {
            ConfigManager.getDefaultConfig().putInt("favEmotionSortMode", v.toInt());
        }
    override val valueState: MutableStateFlow<String?> by lazy {
        MutableStateFlow(state.text())
    }
    override val onUiItemClickListener: (IUiItemAgent, Activity, View) -> Unit = { _, context, _ ->
        val db = ConfigManager.getLastUseEmoticonStore();
        db.clear();
        state = state.next();
        valueState.update { state.text() };
    }
    override var isEnabled
        get() = this.state != SortOptions.None;
        set(value) {throw UnsupportedOperationException()}
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY
    override fun initOnce(): Boolean {
        HookUtils.hookAfterIfEnabled(
            this, Reflex.findMethod(
                Initiator.loadClass("com.tencent.mobileqq.emosm.api.impl.FavroamingDBManagerServiceImpl"),
                null
                , "getEmoticonDataList")
        ) { it ->
            val db = ConfigManager.getLastUseEmoticonStore();

            it.result = (it.result as List<Any>).map{
                val path = it.get("emoPath") as String;
                Pair(it, db.getLongOrDefault(path, 0))
            }.toTypedArray().sortedBy { it.second }.map{ it.first }.toList();
        }

        DexKit.requireClassFromCache(AIOPicElementType).hookAllConstructorAfter {
            if(it.thisObject.get("d") /*picSubType, 1 for emoticons*/ == 1){
                val db = ConfigManager.getLastUseEmoticonStore();
                val path = it.thisObject.get("a") /*origPath*/ as String;
                if(this.state == SortOptions.SortByLastUse) {
                    db.putLong(path, System.currentTimeMillis());
                } else {
                    val lastUse = db.getLongOrDefault(path, 0);
                    db.putLong(path, lastUse+1);
                }
            }
        }

        return true;
    }

    override val isAvailable = QAppUtils.isQQnt();
}
