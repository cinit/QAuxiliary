<div align="center">
    <h1> QAuxiliary </h1>

[![GitHub release](https://img.shields.io/github/release/cinit/QAuxiliary.svg)](https://github.com/cinit/QAuxiliary/releases/latest)
[![main](https://github.com/cinit/QAuxiliary/actions/workflows/push_ci.yml/badge.svg)](https://github.com/cinit/QAuxiliary/actions/workflows/push_ci.yml)
[![Telegram](https://img.shields.io/static/v1?label=Telegram&message=Channel&color=0088cc)](https://t.me/QAuxiliary)
[![Telegram](https://img.shields.io/static/v1?label=Telegram&message=CI&color=0088cc)](https://t.me/QAuxiliary_CI)
[![Telegram](https://img.shields.io/static/v1?label=Telegram&message=Chat&color=0088cc)](https://t.me/QAuxiliaryChat)

</div>

---

QAuxiliary 是一个基于 QNotified 的开源 Xposed 模块

## 使用方法

激活本模块后，在 QQ 或者 TIM 自带设置中点击 QAuxiliary 即可进入设置页面

- Android >= 7.0
- QQ >= 8.2.0, TIM >= 2.2.0, QQLite >= 4.0, QQ HD >= 5.9.3
- 在没有 Xposed 环境的系统上，QAuxiliary 也可以通过 Frida 来加载 (需要 root)，参考 [instruction-qauxv-frida.md](./docs/instruction-qauxv-frida.md).

## 一切开发旨在学习，请勿用于非法用途

- 本项目保证永久开源，欢迎提交 PR，但是请不要提交用于非法用途的功能。
- 如果某功能被大量运用于非法用途，或对其他用户的正常使用造成严重影响，那么该功能将会被移除。
- 本模块完全免费开源，没有任何收费，请勿二次贩卖。
- 鉴于项目的特殊性，开发团队可能在任何时间**停止更新**或**删除项目**

### 许可证

- [EULA](https://github.com/qwq233/License/blob/master/v2/LICENSE.md)

```
版权所有©2022 gao_cai_sheng <qwq233@qwq2333.top, qwq2333.top>

允许在遵守 CC BY-NC-SA 4.0 协议的同时，复制和分发此协议文档的逐字记录副本，
且允许对其进行更改，但必须保留其版权信息与原作者。如果您提出申请特殊权限，协议
作者可在其口头或书面授予任何人任何但不包括以盈利为目的的使用本协议的权利。

请务必仔细阅读和理解通用许可协议书中规定的所有权利和限制。在使用前，您需要仔细
阅读并决定接受或不接受本协议的条款。除非或直至您接受本协议的条款，否则本作品及
其相关副本、相关程序代码或相关资源不得在您的任何终端上下载、安装或使用。

您一旦下载、使用本作品及其相关副本、相关程序代码或相关资源，即表示您同意接受本
协议各项条款的约束。如您不同意本协议中的条款，您则应当立即删除本作品、附属资源
及其相关源代码。

本作品权利只许可使用，而不出售。
```

## 发行渠道说明

<details>

QAuxiliary 采用滚动更新方式发布新版本，我们总是推荐用户使用最新版 QAuxiliary，无论您的 QQ 或者 TIM 客户端是哪个版本。

QAuxiliary 将为分 `CI` 和 `推荐的CI` 两个版本

- `CI` 版本为 commit 后自动触发更新，可能包含外围文档或 CI 流程更新，不会编写任何更新文档或说明，
  具体更新内容可在[GitHub](https://github.com/cinit/QAuxiliary/commits/master)
  自行查看，本更新由开源的流程自动编译发布，可能包含严重的功能及行为异常。

- `推荐的CI` 版本为重大功能变更或长期积累更新，发布频率由开发组决定，包含上次`CI`
  版至今的所有功能更新及 Bug 修复，但可能不包括尚未稳定或正在开发中的功能；
  `推荐的CI` 版本是被挑选出的推荐用户更新的 `CI` 版本 (如：添加功能或者修复重要 Bug)

开发组不限制用户选择自己需要的版本，同时也不为任何版本产生的任何后果承担任何责任
（详情请见[QAuxiliary EULA](https://github.com/cinit/QAuxiliary/blob/master/app/src/main/assets/eula.md)），
但希望各位用户各取所需，根据自己的能力范围选择适合自己的版本。

- QAuxiliary 的版本号组成为`major.minor.bugfix.rev.commit`
- 其中 major 为 主版本号，minor 为 次版本号，bugfix 为修正版本号；
- 所有版本更新的`rev`为 commit 计数，`commit` 位都会是触发此次更新的 commit 的 hash 的前 7 位。

1. [![Telegram](https://img.shields.io/static/v1?label=Telegram&message=QAuxiliary频道&color=0088cc)](https://t.me/QAuxiliary) 将只发布 `推荐的CI` 版更新。

2. [![Telegram](https://img.shields.io/static/v1?label=Telegram&message=QAuxiliary_CI频道&color=0088cc)](https://t.me/QAuxiliary_CI) 发布 `CI` 版更新。

3. [![GitHub release](https://img.shields.io/github/release/cinit/QAuxiliary.svg)](https://github.com/cinit/QAuxiliary/releases/latest) 将只发布 `推荐的CI` 版更新。

4. [![](https://img.shields.io/badge/LSPosed-ClickMe-blue?link=https://github.com/Xposed-Modules-Repo/io.github.qauxv/releases/)](https://github.com/Xposed-Modules-Repo/io.github.qauxv/releases/) 将只发布 `推荐的CI` 版更新。

5. 为什么没有上架 Google Play?  
   因为 Google Play 不允许 app 具有运行时动态加载外部代码的行为。
   而本模块为了能够在运行期动态继承宿主的类（类名被混淆，运行时才能确定），使用了 [byte-buddy](https://github.com/raphw/byte-buddy)
   库用于在运行时动态生成代码并使用 InMemoryDexClassLoader 实现 dex 不落地加载，这是 Google Play 不允许的行为。

</details>

## 不会支持的功能

- 抢红包及其他金钱相关功能
- 修改聊天记录等可能被恶意利用的功能
- 群发消息
- 可能干扰其他用户正常使用的功能（如闪退）

## 编译

1. 安装 git, Ninja 1.11+, JDK 17+, Android SDK, 以及可选的 ccache;
    - Android SDK 和 NDK 及 CMake 版本请参考 [Version.kt](build-logic/convention/src/main/kotlin/Version.kt);
    - JDK 版本最低 17, 当然使用 21 也是可以的。
    - Ninja 请使用 1.11 或更高版本。因为 Ninja 从 1.11 开始支持 C++ 20 module, 你可以从 [Ninja Release](https://github.com/ninja-build/ninja/releases) 下载
      Ninja 二进制文件, 并在 `local.properties` 中指定 `qauxv.override.ninja.path` 为 Ninja 的路径 (如 `qauxv.override.ninja.path=/usr/bin/ninja`).
    - Ccache 是可选的，不装也可以，但它可以让编译更快。  
      注意: 编译脚本会自动寻找 ccache 并使用，而 Windows 平台下 msys2 的 ccache 存在问题会卡在 sync 阶段，
      建议 Windows 用户使用从 ccache 官网下载的 ccache 而不是 msys2 的 ccache;  
      另外你也可以选择不使用 ccache (如果你已经安装了 ccache 但不想使用，可以修改 [build.gradle.kts](app/build.gradle.kts)
      中的 `ccacheExecutablePath` 为 `null`)
2. 将本仓库 clone 至本地；由于本项目使用的 submodule 含有一些不需要的以及非公开的二级 submodule, 请参考以下命令 clone 本项目以跳过这些 submodule:
   ```shell
   git clone https://github.com/cinit/QAuxiliary
   cd QAuxiliary
   git submodule update --init
   git -C "libs/LSPlant" config "submodule.test/src/main/jni/external/lsprism.update" none
   git -C "libs/LSPlant" config "submodule.test/src/main/jni/external/lsparself.update" none
   git -C "libs/LSPlant" config "submodule.docs/doxygen-awesome-css.update" none
   git -C "libs/mmkv/MMKV" config "submodule.Python/pybind11.update" none
   git submodule foreach git submodule update --init --recursive
   ```
   去除不使用的 submodule 后，一共有 14 个 submodule, 如果 clone 了很久也没有完成，请检查您的网络以及代理是否配置正确。
3. 使用 Gradle 编译安装包: `./gradlew :app:assembleDebug` 或者 `./gradlew :app:synthesizeDistReleaseApksCI`;

本项目编译环境配置可能比较复杂，如果您在配置环境或者编译过程中遇到任何问题，您可以直接在交流群或 GitHub Issue 中提出，我们会尽快回复。

---

## 赞助

- 由于项目的特殊性，我们不接受任何形式的捐赠，但是我们希望有更多的人能够参与本项目的开发
- 如果您有兴趣参与本项目的开发，您可以参考[贡献指南](.github/CONTRIBUTING.md)，其中包含了一些可以帮助您快速上手的信息

## [通用许可协议](https://github.com/qwq233/License/blob/master/v2/LICENSE.md)
