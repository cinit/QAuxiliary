-keep class me.** { *; }
-keep class bsh.** { *; }
-keep class de.** { *; }
-keep class cc.** { *; }
-keep class io.** { *; }
-keep class org.** { *; }
-keep class com.microsoft.** { *; }
-keep class com.rymmmmm.** { *; }
-keep class com.codepwn.** { *; }
-keep class com.hicore.** { *; }
-keep class com.xiaoniu.** { *; }
-keep class cn.lliiooll.** { *; }
-keep class xyz.nextalone.** { *; }
-keep class cc.ioctl.** { *; }
-keep class sakura.kooi.** { *; }
-keep class moe.zapic.** { *; }
-keep class cc.microblock.** { *; }
-keep class com.tencent.mmkv.** { *; }
-keep class com.alphi.qhmk.** { *; }
-keep class top.linl.** { *; }
-keep class top.xunflash.** { *; }
-keep class io.github.moonleeeaf.** { *; }
-keep class io.github.fusumayuki.** { *; }
-keep class awoo.linwenxuan04.** { *; }

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-dontobfuscate

-dontwarn javax.**
-dontwarn java.awt.**
-dontwarn org.apache.bsf.*

-keepattributes LineNumberTable,SourceFile

# Keep `Companion` object fields of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# ByteBuddy混淆
-dontwarn com.sun.jna.**
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn java.lang.instrument.**
-keep class com.android.dx.** {
    *;
}
-keep class net.bytebuddy.** {
    *;
}

-dontoptimize
