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
import androidx.annotation.Keep
import androidx.core.content.res.ResourcesCompat
import cc.ioctl.util.LayoutHelper
import cc.ioctl.util.Reflex
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.isNative
import com.github.kyuubiran.ezxhelper.utils.isStatic
import io.github.qauxv.R
import io.github.qauxv.fragment.BaseRootLayoutFragment
import io.github.qauxv.poststartup.StartupInfo
import io.github.qauxv.startup.HybridClassLoader
import io.github.qauxv.util.Log
import io.github.qauxv.util.NonUiThread
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.dexkit.DexFlow
import io.github.qauxv.util.dexkit.DexMethodDescriptor
import io.github.qauxv.util.hookimpl.lsplant.LsplantHookImpl
import io.github.qauxv.util.soloader.NativeLoader
import io.github.qauxv.util.xpcompat.XposedBridge
import net.bytebuddy.ByteBuddy
import net.bytebuddy.android.AndroidClassLoadingStrategy
import net.bytebuddy.implementation.FixedValue
import net.bytebuddy.matcher.ElementMatchers
import java.util.zip.ZipFile

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

    @NonUiThread
    private fun checkForConflictClassForHost() {
        val ctx = requireContext().applicationContext
        // where base.apk is located
        val hostPaths = ArrayList<String>()
        val selfPath = StartupInfo.getModulePath()
        if (StartupInfo.isInHostProcess()) {
            hostPaths.add(ctx.packageCodePath)
        } else {
            val candidates = arrayOf(
                "com.tencent.mobileqq",
                "com.tencent.tim",
                "com.tencent.mobileqqi",
                "com.tencent.minihd.qq",
                "com.tencent.qqlite",
            )
            // check whether the host app is installed
            for (pkg in candidates) {
                val pms = ctx.packageManager
                try {
                    val pi = pms.getPackageInfo(pkg, 0)
                    hostPaths.add(pi.applicationInfo!!.sourceDir)
                } catch (e: Exception) {
                    // not installed
                }
            }
        }
        if (hostPaths.isEmpty()) {
            appendTextToTextView("No host app found to check")
            return
        }
        for (hostPath in hostPaths) {
            try {
                checkForConflictClassForSingleHostPackage(selfPath, hostPath)
            } catch (e: Throwable) {
                val err = if (e is java.lang.reflect.InvocationTargetException) e.targetException else e
                Log.e(e)
                appendTextToTextView("Error checking host package $hostPath:\n" + Log.getStackTraceString(err))
            }
        }
    }

    @NonUiThread
    private fun checkForConflictClassForSingleHostPackage(selfPath: String, hostPackage: String) {

        appendTextToTextView("\n\n====== Checking host package: $hostPackage ======")

        val apk = ZipFile(selfPath)

        val moduleClasses = HashSet<String>()
        var i = 1
        while (true) {
            val name = if (i == 1) "classes.dex" else "classes${i}.dex"
            val entry = apk.getEntry(name) ?: break
            apk.getInputStream(entry).use { ins ->
                val classes = getDeclaredClassesFromDex(readAll(ins))
                moduleClasses.addAll(classes)
                appendTextToTextView("Module $name has ${classes.size} classes")
            }
            i++
        }
        appendTextToTextView("Load module classes done")
        apk.close()

        val hostBasePath = java.io.File(hostPackage)
        val hostClasses = HashSet<String>(10000)
        if (hostBasePath.isDirectory) {
            var j = 1
            while (true) {
                val name = if (j == 1) "classes.dex" else "classes${j}.dex"
                val dexFile = java.io.File(hostBasePath, name)
                if (!dexFile.exists()) break
                java.io.FileInputStream(dexFile).use { fis ->
                    val dex = readAll(fis)
                    val dexClasses = getDeclaredClassesFromDex(dex)
                    hostClasses.addAll(dexClasses)
                    appendTextToTextView("Host $name has ${dexClasses.size} classes.")
                }
                j++
            }
        } else {
            ZipFile(hostBasePath).use { hostApk ->
                var j = 1
                while (true) {
                    val name = if (j == 1) "classes.dex" else "classes${j}.dex"
                    val entry = hostApk.getEntry(name) ?: break
                    hostApk.getInputStream(entry).use { ins ->
                        val classes = getDeclaredClassesFromDex(readAll(ins))
                        hostClasses.addAll(classes)
                        appendTextToTextView("Host $name has ${classes.size} classes")
                    }
                    j++
                }
            }
        }
        appendTextToTextView("Load host classes done")

        val hostPackages = getClassPackages(hostClasses)
        val modulePackages = getClassPackages(moduleClasses)

        val hostDefaultClasses = getDefaultClasses(hostClasses)
        val moduleDefaultClasses = getDefaultClasses(moduleClasses)

        appendTextToTextView("Host has ${hostClasses.size} classes, in ${hostPackages.size} packages.")
        appendTextToTextView("Module has ${moduleClasses.size} classes, in ${modulePackages.size} packages.")
        val overlaps = HashSet<String>(384)
        for (s in modulePackages) {
            if (hostPackages.contains(s)) {
                overlaps.add(s)
            }
        }
        appendTextToTextView("Overlapping package count: ${overlaps.size}")
        val oa = ArrayList<String>(128)
        for (s in overlaps) {
            if (!HybridClassLoader.isConflictingClass("$s.")) {
                oa.add(s)
            }
        }
        oa.sort()
        appendTextToTextView("Conflicting package count: ${oa.size}")
        for (s in oa) {
            appendTextToTextView(s)
        }

        overlaps.clear()

        appendTextToTextView("Host has ${hostDefaultClasses.size} default classes.")
        appendTextToTextView("Module has ${moduleDefaultClasses.size} default classes.")
        for (s in moduleDefaultClasses) {
            if (hostDefaultClasses.contains(s)) {
                overlaps.add(s)
            }
        }
        appendTextToTextView("Overlapping default class count: ${overlaps.size}")
        val oda = ArrayList<String>(128)
        for (s in overlaps) {
            if (!HybridClassLoader.isConflictingClass(s)) {
                oda.add(s)
            }
        }
        oda.sort()
        appendTextToTextView("Conflicting default class count: ${oda.size}")
        for (s in oda) {
            appendTextToTextView(s)
        }

        appendTextToTextView("====== Check finished for host package: $hostPackage ======\n\n")
    }

    private fun readAll(input: java.io.InputStream): ByteArray {
        val baos = java.io.ByteArrayOutputStream()
        input.use { ins ->
            val buf = ByteArray(4096)
            while (true) {
                val n = ins.read(buf)
                if (n <= 0) break
                baos.write(buf, 0, n)
            }
        }
        return baos.toByteArray()
    }

    private fun getDeclaredClassesFromDex(dex: ByteArray): HashSet<String> {
        val result = HashSet<String>(1024)
        val classDefsSize = DexFlow.readLe32(dex, 0x60)
        val classDefsOff = DexFlow.readLe32(dex, 0x64)
        for (cn in 0 until classDefsSize) {
            val classIdx = DexFlow.readLe32(dex, classDefsOff + cn * 32)
            val c = DexFlow.readType(dex, classIdx)
            result.add(c)
        }
        return result
    }

    private fun getClassPackages(vararg clazzes: HashSet<String>): HashSet<String> {
        val result = HashSet<String>(128)
        for (clz in clazzes) {
            for (s in clz) {
                if (s.startsWith("L") && s.endsWith(";")) {
                    val body = s.substring(1, s.length - 1)
                    if (body.contains("/")) {
                        result.add(body.substring(0, body.lastIndexOf('/')).replace('/', '.').intern())
                    }
                } else {
                    throw IllegalArgumentException("Bad class name: $s")
                }
            }
        }
        return result
    }

    private fun getDefaultClasses(vararg clazzes: HashSet<String>): HashSet<String> {
        val result = HashSet<String>(128)
        for (clz in clazzes) {
            for (s in clz) {
                if (s.startsWith("L") && s.endsWith(";")) {
                    val body = s.substring(1, s.length - 1)
                    if (!body.contains("/")) {
                        result.add(body.intern())
                    }
                } else {
                    throw IllegalArgumentException("Bad class name: $s")
                }
            }
        }
        return result
    }

    open abstract class TextClass {
        abstract fun getText(): String
    }

    // virtual method hook test
    open class TestCase1 {

        @Keep
        open fun test1(s: String, i: Int, d: Double, f: Float, j: Long): String {
            return "NG" + this + s + i + d + f + j
        }

        @Keep
        open fun origin1(s: String, i: Int, d: Double, f: Float, j: Long): String {
            return "NG" + this + s + i + d + f + j
        }
    }

    // direct method hook test
    object TestCase2 {

        // static method
        @Keep
        @JvmStatic
        fun test2(s: String, i: Int, d: Double, f: Float, j: Long): String {
            return "NG" + s + i + d + f + j
        }

        @Keep
        @JvmStatic
        fun origin2(s: String, i: Int, d: Double, f: Float, j: Long): String {
            return "NG" + s + i + d + f + j
        }

        // private method
        @Keep
        private fun test4(s: String, i: Int, d: Double, f: Float, j: Long): String {
            return "NG" + this + s + i + d + f + j
        }

        @Keep
        private fun origin4(s: String, i: Int, d: Double, f: Float, j: Long): String {
            return "NG" + this + s + i + d + f + j
        }

        fun callTest4(s: String, i: Int, d: Double, f: Float, j: Long): String {
            // avoid inlining
            if (Math.random() * Math.random() < 2) {
                return test4(s, i, d, f, j)
            } else {
                return "Math.random() < 2"
            }
        }
    }

    // constructor hook test
    class Test3(s: String, i: Int, d: Double, f: Float, j: Long) {
        var r: String

        init {
            r = "NG" + this + s + i + d + f + j
        }
    }

    class OriginTest3(s: String, i: Int, d: Double, f: Float, j: Long) {
        var r: String

        init {
            r = "NG" + this + s + i + d + f + j
        }
    }

    private fun performHookTest(): String {
        val ps = "S" + Math.random()
        val pi = (Math.random() * 10000f).toInt()
        val pd = (Math.random() * 10000f).toDouble()
        val pf = (Math.random() * 10000f).toFloat()
        val pj = (Math.random() * 10000f).toLong()
        val pobj1 = TestCase1()
        var stat = "\n"
        val test1 =
            TestCase1::class.java.getDeclaredMethod("test1", String::class.java, Int::class.java, Double::class.java, Float::class.java, Long::class.java)
        stat += test1
        stat += "\n"
        val test2 =
            TestCase2::class.java.getDeclaredMethod("test2", String::class.java, Int::class.java, Double::class.java, Float::class.java, Long::class.java)
        stat += test2
        stat += "\n"
        val test3 = Test3::class.java.getDeclaredConstructor(String::class.java, Int::class.java, Double::class.java, Float::class.java, Long::class.java)
        stat += test3
        stat += "\n"
        val test4 =
            TestCase2::class.java.getDeclaredMethod("test4", String::class.java, Int::class.java, Double::class.java, Float::class.java, Long::class.java)
        stat += test4
        stat += "\n"

        test1.hookAfter {
            val o = it.thisObject as TestCase1
            val s = it.args[0] as String
            val i = it.args[1] as Int
            val d = it.args[2] as Double
            val f = it.args[3] as Float
            val j = it.args[4] as Long
            it.result = "a" + o + s + i + d + f + j
        }
        test2.hookAfter {
            val s = it.args[0] as String
            val i = it.args[1] as Int
            val d = it.args[2] as Double
            val f = it.args[3] as Float
            val j = it.args[4] as Long
            it.result = "b" + s + i + d + f + j
        }
        test3.hookAfter {
            val o = it.thisObject as Test3
            val s = it.args[0] as String
            val i = it.args[1] as Int
            val d = it.args[2] as Double
            val f = it.args[3] as Float
            val j = it.args[4] as Long
            o.r = "c" + o + s + i + d + f + j
            it.result = o
        }
        test4.hookAfter {
            val o = it.thisObject as TestCase2
            val s = it.args[0] as String
            val i = it.args[1] as Int
            val d = it.args[2] as Double
            val f = it.args[3] as Float
            val j = it.args[4] as Long
            it.result = "d" + o + s + i + d + f + j
        }

        // do test
        val ret1 = pobj1.test1(ps, pi, pd, pf, pj)
        val ret2 = TestCase2.test2(ps, pi, pd, pf, pj)
        val ret3 = Test3(ps, pi, pd, pf, pj)
        val ret4 = TestCase2.callTest4(ps, pi, pd, pf, pj)
        val expected1 = "a" + pobj1 + ps + pi + pd + pf + pj
        val expected2 = "b" + ps + pi + pd + pf + pj
        val expected3 = "c" + ret3 + ps + pi + pd + pf + pj
        val expected4 = "d" + TestCase2 + ps + pi + pd + pf + pj

        stat += "\n"
        if (ret1 != expected1) {
            stat += "Hook test1 FAILED: $ret1 != $expected1"
        } else {
            stat += "Hook test1 success: $ret1"
        }
        stat += "\n"
        if (ret2 != expected2) {
            stat += "Hook test2 FAILED: $ret2 != $expected2"
        } else {
            stat += "Hook test2 success: $ret2"
        }
        stat += "\n"
        if (ret3.r != expected3) {
            stat += "Hook test3 FAILED: ${ret3.r} != $expected3"
        } else {
            stat += "Hook test3 success: ${ret3.r}"
        }
        stat += "\n"
        if (ret4 != expected4) {
            stat += "Hook test4 FAILED: $ret4 != $expected4"
        } else {
            stat += "Hook test4 success: $ret4"
        }

        // check call origin method on not hooked method
        val pobj2 = TestCase1()
        val orig1 = XposedBridge.invokeOriginalMethod(
            TestCase1::class.java.getDeclaredMethod("origin1", String::class.java, Int::class.java, Double::class.java, Float::class.java, Long::class.java),
            pobj2,
            arrayOf(ps, pi, pd, pf, pj)
        ) as String
        val orig2 = XposedBridge.invokeOriginalMethod(
            TestCase2::class.java.getDeclaredMethod("origin2", String::class.java, Int::class.java, Double::class.java, Float::class.java, Long::class.java),
            null,
            arrayOf(ps, pi, pd, pf, pj)
        ) as String
        val pobj3 = Reflex.allocateInstance(Test3::class.java)
        XposedBridge.invokeOriginalMethod(
            Test3::class.java.getDeclaredConstructor(String::class.java, Int::class.java, Double::class.java, Float::class.java, Long::class.java),
            pobj3,
            arrayOf(ps, pi, pd, pf, pj)
        )
        val orig3 = pobj3.r
        val orig4 = XposedBridge.invokeOriginalMethod(
            TestCase2::class.java.getDeclaredMethod("origin4", String::class.java, Int::class.java, Double::class.java, Float::class.java, Long::class.java),
            TestCase2,
            arrayOf(ps, pi, pd, pf, pj)
        ) as String
        // all should start with "NG"
        if (orig1.startsWith("NG") && orig2.startsWith("NG") && orig3.startsWith("NG") && orig4.startsWith("NG")) {
            stat += "\n"
            stat += "Call origin method success"
        } else {
            stat += "\n"
            stat += "Call origin method FAILED"
        }

        return stat
    }

    fun runTests(): String {
        var result = ""
        try {
            val klass = ByteBuddy()
                .subclass(TextClass::class.java)
                .method(ElementMatchers.named("getText"))
                .intercept(FixedValue.value("Hello from a fileless dex!"))
                .make()
                .load(TextClass::class.java.classLoader, AndroidClassLoadingStrategy.Wrapping())
                .loaded
            val textClass = klass.newInstance()
            result += "\n" + textClass.getText()

            // init LSPlant for self test purpose
            if (StartupInfo.getHookBridge() == null) {
                LsplantHookImpl.initializeLsplantHookBridge();
            }
            result += "\n" + performHookTest()
        } catch (e: Throwable) {
            val err = if (e is java.lang.reflect.InvocationTargetException) e.targetException else e
            Log.e(e)
            result += "\n" + Log.getStackTraceString(err)
        }
        return result
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
