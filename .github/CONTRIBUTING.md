# 贡献指南

**首先，欢迎您为QAuxiliary这个项目做出贡献。**

## 分支约定

不管是直接 Push 代码还是提交 Pull Request，都必须使 commit 指向 dev 分支。

## Commit 相关

1. 禁止中文/拼音
2. 简洁明了
3. 一个commit做一件事情
4. 请勿在commit附上任何有关[skip ci]的字段
5. 每个commit都必须附着有效的GPG签名

## Pull Request

1. 请勿在修改会被编译分发至用户的部分时在PR标题添加[skip ci]；请务必在文档、模板等不会影响编译流程和实际分发的目标生成，或完全无法编译但出于必要目的必须提交的PR标题添加[skip ci]

## 开发

1. 请确认您的编辑器支持EditorConfig，否则请注意您的编码、行位序列及其其他事项。

2. 在原则上代码风格遵循[Google Java Style](https://google.github.io/styleguide/javaguide.html)[中文翻译](https://github.com/fantasticmao/google-java-style-guide-zh_cn)

3. 每位开发者的代码风格应保持一致

4. 以UTF-8编码，以LF作为行位序列

5. 命名方面应
    1. 禁止拼音

    2. 使用大写字母分隔单词
6. 使用4个空格缩进

7. 弃用或注释的代码应删除，若需重复使用请翻阅git log

8. 大括号放应同一行上

9. 代码请务必格式化

10. 将自己的代码放在自己的包里，，另外，应注意的是，如果你创建了自己的包，**一定要记得修改[proguard-rules.pro](app/proguard-rules.pro)**

11. 除个别情况，必须添加代码头

12. **在任何时候，您都不应该随意更改[build.gradle.kts](build.gradle.kts)，特别是升级 `com.android.tools.build:gradle` 版本**

## 其他

如还有疑问，可直接在Telegram群聊询问
