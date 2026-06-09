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

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import cc.ioctl.util.DebugTestRunner
import cc.ioctl.util.LayoutHelper
import com.github.kyuubiran.ezxhelper.utils.isNative
import com.github.kyuubiran.ezxhelper.utils.isStatic
import io.github.qauxv.R
import io.github.qauxv.fragment.BaseRootLayoutFragment
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.dexkit.DexMethodDescriptor
import io.github.qauxv.util.soloader.NativeLoader

/**
 * See [cc.ioctl.util.DebugTestRunner] for the test code that will be run in this fragment.
 */
class DebugTestFragment : BaseRootLayoutFragment() {

    private lateinit var mDebugText: TextView

    private fun isScrolledToBottom(): Boolean {
        val scrollView = rootLayoutView as ScrollView
        val child = scrollView.getChildAt(0)
        return scrollView.scrollY + scrollView.height >= child.height
    }

    private fun scrollToBottom() {
        val scrollView = rootLayoutView as ScrollView
        val child = scrollView.getChildAt(0)
        scrollView.scrollTo(0, child.height)
    }

    // can be call on any thread
    private fun appendTextToTextView(text: CharSequence) {
        if (text.isEmpty()) return
        SyncUtils.runOnUiThread {
            val shouldScroll = isScrolledToBottom()
            val current = mDebugText.text
            val ssb = SpannableStringBuilder(current)
            if (current.isNotEmpty() && !current.endsWith("\n")) {
                ssb.append("\n")
            }
            ssb.append(text)
            if (!text.endsWith("\n")) {
                ssb.append("\n")
            }
            mDebugText.text = ssb
            mDebugText.invalidate()
            if (shouldScroll) {
                scrollToBottom()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun doOnCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        title = this.javaClass.simpleName
        val ctx = inflater.context
        val root = ScrollView(context).apply {
            val ll = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                mDebugText = TextView(context).apply {
                    text = "DebugTestFragment"

                    textSize = 14f
                    typeface = Typeface.MONOSPACE
                    setTextIsSelectable(true)
                    setTextColor(ResourcesCompat.getColor(resources, R.color.firstTextColor, ctx.theme))
                    val dp8 = LayoutHelper.dip2px(ctx, 8f)
                    setPadding(dp8, dp8, dp8, dp8)
                    movementMethod = LinkMovementMethod.getInstance()
                }
                addView(mDebugText, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            }
            addView(ll, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
        rootLayoutView = root
        mDebugText.setTextIsSelectable(true)

        SyncUtils.post {
            showInitialTexts()
        }

        return root
    }

    private fun clickableSpan(text: String, onClick: () -> Unit): CharSequence {
        return object : ClickableSpan() {
            override fun onClick(widget: View) {
                onClick()
            }
        }.let {
            val ssb = SpannableStringBuilder(text)
            ssb.setSpan(it, 0, text.length, 0)
            ssb
        }
    }

    // ====== begin hook test code ======

    private fun showInitialTexts() {
        val ssb = SpannableStringBuilder()
        val info = "API " + android.os.Build.VERSION.SDK_INT +
            ", ISA: " + NativeLoader.getIsaName(NativeLoader.getPrimaryNativeLibraryIsa()) +
            ", page size: " + android.system.Os.sysconf(android.system.OsConstants._SC_PAGESIZE)
        ssb.append(info)

        ssb.append('\n')
        ssb.append('\n')
        ssb.append(clickableSpan("Run Xposed Hook Tests") {
            appendTextToTextView("Running tests...")
            val result = runTests()
            appendTextToTextView("Tests finished:\n$result")
        })
        ssb.append('\n')
        ssb.append('\n')

        ssb.append(clickableSpan("Check for conflict class for host") {
            SyncUtils.async {
                checkForConflictClassForHost()
            }
        })
        appendTextToTextView(ssb)
        ssb.append('\n')
    }

    private fun checkForConflictClassForHost() {
        val result = DebugTestRunner.checkForConflictClassForHost(
            requireContext().applicationContext,
            ::appendTextToTextView,
        )
        if (!result.success) {
            appendTextToTextView("Conflict class check FAILED")
        }
    }

    fun runTests(): String {
        return DebugTestRunner.runXposedHookTests().output
    }


}

////@formatter:off
//static JNINativeMethod gMethods[] = {
//    {"nativeInitializeLsplant", "()V", reinterpret_cast<void*>(Java_io_github_qauxv_util_hookimpl_lsplant_LsplantBridge_nativeInitializeLsplant)},
//    {"nativeHookMethod", "(Ljava/lang/reflect/Member;Ljava/lang/reflect/Member;Ljava/lang/Object;)Ljava/lang/reflect/Method;", reinterpret_cast<void*>(Java_io_github_qauxv_util_hookimpl_lsplant_LsplantBridge_nativeHookMethod)},
//    {"nativeIsMethodHooked", "(Ljava/lang/reflect/Member;)Z", reinterpret_cast<void*>(Java_io_github_qauxv_util_hookimpl_lsplant_LsplantBridge_nativeIsMethodHooked)},
//    {"nativeUnhookMethod", "(Ljava/lang/reflect/Member;)Z", reinterpret_cast<void*>(Java_io_github_qauxv_util_hookimpl_lsplant_LsplantBridge_nativeUnhookMethod)},
//    {"nativeDeoptimizeMethod", "(Ljava/lang/reflect/Member;)Z", reinterpret_cast<void*>(Java_io_github_qauxv_util_hookimpl_lsplant_LsplantBridge_nativeDeoptimizeMethod)},
//};
////@formatter:on
//REGISTER_PRIMARY_PRE_INIT_NATIVE_METHODS("io/github/qauxv/util/Natives", gMethods);

private fun makeJniSignatures(klass: Class<*>): String {
    var body = ""
    for (method in klass.declaredMethods) {
        if (!method.isNative) continue
        val name = method.name
        val sig = DexMethodDescriptor.getMethodTypeSig(method)
        val func = "Java_${klass.name.replace(".", "_")}_${name}"
        body += "    {\"$name\", \"$sig\", reinterpret_cast<void*>($func)},\n"
    }
    val defs = "static JNINativeMethod gMethods[] = {\n$body\n};\n"
    val prefix = "//@formatter:off\n"
    val suffix = "//@formatter:on\n"
    val reg = "REGISTER_PRIMARY_PRE_INIT_NATIVE_METHODS(\"${klass.name.replace(".", "/")}\", gMethods);\n"
    return prefix + defs + suffix + reg
}

private fun typeToJniType(type: Class<*>): Pair<String, String> {
    // type -> <type, name>
    // String -> <"jstring", "str">
    // int -> <"jint", "i">
    // long[] -> <"jlongArray", "arr">
    // int[][] -> <"jojectArray", "arr">
    // Object -> <"jobject", "obj">
    // void -> <"void", "void">
    if (type.isArray) {
        val component = type.componentType
        if (component.isPrimitive) {
            return when (component) {
                Int::class.java -> "jintArray" to "arr"
                Long::class.java -> "jlongArray" to "arr"
                Boolean::class.java -> "jbooleanArray" to "arr"
                Byte::class.java -> "jbyteArray" to "arr"
                Char::class.java -> "jcharArray" to "arr"
                Short::class.java -> "jshortArray" to "arr"
                Float::class.java -> "jfloatArray" to "arr"
                Double::class.java -> "jdoubleArray" to "arr"
                else -> error("Unsupported primitive array type: $component")
            }
        } else {
            return "jobjectArray" to "arr"
        }
    }
    // for common types
    return when (type) {
        String::class.java -> "jstring" to "str"
        Class::class.java -> "jclass" to "cls"
        Int::class.java -> "jint" to "i"
        Long::class.java -> "jlong" to "j"
        Boolean::class.java -> "jboolean" to "b"
        Byte::class.java -> "jbyte" to "b"
        Char::class.java -> "jchar" to "c"
        Short::class.java -> "jshort" to "s"
        Float::class.java -> "jfloat" to "f"
        Double::class.java -> "jdouble" to "d"
        Void.TYPE -> "void" to "v"
        else -> "jobject" to "obj"
    }
}

private fun makeJniHeader(klass: Class<*>): String {
    // void Java_io_github_qauxv_util_Natives_free(JNIEnv* env, jclass klass, jlong j1);
    // jint Java_org_luckypray_dexkit_DexKitBridge_nativeGetDexNum(JNIEnv *env, jobject thiz, jlong j1);
    var result = ""
    for (method in klass.declaredMethods) {
        if (!method.isNative) continue
        val name = method.name
        val isStatic = method.isStatic
        val ret = typeToJniType(method.returnType)
        val params = method.parameterTypes.mapIndexed { i, type ->
            val (jtype, jname) = typeToJniType(type)
            "$jtype j$i"
        }.joinToString(", ")
        val func = "Java_${klass.name.replace(".", "_")}_${name}"
        result += "JNIEXPORT extern \"C\" " + "${ret.first} $func(JNIEnv* env, ${if (isStatic) "jclass" else "jobject"} ${if (isStatic) "klass" else "thiz"}, $params);\n"
    }
    return result
}
