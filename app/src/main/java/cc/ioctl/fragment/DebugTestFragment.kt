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

package cc.ioctl.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import android.widget.ScrollView
import android.widget.TextView
import io.github.qauxv.fragment.BaseRootLayoutFragment
import io.github.qauxv.util.CustomMenu
import io.github.qauxv.util.Log
import net.bytebuddy.ByteBuddy
import net.bytebuddy.android.AndroidClassLoadingStrategy
import net.bytebuddy.implementation.FixedValue
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.matcher.ElementMatchers

class DebugTestFragment : BaseRootLayoutFragment() {

    private lateinit var mDebugText: TextView

    open abstract class TextClass {
        abstract fun getText(): String
    }

    override fun doOnCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        title = this.javaClass.simpleName
        val root = ScrollView(context).apply {
            val ll = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                mDebugText = TextView(context).apply {
                    text = "DebugTestFragment"
                }
                addView(mDebugText, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            }
            addView(ll, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
        rootLayoutView = root

        try {
            val klass = ByteBuddy()
                .subclass(TextClass::class.java)
                .method(ElementMatchers.named("getText"))
                .intercept(FixedValue.value("Hello from a fileless dex!"))
                .make()
                .load(TextClass::class.java.classLoader, AndroidClassLoadingStrategy.Wrapping())
                .loaded
            val textClass = klass.newInstance()
            mDebugText.text = mDebugText.text.toString() + "\n" + textClass.getText()
        } catch (e: Exception) {
            val err = if (e is java.lang.reflect.InvocationTargetException) e.targetException else e
            mDebugText.text = mDebugText.text.toString() + "\n" + Log.getStackTraceString(err)
        }

        return root
    }

}
