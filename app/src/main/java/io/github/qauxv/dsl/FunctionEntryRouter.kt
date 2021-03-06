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
import io.github.qauxv.dsl.func.*
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
            category("host-ui", "????????????") {
                fragment("host-ui-main", "??????") {
                    category("ui-title", "?????????")
//                    category("ui-msg", "??????")
                    category("ui-contact", "?????????")
                    category("ui-operation-log", "??????")
//                    category("ui-other", "??????", false)
                }
                fragment("host-ui-sideswipe", "?????????")
                fragment("host-ui-chat", "????????????") {
                    category("ui-chat-message", "??????")
                    category("ui-chat-decoration", "??????")
                    category("ui-chat-emoticon", "??????")
                    category("ui-chat-other", "??????", false)
                }
                fragment("host-ui-group-chat", "??????") {
                    category("ui-group-chat-title", "?????????")
                    category("ui-group-chat-other", "??????", false)
                }
                fragment("ui-profile", "?????????")
                fragment("ui-misc", "??????", false)
            }
            category("auxiliary-function", "????????????") {
                fragment("auxiliary-chat-and-message", "???????????????") {
                    category("auxiliary-chat", "??????")
                    category("auxiliary-message", "??????")
                    category("auxiliary-guild", "??????")
                }
                fragment("auxiliary-file", "???????????????")
                fragment("auxiliary-friend-and-profile", "??????????????????") {
                    category("auxiliary-friend", "??????")
                    category("auxiliary-profile", "?????????")
                }
                fragment("auxiliary-notification", "????????????")
                fragment("auxiliary-experimental", "???????????????")
                fragment("entertainment-function", "????????????")
                fragment("auxiliary-misc", "??????", false)
            }
            category("module-config", "??????", false) {
                fragmentImpl("cfg-backup-restore", "???????????????", BackupRestoreConfigFragment::class.java)
            }
            category("debug-category", "??????", false) {
                fragment("debug-function", "????????????", false)
                fragmentImpl("debug-impl", "????????????", TroubleshootFragment::class.java)
            }
            category("other-config", "??????") {
                fragmentImpl("other-coming-soon", "??????????????????", PendingFunctionFragment::class.java, false)
                fragmentImpl("other-about", "??????", AboutFragment::class.java, false)
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
    class Locations {
        companion object {
            @JvmField
            val ANY_CAST_PREFIX: String = "@any-cast"
        }

        class Simplify {
            companion object {
                /**
                 * ??????
                 */
                @JvmField
                val MAIN_UI_TITLE: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-title")

                @JvmField
                val MAIN_UI_MSG: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-msg")

                @JvmField
                val MAIN_UI_CONTACT: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-contact")

                @JvmField
                val MAIN_UI_OPERATION_LOG: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-operation-log")

                @JvmField
                val MAIN_UI_MISC: Array<String> = arrayOf(ANY_CAST_PREFIX, "ui-misc")

                /**
                 * ?????????
                 */
                @JvmField
                val SLIDING_UI: Array<String> = arrayOf(ANY_CAST_PREFIX, "host-ui-sideswipe")

                /**
                 * ??????
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
        }

        class Auxiliary {
            companion object {
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
                val PROFILE_CATEGORY: Array<String> = arrayOf(ANY_CAST_PREFIX, "auxiliary-profile")

                @JvmField
                val NOTIFICATION_CATEGORY: Array<String> = arrayOf(ANY_CAST_PREFIX, "auxiliary-notification")

                @JvmField
                val EXPERIMENTAL_CATEGORY: Array<String> = arrayOf(ANY_CAST_PREFIX, "auxiliary-experimental")

                @JvmField
                val MISC_CATEGORY: Array<String> = arrayOf(ANY_CAST_PREFIX, "auxiliary-misc")
            }
        }

        class Entertainment {
            companion object {
                @JvmField
                val ENTERTAIN_CATEGORY: Array<String> = arrayOf(ANY_CAST_PREFIX, "entertainment-function")
            }
        }

        class ConfigCategory {
            companion object {
                @JvmField
                val CONFIG_CATEGORY: Array<String> = arrayOf(ANY_CAST_PREFIX, "module-config")
            }
        }

        class DebugCategory {
            companion object {
                @JvmField
                val DEBUG_CATEGORY: Array<String> = arrayOf(ANY_CAST_PREFIX, "debug-function")
            }
        }
    }
}
