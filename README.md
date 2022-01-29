<div align="center">
    <h1> QAuxiliary </h1>

[![license](https://img.shields.io/github/license/cinit/QAuxiliary.svg)](https://www.gnu.org/licenses/agpl-3.0.html)
[![GitHub release](https://img.shields.io/github/release/cinit/QAuxiliary.svg)](https://github.com/cinit/QAuxiliary/releases/latest)
[![Telegram](https://img.shields.io/static/v1?label=Telegram&message=Channel&color=0088cc)](https://t.me/QAuxiliary)
[![Telegram](https://img.shields.io/static/v1?label=Telegram&message=CI&color=0088cc)](https://t.me/QAuxiliary_CI)
[![Telegram](https://img.shields.io/static/v1?label=Telegram&message=Chat&color=0088cc)](https://t.me/QAuxiliaryChat)
</div>

-----

QAuxiliary 是一个基于 QNotified 的开源 Xposed 模块

## 使用方法

激活本模块后，在 QQ 或者 TIM 自带设置中点击 QAuxiliary 即可进入设置页面

- Android >= 5.0
- QQ >= 7.0.0, TIM >= 2.2.0

## 一切开发旨在学习，请勿用于非法用途

- 本项目保证永久开源，欢迎提交PR，但是请不要提交用于非法用途的功能。
- 如果某功能被大量运用于非法用途或严重侵害插件使用者权益，那么该功能将会被移除。
- 本模块完全免费开源，没有任何收费，请勿二次贩卖。
- 鉴于项目的特殊性，开发团队可能在任何时间**停止更新**或**删除项目**

### 许可证

- [AGPL-3.0](https://www.gnu.org/licenses/agpl-3.0.html)

```
Copyright (C) 2019-2022 qwq233@qwq2333.top

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
```

- [EULA](https://github.com/cinit/QAuxiliary/blob/master/app/src/main/assets/eula.md)

```
版权所有©2022 gao_cai_sheng <qwq233@qwq2333.top, qwq2333.top>

允许在其遵守CC BY-NC-SA 4.0协议的同时，每个人复制和分发此许可证文档的逐字记录副本，且允许对其进行更改，但必须保留其版权信息与原作者。
请务必仔细阅读和理解 QAuxiliary 最终用户许可协议中规定的所有权利和限制。在使用前，您需要仔细阅读并决定接受或不接受本协议的条款。除非或直至您接受本协议的条款，否则本软件及其相关副本、相关程序代码或相关资源不得在您的任何终端上下载、安装或使用。
您一旦下载、使用本软件及其相关副本、相关程序代码或相关资源，即表示您同意接受本协议各项条款的约束。如您不同意本协议中的条款，您则应当立即删除本软件、附属资源及其相关源代码。
本软件权利只许可使用，而不出售。
本协议与GNU Affero通用公共许可证(即AGPL协议)共同作为本软件与您的协议，且本协议与AGPL协议的冲突部分均按照本协议约束。您必须同时同意并遵守本协议与AGPL协议，否则，您应立即卸载、删除本软件、附属资源及其相关源代码。
```

## 发行渠道说明

<details>

QAuxiliary 采用滚动更新方式发布新版本，我们总是推荐用户使用最新版 QAuxiliary，无论您的 QQ 或者 TIM 客户端是哪个版本。

QAuxiliary 将为分 `CI` 和 `推荐的CI` 两个版本

- `CI` 版本为每commit自动更新，可能不包含外围文档或CI流程更新，不会编写任何更新文档或说明，
  具体更新内容可在[Github](https://github.com/cinit/QAuxiliary/commits/master)
  自行查看，本更新由开源的流程自动编译发布，可能包含严重的功能及行为异常。

- `推荐的CI` 版本为重大功能变更或长期积累更新，发布频率由开发组决定，包含上次`CI`
  版至今的所有功能更新及Bug修复，但可能不包括尚未稳定或正在开发中的功能；
  `推荐的CI` 版本是被挑选出的推荐用户更新的 `CI` 版本 (如：功能添加或者修复重要BUG)

开发组不限制用户选择自己需要的版本，同时也不为任何版本产生的任何后果承担任何责任
（详情请见[QAuxiliary EULA](https://github.com/cinit/QAuxiliary/blob/master/app/src/main/assets/eula.md)），
但希望各位用户各取所需，根据自己的能力范围选择适合自己的版本。

- QAuxiliary 的版本号组成为`major.minor.bugfix.rev.commit`
- 其中 major 为 主版本号，minor 为 次版本号，bugfix 为修正版本号；
- 所有版本更新的`rev`为 commit 计数，`commit` 位都会是触发此次更新的Commit的hash的前7位。

1. [![Telegram](https://img.shields.io/static/v1?label=Telegram&message=QAuxiliary频道&color=0088cc)](https://t.me/QAuxiliary) 将只发布 `推荐的CI` 版更新。

2. [![Telegram](https://img.shields.io/static/v1?label=Telegram&message=QAuxiliary_CI频道&color=0088cc)](https://t.me/QAuxiliary_CI) 发布 `CI` 版更新。

3. [![GitHub release](https://img.shields.io/github/release/cinit/QAuxiliary.svg)](https://github.com/cinit/QAuxiliary/releases/latest) 将只发布 `推荐的CI` 版更新。

4. ![](https://img.shields.io/badge/LSPosed-ClickMe-blue?link=https://github.com/Xposed-Modules-Repo/io.github.qauxv/releases/) 将发布所有版本更新，其中`CI`版本更新将被标注为 Pre-release。
</details>

-----

## 赞助

- 由于项目的特殊性，我们不接受任何形式的捐赠，但是我们希望有更多的人能够参与本项目的开发

## [QAuxiliary最终用户许可协议](https://github.com/cinit/QAuxiliary/blob/master/app/src/main/assets/eula.txt)
