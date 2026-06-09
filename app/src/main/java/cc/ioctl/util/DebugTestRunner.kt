/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2026 QAuxiliary developers
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

package cc.ioctl.util

import android.content.Context
import androidx.annotation.Keep
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import io.github.qauxv.poststartup.StartupInfo
import io.github.qauxv.startup.HybridClassLoader
import io.github.qauxv.util.Log
import io.github.qauxv.util.dexkit.DexFlow
import io.github.qauxv.util.hookimpl.lsplant.LsplantHookImpl
import io.github.qauxv.util.xpcompat.XposedBridge
import net.bytebuddy.ByteBuddy
import net.bytebuddy.android.AndroidClassLoadingStrategy
import net.bytebuddy.implementation.FixedValue
import net.bytebuddy.matcher.ElementMatchers
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.lang.reflect.InvocationTargetException
import java.util.zip.ZipFile

/**
 *   CI/ADB invocation:
 *
 *   ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
 *
 *   adb install -r app/build/outputs/apk/debug/app-debug.apk
 *   adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
 *
 *   adb shell am instrument -w -r \
 *     -e class cc.ioctl.fragment.DebugTestFragmentInstrumentedTest#checkForConflictClassForHost \
 *     io.github.qauxv.test/androidx.test.runner.AndroidJUnitRunner
 *
 *   adb shell am instrument -w -r \
 *     -e class cc.ioctl.fragment.DebugTestFragmentInstrumentedTest#runXposedHookTests \
 *     io.github.qauxv.test/androidx.test.runner.AndroidJUnitRunner
 *
 *  For debugging ART native crash, it is recommended to clear logcat (logcat -c) before running the test,
 *      and then filter logcat by "crash" keyword after the test to find the crash log.
 *       tag:QAuxv | level:assert | is:crash | tag:LSPlant
 *  e.g. 2026-06-09 23:58:59.163  3337-3337  libc  io.github.qauxv A  Fatal signal 11 (SIGSEGV), code 128 (SI_KERNEL),
 *       2026-06-09 23:58:59.421  3382-3382  DEBUG  crash_dump64   A  signal 11 (SIGSEGV), code 128 (SI_KERNEL), fault addr 0x0000000000000000
 */
object DebugTestRunner {

    data class RunResult(val success: Boolean, val output: String)

    private val hostPackageCandidates = arrayOf(
        "com.tencent.mobileqq",
        "com.tencent.tim",
        "com.tencent.mobileqqi",
        "com.tencent.minihd.qq",
        "com.tencent.qqlite",
    )

    fun runXposedHookTests(): RunResult {
        val result = StringBuilder()
        var success = true
        try {
            val klass = ByteBuddy()
                .subclass(TextClass::class.java)
                .method(ElementMatchers.named("getText"))
                .intercept(FixedValue.value("Hello from a fileless dex!"))
                .make()
                .load(TextClass::class.java.classLoader, AndroidClassLoadingStrategy.Wrapping())
                .loaded
            val textClass = klass.getDeclaredConstructor().newInstance()
            result.append('\n').append(textClass.getText())

            // init LSPlant for self test purpose
            if (StartupInfo.getHookBridge() == null) {
                LsplantHookImpl.initializeLsplantHookBridge()
            }
            val hookResult = performHookTest()
            result.append('\n').append(hookResult.output)
            success = hookResult.success
        } catch (e: Throwable) {
            val err = unwrapInvocationTargetException(e)
            Log.e(e)
            result.append('\n').append(Log.getStackTraceString(err))
            success = false
        }
        return RunResult(success, result.toString())
    }

