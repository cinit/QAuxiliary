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

package io.github.qauxv.base

import android.app.Activity
import android.content.Context
import android.view.View
import kotlinx.coroutines.flow.StateFlow

/**
 * It's a single "cell" of the UI on the settings page.
 * A cell must have a title,
 * and may or may not have a description(usually gray text),
 * and may or may not have a switch,
 * and may or may not have a value(usually orange text),
 * and may or may not have a on click listener.
 *
 * Cells are searchable by their title description. (Value is not searchable)
 * You can provide extra searchable words by implementing [IUiItemAgent.extraSearchKeywordProvider],
 * The search is implemented by full-text search, so you may want to keep the search key words long.
 * the search is like
 * ```
 * int score = 0;
 * if(title == userSearchInput) {
 *    score += 100;
 * } else if (title.contains(userSearchInput)) {
 *    score += 20;
 * }
 * ... // same to description
 * for(auto &word : extraSearchKeyWords?.invoke(...)?: emptyList()) {
 *    if (word == userSearchInput) {
 *        score += 50;
 *    } else if (word.contains(userSearchInput)) {
 *        score += 10;
 *    }
 * }
 * return score; // if score is 0, the cell will not show in the search result.
 * ```
 * If it has a on click listener, this cell will be clickable. If it don't have a validator, it will always be valid.
 *
 * You can't both have a switch, value, and description at the same time, if you do, the description will be ignored.
 */
interface IUiItemAgent : IEntityAgent {
    val valueState: StateFlow<String?>?
    val validator: ((IUiItemAgent) -> Boolean)?
    val switchProvider: ISwitchCellAgent?
    val onClickListener: ((IUiItemAgent, Activity, View) -> Unit)?
    val extraSearchKeywordProvider: ((IUiItemAgent, Context) -> Array<String>?)?
}
