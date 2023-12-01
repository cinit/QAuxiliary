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

package io.github.qauxv.dsl

import io.github.qauxv.base.IUiItemAgentProvider
import io.github.qauxv.dsl.func.FragmentDescription
import io.github.qauxv.dsl.func.IDslItemNode
import io.github.qauxv.dsl.func.IDslParentNode
import io.github.qauxv.dsl.func.RootFragmentDescription
import io.github.qauxv.dsl.func.UiItemAgentDescription
import io.github.qauxv.fragment.AboutFragment
import io.github.qauxv.fragment.BackupRestoreConfigFragment
import io.github.qauxv.fragment.PendingFunctionFragment
import io.github.qauxv.fragment.TroubleshootFragment

object FunctionEntryRouter {

    private val mAnnotatedUiItemAgentDescriptionList: Array<IUiItemAgentProvider> by lazy {
        io.github.qauxv.gen.getAnnotatedUiItemAgentEntryList()
    }

    /**
     * The full UI-DSL-function tree
     */
    @JvmStatic
    val settingsUiItemDslTree: RootFragmentDescription by lazy {
        zwBuildUiItemDslTree()
    }

    /**
     * Skeletons of the DSL tree, used for any-cast lookup
     */
    private val settingsUiItemDslTreeSkeleton: RootFragmentDescription by lazy {
        zwCreateBaseDslTree()
    }

    @JvmStatic
    fun findDescriptionByLocation(location: Array<String>): IDslItemNode? {
        val absoluteLocation = resolveUiItemAnycastLocation(location) ?: return null
        if (absoluteLocation.isEmpty() || absoluteLocation.size == 1 && absoluteLocation[0] == "") {
            // root
            return settingsUiItemDslTree
        }
        return settingsUiItemDslTree.lookupHierarchy(absoluteLocation)
    }

    @JvmStatic
    fun queryAnnotatedUiItemAgentEntries(): Array<IUiItemAgentProvider> {
        return mAnnotatedUiItemAgentDescriptionList
    }

    @JvmStatic
    fun resolveUiItemAnycastLocation(location: Array<String>): Array<String>? {
        // check if the location is a anycast location
        if (location.size != 2 || location[0] != Locations.ANY_CAST_PREFIX) {
            // return as is
            return location
        }
        val tag = location[1]
        if (tag.isEmpty()) {
            // wtf?
            return arrayOf()
        }
        return settingsUiItemDslTreeSkeleton.findLocationByIdentifier(tag)
    }

    /**
     * Create a DSL tree for the function entry. The tree is a tree of [RootFragmentDescription]
     * Categories are defined here. Keep values sync with the [Locations].
     * Otherwise, the tree will be broken and items will be thrown into lost-and-found.
     */
    private fun zwCreateBaseDslTree(): RootFragmentDescription {
        val baseTree = RootFragmentDescription {
            category("host-ui", "净化设置") {
                fragment("host-ui-main", "主页") {
                    category("ui-title", "标题栏")
                    category("ui-msg-list", "消息列表")
                    category("ui-contact", "联系人")
                    category("ui-operation-log", "动态")
//                    category("ui-other", "其他", false)
                }
                fragment("host-ui-sideswipe", "侧滑栏")
                fragment("host-ui-chat", "聊天界面") {
                    category("ui-chat-message", "消息")
                    category("ui-chat-decoration", "装扮")
                    category("ui-chat-emoticon", "表情")
                    category("ui-chat-other", "其他", false)
                }
                fragment("host-ui-group-chat", "群聊") {
                    category("ui-group-chat-title", "标题栏")
                    category("ui-group-chat-other", "其他", false)
                }
                fragment("ui-profile", "资料卡")
                fragment("ui-misc", "杂项", false)
            }
            category("auxiliary-function", "辅助功能") {
                fragment("auxiliary-chat-and-message", "聊天和消息") {
                    category("auxiliary-chat", "聊天")
                    category("auxiliary-message", "消息")
                    category("auxiliary-guild", "频道")
                }
                fragment("auxiliary-file", "文件与存储")
                fragment("auxiliary-friend-and-profile", "好友和资料卡") {
                    category("auxiliary-friend", "好友")
                    category("auxiliary-profile", "资料卡")
                }
                fragment("auxiliary-group", "群聊")
                fragment("auxiliary-notification", "通知设置")
                fragment("auxiliary-experimental", "实验性功能")
                fragment("entertainment-function", "娱乐功能")
                fragment("auxiliary-misc", "杂项", false)
            }
            category("module-config", "配置", false) {
                fragmentImpl("cfg-backup-restore", "备份和恢复", BackupRestoreConfigFragment::class.java)
            }
            category("debug-category", "调试", false) {
                fragment("debug-function", "调试功能", false)
                fragmentImpl("debug-impl", "故障排查", TroubleshootFragment::class.java)
            }
            category("other-config", "其他") {
                fragmentImpl("other-coming-soon", "开发中的功能", PendingFunctionFragment::class.java, false)
                fragmentImpl("other-about", "关于", AboutFragment::class.java, false)
            }
        }
        return baseTree
    }

