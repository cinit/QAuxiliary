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

package io.github.qauxv.fragment

import io.github.qauxv.config.ConfigManager

object ConfigEntrySearchHistoryManager {

    // the ordered search history, split by '\n'
    private const val KEY_SEARCH_HISTORY_LIST = "search_history_list"

    // the max length of search history list, set to 0 to disable
    private const val KEY_SEARCH_HISTORY_LIST_SIZE = "search_history_list_size"

    private const val SEARCH_HISTORY_LIST_SIZE_DEFAULT = 30

    private val cfg: ConfigManager by lazy {
        ConfigManager.getDefaultConfig()
    }

    val historyList: List<String>
        get() = getHistoryListInternal()

    var maxHistoryListSize: Int
        get() = cfg.getInt(KEY_SEARCH_HISTORY_LIST_SIZE, SEARCH_HISTORY_LIST_SIZE_DEFAULT)
        set(value) {
            cfg.putInt(KEY_SEARCH_HISTORY_LIST_SIZE, value)
            // trim the list
            val list = historyList
            if (list.size > value) {
                val newList = list.subList(0, value)
                updateHistoryListInternal(newList)
            }
        }

    fun addHistory(history: String) {
        val list = ArrayList(getHistoryListInternal())
        // check if the history is already in the list, if so, remove it and add it to the head
        val index = list.indexOf(history)
        if (index >= 0) {
            list.removeAt(index)
        }
        list.add(0, history)
        // trim the list if necessary
        if (list.size > maxHistoryListSize) {
            list.subList(maxHistoryListSize, list.size).clear()
        }
        updateHistoryListInternal(list)
    }

    fun removeHistory(history: String) {
        val list = ArrayList(getHistoryListInternal()).filter { it != history }
        updateHistoryListInternal(list)
    }

    val isHistoryListEnabled: Boolean
        get() = maxHistoryListSize > 0

    fun clearHistoryList() {
        cfg.putString(KEY_SEARCH_HISTORY_LIST, "")
    }

    private fun getHistoryListInternal(): List<String> {
        return cfg.getString(KEY_SEARCH_HISTORY_LIST, "").orEmpty()
            .split("\n").filter { it.isNotEmpty() }
    }

    private fun updateHistoryListInternal(list: List<String>) {
        cfg.putString(KEY_SEARCH_HISTORY_LIST, list.filter { it.isNotEmpty() }.joinToString("\n"))
    }
}
