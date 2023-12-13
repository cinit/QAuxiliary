# 贡献指南

**首先，欢迎您为 QAuxiliary 这个项目做出贡献。**

## 写在前面

本项目由若干个功能组成，在大多数情况下，每个功能都是独立的，但是也有一些功能是依赖于其他功能的。
总体来说，每个功能都是由使用它的开发者维护的。这意味着，存在一些功能，由于没有开发者使用，而无人维护。
这很正常，本项目的初衷就是通过分享自己已经开发的功能，来省去其他有相同需求的人的重复劳动，而不是追求功能的多少。

由于本项目的特殊性，我并不推荐开发者在本项目上花费过多的时间，开发者只需要让自己需要的功能正常运行即可。
至于那些你自己都不用的功能，我并不推荐你去维护它，让真正需要它的人去维护就行了。

大可把开源项目当作一个并不重要的爱好，心情好就玩一玩，不想做了就不做，玩腻了就丢一边，没有人有资格去要求你做什么。
取悦自己，而不是为了没用的胜负欲去取悦别人。

## 如何添加一个新的功能

1. 如果你是第一次提交代码，先选一个 package, 通常是反写的域名加上一些其他信息，如 `com.example.hook`.
   然后将这个 package 加入到 [proguard-rules.pro](../app/proguard-rules.pro) 中，以防止被 R8 优化掉。
2. 在 package 下新建一个类，一个功能一个类，类名应该是一个名词，如 `RemoveShakeAdExampleHook`, 按下面的模板编写功能代码。
   写了类后，它就会自动被注册到功能列表中，如果用户启用了这个功能，那么这个类的 initOnce 方法将会自动调用。
3. 测试功能，记得去模块里打开你写的功能。由于 Android Studio 在 Android 11+ 默认启用部署优化(deployment optimization)，
   使用 JVMTI(ARTTI) 来热加载代码，这会导致实际并没有更新 apk，因此需要在 Android Studio 中禁用部署优化。
   在 `Run` 菜单中选择 `Edit Configurations...`，在 `Android App` 标签页中，选择 `QAuxiliary.app`，
   在 `General` 标签页中，将 `Install Options` 中的 `Always install with package manager (disable install optimization)` 勾选上。
   你也可以通过直接运行 `:app:installDebug` task 来安装应用，而不是通过 Android Studio 运行 app。

## 功能模板

如果你倾向于使用 Kotlin 来编写功能，可以参考以下模板.

```kotlin
package com.example.hook

import cc.ioctl.util.hookBeforeIfEnabled
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion

// FunctionHookEntry 和 UiItemAgentEntry 用于注册功能，这两个注解都是必要的
// 注意是 object，而不是 class
@FunctionHookEntry
@UiItemAgentEntry
object RemoveShakeAdExampleHook : CommonSwitchFunctionHook() {

    override val name = "这里写功能名字"

    override val description = "这里写功能描述"

    override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.这里写功能位置

    // isAvailable 可选，可以不写
    override val isAvailable: Boolean get() = requireMinQQVersion(QQVersion.最低支持版本)

    override fun initOnce(): Boolean {
        // 在这里写功能初始化代码，如果用户启用了这个功能，那么这里的代码将会被执行一次
        // initOnce 里可以随便 throw 异常, throw 的异常会显示在日志和故障排除方便定位问题
        val klass = Initiator.loadClass("com.tencent.mobileqq.example.SomeClassManager")
        // Initiator.loadClass 用于加载 QQ 的类，如果类不存在，将会 throw ClassNotFoundException
        val someMethod = kTroopInfo.getDeclaredMethod("someMethod", Long::class.java)
        // hookBeforeIfEnabled 只有在用户启用了这个功能的情况下才会执行 hook 回调
        // 避免使用 XposedBridge 和 XposedHelpers，因为这些类在 new Xposed API 中不复存在
        hookBeforeIfEnabled(someMethod) {
            // 做一些操作，其中 it 是一个 HookParam 对象，可以通过 it.args 获取方法参数
            it.result = 0L
        }
        // return true 表示初始化成功，false 或者抛异常表示初始化失败
        return true
    }
}
```

如果你倾向于使用 Java 来编写功能，可以参考以下模板.

