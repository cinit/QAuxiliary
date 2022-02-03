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
import io.github.qauxv.dsl.func.IDslParentNode
import io.github.qauxv.dsl.func.RootFragmentDescription
import io.github.qauxv.dsl.func.UiItemAgentDescription

object FunctionEntryRouter {

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
    fun queryAnnotatedUiItemAgentEntries(): List<IUiItemAgentProvider> {
        TODO("not implemented")
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
                    category("ui-msg", "消息")
                    category("ui-contact", "联系人")
                    category("ui-operation-log", "动态")
                    category("ui-other", "其他", false)
                }
                fragment("host-ui-sideswipe", "侧滑烂")
                fragment("host-ui-chat", "聊天界面") {
                    category("ui-chat-decoration", "装扮")
                    category("ui-chat-emoticon", "表情")
                    category("ui-chat-other", "其他", false)
                }
                fragment("host-ui-group-chat", "群聊") {
                    category("ui-group-chat-title", "标题栏")
                    category("ui-group-chat-animation", "动画")
                    category("ui-group-chat-other", "其他", false)
                }
                fragment("host-ui-misc", "杂项", false)
            }
            category("auxiliary-function", "辅助功能") {
                fragment("auxiliary-chat-and-message", "聊天和消息") {
                    category("auxiliary-chat", "聊天")
                    category("auxiliary-message", "消息")
                }
                fragment("auxiliary-file", "文件与存储")
                fragment("auxiliary-profile", "资料卡")
                fragment("auxiliary-experimental", "实验性功能")
                fragment("auxiliary-misc", "杂项", false)
            }
            fragment("entertainment-function", "娱乐功能")
            category("other-config", "其他") {
                fragment("other-argv-config", "参数配置")
                fragment("other-debug", "故障排查")
                fragment("other-coming-soon", "开发中的功能", false)
                fragment("other-about", "关于", false)
            }
        }
        return baseTree
    }

    private fun zwBuildUiItemDslTree(): RootFragmentDescription {
        val baseTree: RootFragmentDescription = zwCreateBaseDslTree()
        val lostAndFoundItems = mutableListOf<IUiItemAgentProvider>()
        val annotatedUiItemAgentEntries = queryAnnotatedUiItemAgentEntries()
        for (uiItemAgentEntry in annotatedUiItemAgentEntries) {
            val location = uiItemAgentEntry.uiItemLocation
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
                    UiItemAgentDescription(it)
                }
            }
            // add to the top of the tree to make it the first node
            baseTree.addChild(lostAndFoundFragmentDescription, 0)
        }
        return baseTree
    }

    /**
     * Static routing destinations
     */
    class Locations {
        companion object {
            @JvmStatic
            val ANY_CAST_PREFIX: String = "@any-cast"

            class Simplify {
                companion object {
                    /**
                     * 主页
                     */
                    @JvmStatic
                    val MAIN_UI_TITLE: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-title")

                    @JvmStatic
                    val MAIN_UI_MSG: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-msg")

                    @JvmStatic
                    val MAIN_UI_CONTACT: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-contact")

                    @JvmStatic
                    val MAIN_UI_OPERATION_LOG: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-operation-log")

                    @JvmStatic
                    val MAIN_UI_MISC: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-misc")

                    /**
                     * 侧滑烂
                     */
                    @JvmStatic
                    val SLIDING_UI: Array<String> = arrayOf(ANY_CAST_PREFIX, "host-ui-sideswipe")

                    /**
                     * 聊天
                     */
                    @JvmStatic
                    val CHAT_DECORATION: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-chat-decoration")

                    @JvmStatic
                    val CHAT_EMOTICON: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-chat-emoticon")

                    @JvmStatic
                    val CHAT_OTHER: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-chat-other")

                    @JvmStatic
                    val CHAT_GROUP_TITLE: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-group-chat-title")

                    @JvmStatic
                    val CHAT_GROUP_ANIMATION: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-group-chat-animation")

                    @JvmStatic
                    val CHAT_GROUP_OTHER: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-group-chat-other")

                    @JvmStatic
                    val UI_MISC: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-misc-title")
                }
            }

            class Auxiliary {
                companion object {
                    @JvmStatic
                    val CHAT_CATEGORY: Array<String> = arrayOf(ANY_CAST_PREFIX, "auxiliary-chat")

                    @JvmStatic
                    val MESSAGE_CATEGORY: Array<String> = arrayOf(ANY_CAST_PREFIX, "auxiliary-message")

                    @JvmStatic
                    val FILE_CATEGORY: Array<String> = arrayOf(ANY_CAST_PREFIX, "auxiliary-file")

                    @JvmStatic
                    val PROFILE_CATEGORY: Array<String> = arrayOf(ANY_CAST_PREFIX, "auxiliary-profile")

                    @JvmStatic
                    val EXPERIMENTAL_CATEGORY: Array<String> = arrayOf(ANY_CAST_PREFIX, "auxiliary-experimental")

                    @JvmStatic
                    val MISC_CATEGORY: Array<String> = arrayOf(ANY_CAST_PREFIX, "auxiliary-misc")
                }
            }

            class Entertainment {
                companion object {
                    @JvmStatic
                    val ENTERTAIN_CATEGORY: Array<String> = arrayOf(ANY_CAST_PREFIX, "entertainment-function")
                }
            }
        }
    }
}
