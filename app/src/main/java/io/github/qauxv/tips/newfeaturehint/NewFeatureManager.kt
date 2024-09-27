/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2024 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is an opensource software: you can redistribute it
 * and/or modify it under the terms of the General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the General Public License for more details.
 *
 * You should have received a copy of the General Public License
 * along with this software.
 * If not, see
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package io.github.qauxv.tips.newfeaturehint

import android.app.Activity
import android.content.Context
import android.view.View
import io.github.qauxv.BuildConfig
import io.github.qauxv.base.IEntityAgent
import io.github.qauxv.base.ISwitchCellAgent
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.IUiItemAgentProvider
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.dsl.FunctionEntryRouter
import kotlinx.coroutines.flow.StateFlow


object NewFeatureManager {

    private const val KEY_KNOW_FEATURES = "NewFeatureManager.KnownFeatures"
    private const val KEY_LAST_CHECK_VERSION_CODE = "NewFeatureManager.LastCheckVersionCode"
    private const val KEY_NEW_FEATURE_TIP_ENABLED = "NewFeatureManager.NewFeatureTipEnabled"

    private var knownFeatures: Set<String>
        get() {
            return ConfigManager.getCache().getStringSetOrDefault(KEY_KNOW_FEATURES, setOf())
        }
        set(value) {
            ConfigManager.getCache().putStringSet(KEY_KNOW_FEATURES, value)
        }

    private var lastCheckVersionCode: Int
        get() {
            return ConfigManager.getCache().getIntOrDefault(KEY_LAST_CHECK_VERSION_CODE, -1)
        }
        set(value) {
            ConfigManager.getCache().putInt(KEY_LAST_CHECK_VERSION_CODE, value)
        }

    var newFeatureTipEnabled: Boolean
        get() {
            return ConfigManager.getCache().getBooleanOrDefault(KEY_NEW_FEATURE_TIP_ENABLED, true)
        }
        set(value) {
            ConfigManager.getCache().putBoolean(KEY_NEW_FEATURE_TIP_ENABLED, value)
        }

    fun queryNewFeatures(): Set<String>? {
        val currentVersion = BuildConfig.VERSION_CODE
        if (currentVersion == lastCheckVersionCode) {
            return null
        }
        if (lastCheckVersionCode == -1) {
            // first time
            markAllFeaturesKnown()
            return null
        }
        val known = knownFeatures
        if (known.isEmpty()) {
            return null
        }
        val current = enumerateCurrentFeatures()
        // check if there are new features
        val newFeatures = current - known
        if (newFeatures.isEmpty()) {
            return null
        }
        return newFeatures
    }

    fun markAllFeaturesKnown() {
        val old = knownFeatures
        val current = enumerateCurrentFeatures()
        val newKnown = old + current
        knownFeatures = newKnown
        // update last check version code
        lastCheckVersionCode = BuildConfig.VERSION_CODE
    }

    private fun enumerateCurrentFeatures(): Set<String> {
        val featureNames = mutableSetOf<String>()
        val items = io.github.qauxv.gen.getAnnotatedUiItemAgentEntryList()
        for (item in items) {
            featureNames.add(item.itemAgentProviderUniqueIdentifier)
        }
        return featureNames
    }

    @UiItemAgentEntry
    object NewFeatureTipEnabledAgent : IUiItemAgentProvider, IUiItemAgent, ISwitchCellAgent {
        override val uiItemAgent: IUiItemAgent = this
        override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.Auxiliary.MISC_CATEGORY
        override val titleProvider: (IEntityAgent) -> String
            get() = { "新功能提示" }
        override val summaryProvider: ((IEntityAgent, Context) -> CharSequence?)
            get() = { _, _ -> "开启后，模块更新后会提示新功能" }
        override val valueState: StateFlow<String?>? = null
        override val validator: ((IUiItemAgent) -> Boolean)? = null
        override val switchProvider: ISwitchCellAgent = this
        override val onClickListener: ((IUiItemAgent, Activity, View) -> Unit)? = null
        override val extraSearchKeywordProvider: ((IUiItemAgent, Context) -> Array<String>?)? = null
        override var isChecked: Boolean
            get() = newFeatureTipEnabled
            set(value) {
                newFeatureTipEnabled = value
            }
        override val isCheckable: Boolean = true

    }

}
