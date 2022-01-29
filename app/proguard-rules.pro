-keep class nil.** { *; }
-keep class me.** { *; }
-keep class bsh.** { *; }
-keep class de.** { *; }
-keep class cc.** { *; }
-keep class io.** { *; }
-keep class org.** { *; }
-keep class com.microsoft.** { *; }
-keep class com.rymmmmm.** { *; }
-keep class cn.lliiooll.** { *; }
-keep class xyz.nextalone.** { *; }
-keep class cc.ioctl.** { *; }
-keep class com.tencent.mmkv.** { *; }

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
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