    fun checkForConflictClassForHost(
        context: Context,
        appendText: (CharSequence) -> Unit = {},
    ): RunResult {
        val output = StringBuilder()
        var success = true

        fun report(text: CharSequence) {
            if (text.isEmpty()) return
            if (output.isNotEmpty() && output.last() != '\n') {
                output.append('\n')
            }
            output.append(text)
            if (!text.endsWith("\n")) {
                output.append('\n')
            }
            appendText(text)
        }

        val ctx = context.applicationContext
        val hostPaths = ArrayList<String>()
        val selfPath = StartupInfo.getModulePath()
        val inHostProcess = try {
            StartupInfo.isInHostProcess()
        } catch (e: IllegalStateException) {
            false
        }
        if (inHostProcess) {
            hostPaths.add(ctx.packageCodePath)
        } else {
            val pms = ctx.packageManager
            for (pkg in hostPackageCandidates) {
                try {
                    val pi = pms.getPackageInfo(pkg, 0)
                    hostPaths.add(pi.applicationInfo!!.sourceDir)
                } catch (e: Exception) {
                    // not installed
                }
            }
        }
        if (hostPaths.isEmpty()) {
            report("No host app found to check")
            return RunResult(true, output.toString())
        }
        for (hostPath in hostPaths) {
            try {
                success = checkForConflictClassForSingleHostPackage(selfPath, hostPath, ::report) && success
            } catch (e: Throwable) {
                val err = unwrapInvocationTargetException(e)
                Log.e(e)
                report("Error checking host package $hostPath:\n" + Log.getStackTraceString(err))
                success = false
            }
        }
        return RunResult(success, output.toString())
    }

    private fun checkForConflictClassForSingleHostPackage(
        selfPath: String,
        hostPackage: String,
        report: (CharSequence) -> Unit,
    ): Boolean {
        var success = true

        report("\n\n====== Checking host package: $hostPackage ======")

        val moduleClasses = HashSet<String>()
        ZipFile(selfPath).use { apk ->
            var i = 1
            while (true) {
                val name = if (i == 1) "classes.dex" else "classes${i}.dex"
                val entry = apk.getEntry(name) ?: break
                apk.getInputStream(entry).use { ins ->
                    val classes = getDeclaredClassesFromDex(readAll(ins))
                    moduleClasses.addAll(classes)
                    report("Module $name has ${classes.size} classes")
                }
                i++
            }
        }
        report("Load module classes done")

        val hostBasePath = File(hostPackage)
        val hostClasses = HashSet<String>(10000)
        if (hostBasePath.isDirectory) {
            var j = 1
            while (true) {
                val name = if (j == 1) "classes.dex" else "classes${j}.dex"
                val dexFile = File(hostBasePath, name)
                if (!dexFile.exists()) break
                FileInputStream(dexFile).use { fis ->
                    val dex = readAll(fis)
                    val dexClasses = getDeclaredClassesFromDex(dex)
                    hostClasses.addAll(dexClasses)
                    report("Host $name has ${dexClasses.size} classes.")
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
                        report("Host $name has ${classes.size} classes")
                    }
                    j++
                }
            }
        }
        report("Load host classes done")

        val hostPackages = getClassPackages(hostClasses)
        val modulePackages = getClassPackages(moduleClasses)

        val hostDefaultClasses = getDefaultClasses(hostClasses)
        val moduleDefaultClasses = getDefaultClasses(moduleClasses)

        report("Host has ${hostClasses.size} classes, in ${hostPackages.size} packages.")
        report("Module has ${moduleClasses.size} classes, in ${modulePackages.size} packages.")
        val overlaps = HashSet<String>(384)
        for (s in modulePackages) {
            if (hostPackages.contains(s)) {
                overlaps.add(s)
            }
        }
        report("Overlapping package count: ${overlaps.size}")
        val packageConflicts = ArrayList<String>(128)
        for (s in overlaps) {
            if (!HybridClassLoader.isConflictingClass("$s.")) {
                packageConflicts.add(s)
            }
        }
        packageConflicts.sort()
        report("Conflicting package count: ${packageConflicts.size}")
        if (packageConflicts.isNotEmpty()) {
            success = false
        }
        for (s in packageConflicts) {
            report(s)
        }

        overlaps.clear()

        report("Host has ${hostDefaultClasses.size} default classes.")
        report("Module has ${moduleDefaultClasses.size} default classes.")
        for (s in moduleDefaultClasses) {
            if (hostDefaultClasses.contains(s)) {
                overlaps.add(s)
            }
        }
        report("Overlapping default class count: ${overlaps.size}")
        val defaultClassConflicts = ArrayList<String>(128)
        for (s in overlaps) {
            if (!HybridClassLoader.isConflictingClass(s)) {
                defaultClassConflicts.add(s)
            }
        }
        defaultClassConflicts.sort()
        report("Conflicting default class count: ${defaultClassConflicts.size}")
        if (defaultClassConflicts.isNotEmpty()) {
            success = false
        }
        for (s in defaultClassConflicts) {
            report(s)
        }

