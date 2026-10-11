
-dontwarn javax.**
-dontwarn java.awt.**
-dontwarn org.apache.bsf.*

# ByteBuddy混淆
-dontwarn com.sun.jna.**
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn java.lang.instrument.**

# Xposed API
-dontwarn de.robv.android.xposed.**
-dontwarn io.github.libxposed.api.**

-keepattributes LineNumberTable,SourceFile

-dontoptimize
-dontobfuscate

# AGP 9.0.0 R8 bug
# https://issuetracker.google.com/issues/470510982
# replace '-dontshrink' with the following line for TEMPORARY WORKAROUND
-dontshrink
# XX: diable workaround, we use "isMinifyEnabled = false" now.
#-keep class * { *; }
# Change the above line back to '-dontshrink' when AGP 9.0.0 R8 bug is fixed.
# Notice: AGP 9.0.0 R8 creates method outline even with '-dontoptimize' option.
#         This may lead to incorrect behavior in some cases.
#         e.g. "org.jf.util.Hex$$ExternalSyntheticBUOutline0"
# End of AGP 9.0.0 R8 bug workaround
