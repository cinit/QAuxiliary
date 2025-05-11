# 使用 Frida 加载 QAuxiliary 模块

QAuxiliary 支持在没有 Xposed 环境的 Android 系统上通过 Frida 加载模块。

## 环境需求

- Android >= 7.0
- QAuxiliary >= 1.5.8
- 宿主 QQ/TIM
- root (Magisk/KernelSU/APatch etc.)
- Frida

## 操作方法

1. 启动 Frida Server
   请参考 [Frida 官方文档](https://frida.re/docs/android/)。
2. 启动 QQ/TIM
   手动从 launcher 点开或者 am start 都可以。
3. 使用 Frida 注入目标宿主进程，使用 `Java.use` 调用 QAuxiliary 的 Frida 入口点 `io.github.qauxv.loader.sbl.frida.FridaInjectEntry#entry1(String)` 。
   其中 entry1 方法需要 modulePath 作为参数，需要传入模块 apk 路径，如 `/data/app/io.github.qauxv-1/base.apk`。
   可以参考本文末尾的 JavaScript 脚本。

# 参考脚本

```javascript
Java.perform(() => {
    const ActivityThread = Java.use("android.app.ActivityThread");
    const activityThread = ActivityThread.currentActivityThread();
    const ctx = activityThread.getApplication();
    console.log("ctx:", ctx);
    const ai = ctx.getPackageManager().getPackageInfo("io.github.qauxv", 0).applicationInfo.value;
    const modulePath = ai.sourceDir.value;
    console.log("applicationInfo:", ai);
    console.log("modulePath:", modulePath);
    // Function to hook is defined here
    const BaseDexClassLoader = Java.use("dalvik.system.BaseDexClassLoader");
    const StringClass = Java.use("java.lang.Class").forName("java.lang.String");
    // public BaseDexClassLoader(String dexPath, File optimizedDirectory, String librarySearchPath, ClassLoader parent);
    // create a new instance of BaseDexClassLoader
    const parent = Java.use("java.lang.Class").forName("java.lang.Class").getClassLoader();
    const cl = BaseDexClassLoader.$new(modulePath, null, null, parent);
    console.log("BaseDexClassLoader:", cl);
    // load io.github.qauxv.loader.sbl.frida.FridaInjectEntry
    const FridaInjectEntry = cl.loadClass("io.github.qauxv.loader.sbl.frida.FridaInjectEntry");
    console.log("FridaInjectEntry:", FridaInjectEntry);
    // io.github.qauxv.loader.sbl.frida.FridaInjectEntry#entry1(String modulePath)
    const entry1 = FridaInjectEntry.getMethod("entry1", Java.array("java.lang.Class", [StringClass]));
    console.log("entry1:", entry1);
    entry1.invoke(null, Java.array("java.lang.String", [modulePath]));
});
```
