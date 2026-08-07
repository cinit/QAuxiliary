/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2026 QAuxiliary developers
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

package io.github.huanxin996.hook

import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Log
import java.lang.reflect.Method

/**
 * 黑名单查询工具。
 *
 * QQ 9.x 起黑名单判断已迁移到 NT 好友数据（FriendsInfoBean.isBlock）
 * 因此这里直接反射调用 QQ 官方黑名单判断 API：
 * `QRoute.api(IProfileCardFeatureApi.class).isBlacklistUin(uin, null)`
 */
object BlockListManager {

    @Volatile
    private var sApi: Any? = null
    @Volatile
    private var sIsBlacklistUinMethod: Method? = null
    @Volatile
    private var sQueryMethod: Method? = null
    @Volatile
    private var sSingleton: Any? = null

    /**
     * 查询结果 TTL 缓存
     */
    private class CacheEntry(val value: Boolean, val expireAt: Long)

    private val sCache = java.util.concurrent.ConcurrentHashMap<String, CacheEntry>()

    private const val CACHE_TTL_MS = 30_000L

    /**
     * 反射准备查询方法，可重复调用。
     */
    @Synchronized
    fun prepare(): Boolean {
        if (sIsBlacklistUinMethod != null && sApi != null) {
            return true
        }
        return try {
            // QQ 官方黑名单判断 API
            val apiClass = Initiator.load("com/tencent/mobileqq/profilecard/api/IProfileCardFeatureApi")
            val qRouteClass = Initiator.load("com/tencent/mobileqq/qroute/QRoute")
            if (apiClass != null && qRouteClass != null) {
                val apiMethod = qRouteClass.getMethod("api", Class::class.java)
                val listenerClass = Initiator.load(
                    "com/tencent/mobileqq/profilecard/listener/CheckBlacklistListener"
                )
                val query = apiClass.getMethod("isBlacklistUin", String::class.java, listenerClass)
                sApi = apiMethod.invoke(null, apiClass)
                sIsBlacklistUinMethod = query
                Log.i("BlockListManager: prepared via IProfileCardFeatureApi.isBlacklistUin")
                return true
            }
            // 旧 MMKV 路径 g.c().e(uin)
            val gClass = Initiator.load("com/tencent/relation/common/utils/g")
            if (gClass == null) {
                Log.e("BlockListManager: g class not found")
                return false
            }
            sSingleton = gClass.getMethod("c").invoke(null)
            sQueryMethod = gClass.getMethod("e", String::class.java)
            Log.i("BlockListManager: prepared via g.e(uin) fallback")
            true
        } catch (e: Throwable) {
            Log.e("BlockListManager: prepare failed", e)
            sApi = null
            sIsBlacklistUinMethod = null
            sQueryMethod = null
            sSingleton = null
            false
        }
    }

    /**
     * 查询指定 uin 是否被当前账号拉黑。
     */
    @JvmStatic
    fun isBlocked(uin: Long): Boolean = isBlocked(uin.toString())

    /**
     * 查询指定 uin 是否被当前账号拉黑，传入 null 或空字符串时返回 false。
     */
    @JvmStatic
    fun isBlocked(uin: String?): Boolean {
        if (uin.isNullOrBlank()) {
            return false
        }
        // 只接受纯数字 uin
        if (!isValidUin(uin)) {
            return false
        }
        val now = System.currentTimeMillis()
        sCache[uin]?.let { entry ->
            if (now < entry.expireAt) {
                return entry.value
            }
        }
        val result = try {
            if (sIsBlacklistUinMethod == null && sQueryMethod == null) {
                if (!prepare()) {
                    return false
                }
            }
            if (sIsBlacklistUinMethod != null && sApi != null) {
                sIsBlacklistUinMethod!!.invoke(sApi, uin, null) as? Boolean ?: false
            } else {
                val queryMethod = sQueryMethod ?: return false
                val singleton = sSingleton ?: return false
                queryMethod.invoke(singleton, uin) as? Boolean ?: false
            }
        } catch (e: Throwable) {
            Log.e("BlockListManager: query ${maskUin(uin)} failed", e)
            false
        }
        sCache[uin] = CacheEntry(result, now + CACHE_TTL_MS)
        return result
    }

    /**
     * uin 日志脱敏
     */
    private fun maskUin(uin: String): String {
        return if (uin.length > 4) "****${uin.takeLast(4)}" else "****"
    }

    private fun isValidUin(uin: String): Boolean {
        if (uin.length < 5 || uin.length > 12) {
            return false
        }
        for (c in uin) {
            if (c !in '0'..'9') {
                return false
            }
        }
        return true
    }
}