    private fun zwBuildUiItemDslTree(): RootFragmentDescription {
        val baseTree: RootFragmentDescription = zwCreateBaseDslTree()
        val lostAndFoundItems = mutableListOf<IUiItemAgentProvider>()
        val annotatedUiItemAgentEntries = queryAnnotatedUiItemAgentEntries()
        for (uiItemAgentEntry in annotatedUiItemAgentEntries) {
            var location = uiItemAgentEntry.uiItemLocation
            location = resolveUiItemAnycastLocation(location) ?: location
            // find the parent node
            val parentNode = baseTree.lookupHierarchy(location)
            if (parentNode is IDslParentNode) {
                parentNode.addChild(UiItemAgentDescription(uiItemAgentEntry))
            } else {
                // not found, add to lost and found
                lostAndFoundItems.add(uiItemAgentEntry)
            }
        }
        if (lostAndFoundItems.isNotEmpty()) {
            // create a lost and found node
            val lostAndFoundFragmentDescription = FragmentDescription("lost-and-found", "Lost & Found") {
                lostAndFoundItems.forEach {
                    addChild(UiItemAgentDescription(it))
                }
            }
            // add to the top of the tree to make it the first node
            baseTree.addChild(lostAndFoundFragmentDescription, 0)
            // sync with the skeleton
            settingsUiItemDslTreeSkeleton.addChild(FragmentDescription("lost-and-found", "Lost & Found", false, null), 0)
        }
        return baseTree
    }

    /**
     * Static routing destinations
     */
    object Locations {

        const val ANY_CAST_PREFIX: String = "@any-cast"


        object Simplify {

            /**
             * 主页
             */
            @JvmField
            val MAIN_UI_TITLE: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-title")

            @JvmField
            val MAIN_UI_MSG_LIST: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-msg-list")

            @JvmField
            val MAIN_UI_CONTACT: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-contact")

            @JvmField
            val MAIN_UI_OPERATION_LOG: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-operation-log")

            @JvmField
            val MAIN_UI_MISC: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-misc")

            /**
             * 侧滑栏
             */
            @JvmField
            val SLIDING_UI: Array<String> = arrayOf(ANY_CAST_PREFIX, "host-ui-sideswipe")

            /**
             * 聊天
             */
            @JvmField
            val CHAT_DECORATION: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-chat-decoration")

            @JvmField
            val CHAT_EMOTICON: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-chat-emoticon")

            @JvmField
            val CHAT_OTHER: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-chat-other")

            @JvmField
            val CHAT_GROUP_TITLE: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-group-chat-title")

            @JvmField
            val UI_CHAT_MSG: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-chat-message")

            @JvmField
            val CHAT_GROUP_ANIMATION: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-group-chat-animation")

            @JvmField
            val CHAT_GROUP_OTHER: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-group-chat-other")

            @JvmField
            val UI_PROFILE: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-profile")

            @JvmField
            val UI_MISC: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-misc")
        }

        object Auxiliary {

            @JvmField
            val CHAT_CATEGORY: Array<String> = arrayOf(ANY_CAST_PREFIX, "auxiliary-chat")

            @JvmField
            val MESSAGE_CATEGORY: Array<String> = arrayOf(ANY_CAST_PREFIX, "auxiliary-message")

            @JvmField
            val GUILD_CATEGORY: Array<String> = arrayOf(ANY_CAST_PREFIX, "auxiliary-guild")

            @JvmField
            val FILE_CATEGORY: Array<String> = arrayOf(ANY_CAST_PREFIX, "auxiliary-file")

            @JvmField
            val FRIEND_CATEGORY: Array<String> = arrayOf(ANY_CAST_PREFIX, "auxiliary-friend")

            @JvmField
            val GROUP_CATEGORY: Array<String> = arrayOf(ANY_CAST_PREFIX, "auxiliary-group")

            @JvmField
            val PROFILE_CATEGORY: Array<String> = arrayOf(ANY_CAST_PREFIX, "auxiliary-profile")

            @JvmField
            val NOTIFICATION_CATEGORY: Array<String> = arrayOf(ANY_CAST_PREFIX, "auxiliary-notification")

            @JvmField
            val EXPERIMENTAL_CATEGORY: Array<String> = arrayOf(ANY_CAST_PREFIX, "auxiliary-experimental")

            @JvmField
            val MISC_CATEGORY: Array<String> = arrayOf(ANY_CAST_PREFIX, "auxiliary-misc")

        }

        object Entertainment {

            @JvmField
            val ENTERTAIN_CATEGORY: Array<String> = arrayOf(ANY_CAST_PREFIX, "entertainment-function")

        }

        object ConfigCategory {

            @JvmField
            val CONFIG_CATEGORY: Array<String> = arrayOf(ANY_CAST_PREFIX, "module-config")

        }

        object DebugCategory {

            @JvmField
            val DEBUG_CATEGORY: Array<String> = arrayOf(ANY_CAST_PREFIX, "debug-function")

        }
    }
}