        report("====== Check finished for host package: $hostPackage ======\n\n")
        return success
    }

    private fun readAll(input: InputStream): ByteArray {
        val baos = ByteArrayOutputStream()
        val buf = ByteArray(4096)
        while (true) {
            val n = input.read(buf)
            if (n <= 0) break
            baos.write(buf, 0, n)
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

    abstract class TextClass {
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
            return if (Math.random() * Math.random() < 2) {
                test4(s, i, d, f, j)
            } else {
                "Math.random() < 2"
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

    private fun performHookTest(): RunResult {
        val ps = "S" + Math.random()
        val pi = (Math.random() * 10000f).toInt()
        val pd = (Math.random() * 10000f).toDouble()
        val pf = (Math.random() * 10000f).toFloat()
        val pj = (Math.random() * 10000f).toLong()
        val pobj1 = TestCase1()
        val stat = StringBuilder("\n")
        var success = true
        val test1 =
            TestCase1::class.java.getDeclaredMethod("test1", String::class.java, Int::class.java, Double::class.java, Float::class.java, Long::class.java)
        stat.append(test1)
        stat.append('\n')
        val test2 =
            TestCase2::class.java.getDeclaredMethod("test2", String::class.java, Int::class.java, Double::class.java, Float::class.java, Long::class.java)
        stat.append(test2)
        stat.append('\n')
        val test3 = Test3::class.java.getDeclaredConstructor(String::class.java, Int::class.java, Double::class.java, Float::class.java, Long::class.java)
        stat.append(test3)
        stat.append('\n')
        val test4 =
            TestCase2::class.java.getDeclaredMethod("test4", String::class.java, Int::class.java, Double::class.java, Float::class.java, Long::class.java)
        stat.append(test4)
        stat.append('\n')

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

        val ret1 = pobj1.test1(ps, pi, pd, pf, pj)
        val ret2 = TestCase2.test2(ps, pi, pd, pf, pj)
        val ret3 = Test3(ps, pi, pd, pf, pj)
        val ret4 = TestCase2.callTest4(ps, pi, pd, pf, pj)
        val expected1 = "a" + pobj1 + ps + pi + pd + pf + pj
        val expected2 = "b" + ps + pi + pd + pf + pj
        val expected3 = "c" + ret3 + ps + pi + pd + pf + pj
        val expected4 = "d" + TestCase2 + ps + pi + pd + pf + pj

        stat.append('\n')
        if (ret1 != expected1) {
            stat.append("Hook test1 FAILED: $ret1 != $expected1")
            success = false
        } else {
            stat.append("Hook test1 success: $ret1")
        }
        stat.append('\n')
        if (ret2 != expected2) {
            stat.append("Hook test2 FAILED: $ret2 != $expected2")
            success = false
        } else {
            stat.append("Hook test2 success: $ret2")
        }
        stat.append('\n')
        if (ret3.r != expected3) {
            stat.append("Hook test3 FAILED: ${ret3.r} != $expected3")
            success = false
        } else {
            stat.append("Hook test3 success: ${ret3.r}")
        }
        stat.append('\n')
        if (ret4 != expected4) {
            stat.append("Hook test4 FAILED: $ret4 != $expected4")
            success = false
        } else {
            stat.append("Hook test4 success: $ret4")
        }

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
        if (orig1.startsWith("NG") && orig2.startsWith("NG") && orig3.startsWith("NG") && orig4.startsWith("NG")) {
            stat.append('\n')
            stat.append("Call origin method success")
        } else {
            stat.append('\n')
            stat.append("Call origin method FAILED")
            success = false
        }

        return RunResult(success, stat.toString())
    }

    private fun unwrapInvocationTargetException(e: Throwable): Throwable {
        return if (e is InvocationTargetException) e.targetException else e
    }
}