```java
package com.example.hook;

import cc.ioctl.util.hookBeforeIfEnabled;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.QQVersion;
import io.github.qauxv.util.requireMinQQVersion;

// FunctionHookEntry 和 UiItemAgentEntry 用于注册功能，这两个注解都是必要的
@FunctionHookEntry
@UiItemAgentEntry
public final class RemoveShakeAdExampleHook extends CommonSwitchFunctionHook {

    // INSTANCE 是必须的，因为 Java 没有 object，所以我们需要一个单例
    public static final RemoveShakeAdExampleHook INSTANCE = new RemoveShakeAdExampleHook();

    @Override
    public String getName() {
        return "这里写功能名字";
    }

    @Override
    public String getDescription() {
        return "这里写功能描述";
    }

    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.这里写功能位置;
    }

    // isAvailable 可选，可以不写
    @Override
    public boolean isAvailable() {
        return requireMinQQVersion(QQVersion.最低支持版本);
    }

    @Override
    public boolean initOnce() throws Exception {
        // 在这里写功能初始化代码，如果用户启用了这个功能，那么这里的代码将会被执行一次
        // initOnce 里可以随便 throw 异常, throw 的异常会显示在日志和故障排除方便定位问题
        Class<?> klass = Initiator.loadClass("com.tencent.mobileqq.example.SomeClassManager");
        // Initiator.loadClass 用于加载 QQ 的类，如果类不存在，将会 throw ClassNotFoundException
        Method someMethod = kTroopInfo.getDeclaredMethod("someMethod", Long.class);
        // hookBeforeIfEnabled 只有在用户启用了这个功能的情况下才会执行 hook 回调
        // 避免使用 XposedBridge 和 XposedHelpers，因为这些类在 new Xposed API 中不复存在
        HookUtils.hookBeforeIfEnabled(this, someMethod, param -> {
            // 做一些操作，其中 param 是一个 HookParam 对象，可以通过 param.args 获取方法参数
            param.setResult(0L);
        });
        // return true 表示初始化成功，false 或者抛异常表示初始化失败
        return true;
    }
}
```

## Commit 相关

1. 类名、变量名、方法名禁止中文/拼音(例外: 如果 QQ 的 API 本身就是拼音的, 那么就只能用拼音了)
2. 简洁明了
3. 一个 commit 做一件事情
4. 请勿在 commit 附上任何有关 [skip ci] 的字段
5. 在 commit 之前请先更新到最新的 main 分支, 以方便我们进行快速合并(fast-forward merge).
6. 每个 commit 都必须附着有效的GPG签名，如果您不知道如何使用 GPG 签名，请参阅
   [这里](https://docs.github.com/cn/github/authenticating-to-github/managing-commit-signature-verification).
   如果你实在不会配置 GPG 签名，你仍然可以提交 PR, 但由于 main 分支要求所有 commit 必须附着有效的 GPG 签名，就只好由我们来代替你签名了.

## Pull Request

1. 请勿在修改会被编译分发至用户的部分时在 PR 标题添加 [skip ci]；请务必在文档、模板等不会影响编译流程和实际分发的目标生成，或完全无法编译但出于必要目的必须提交的 PR 标题添加 [skip ci]

## 注意事项

1. 请确认您的编辑器支持 EditorConfig，否则请注意您的编码、行位序列等事项。

2. 代码风格建议遵循 [Google Java Style](https://google.github.io/styleguide/javaguide.html) [中文翻译](https://github.com/fantasticmao/google-java-style-guide-zh_cn)

3. 每位开发者自己的代码风格应保持一致

4. 以 UTF-8 编码，以 LF 作为行位序列

5. 命名方面: 禁止拼音 (参考上面的例外)

6. 使用 4 个空格缩进 (Java/Kotlin/C++)

7. 弃用或注释的代码应删除，若需重复使用请翻阅 `git log`

8. 大括号放应同一行上 (Java/Kotlin/C++)

9. 代码请务必格式化

10. 将自己的代码放在自己的包里，另外，应注意的是，如果你创建了自己的包，**一定要记得修改 [proguard-rules.pro](app/proguard-rules.pro)**

11. 除例外情况外，原则上要求添加代码头 (例外情况: 反编译的代码、自动生成的代码、非贡献者编写的代码、由于文件格式或其它原因不适合添加代码头等)

## 其他

如还有疑问，可直接在 Telegram 群聊询问
