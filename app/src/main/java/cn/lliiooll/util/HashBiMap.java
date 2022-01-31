/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by ferredoxin.
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

package cn.lliiooll.util;

import java.util.HashMap;

public class HashBiMap<K, V> extends HashMap<K, V> implements BiMap<K, V> {

    /**
     * @return 一个反向map
     */
    @Override
    public BiMap<V, K> getReverseMap() {
        BiMap<V, K> reverse = new HashBiMap<>();
        for (K key : this.keySet()) {
            V value = this.get(key);
            reverse.put(value, key);
        }
        return reverse;
    }
}
