-keep class me.** { *; }
-keep class bsh.** { *; }
-keep class de.** { *; }
-keep class cc.** { *; }
-keep class io.** { *; }
-keep class org.** { *; }
-keep class com.microsoft.** { *; }
-keep class com.rymmmmm.** { *; }
-keep class com.hicore.** { *; }
-keep class cn.lliiooll.** { *; }
-keep class xyz.nextalone.** { *; }
-keep class cc.ioctl.** { *; }
-keep class sakura.kooi.** { *; }
-keep class com.tencent.mmkv.** { *; }

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
-dontoptimize

-dontwarn javax.**
-dontwarn java.awt.**
-dontwarn java.lang.instrument.ClassFileTransformer
-dontwarn sun.misc.SignalHandler
-dontwarn org.apache.bsf.util.BSFEngineImpl
-dontwarn java.applet.Applet
-dontwarn org.apache.bsf.*
