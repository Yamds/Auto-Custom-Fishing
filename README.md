# Auto Custom Fishing for CustomFishing

## 说明

本项目代码来自 **100% AI 生成**。

这意味着：

- 代码能用，但实现未必优雅
- 工程结构和细节处理可能比较混乱
- 这个仓库更像是一个“可运行的思路验证 + 可继续改进的基础”

如果你是来读代码的，建议把它当成一个思路参考，不要预期它已经被人工精修过。

## 当前缺陷

这个项目目前最大的问题是：

**不能做到在收到正确 Title/Subtitle 信息的那一刻就立刻、稳定地完成收杆。**

实际表现是，理论上已经识别到了正确时机，但真正执行点击时仍然会有一点延迟。这个延迟原因我目前没有彻底定位清楚，所以现在采用的是另一种折中方案：

- 不直接依赖“看到正确 title 就马上点”
- 而是先计算浮漂 / 指针移动速度
- 再结合速度预测未来位置，提前一点点收杆

这个办法能覆盖大部分情况，但副作用也很明确：

- 仍然会偶尔漏鱼
- 某些 bar 或某些速度下需要人工调提前量

因此仓库里又做了一个 **提前量曲线编辑工具**，专门用来按不同 bar、不同速度调节提前点击量。默认参数已经能钓到大部分鱼，但不是百分之百。

如果你知道为什么“识别到正确时机后仍然不能无延迟收杆”，或者你能把这部分逻辑做得更稳定，欢迎直接提 Issue / PR。

## 项目简介

这是一个为 [Custom-Fishing](https://github.com/Xiao-MoMi/Custom-Fishing) 服务端插件编写的客户端自动钓鱼 Mod，运行在 `Minecraft 1.21.11 + Fabric`。

项目目标很直接：针对 Custom-Fishing 的“仿星露谷”钓鱼小游戏，把重复的抛竿、等待、识别小游戏、判断时机、收杆这一整套流程自动化。

当前版本只实现了河钓模式的自动化，不支持海钓模式。

## 这项目是做什么的

Custom-Fishing 的河钓并不是原版 Minecraft 那种“浮漂下沉就收杆”，而是在鱼咬钩后进入一个带指针和成功区间的小游戏。这个 Mod 做的事情是：

- 自动抛竿
- 监听鱼咬钩并自动收杆进入小游戏
- 解析服务器发来的 Title / Subtitle UI 数据
- 反推出当前指针所在位置和移动速度
- 在最合适的时机自动点击完成小游戏
- 完成后自动重新抛竿，进入下一轮

它本质上是一个“针对 Custom-Fishing 河钓 UI 协议做定制解析”的客户端自动化工具。

## 当前支持范围

- 支持：河钓 `accurate_click` 类小游戏
- 不支持：海钓 / tension 类玩法
- 不支持：通用服务器自动钓鱼
- 依赖：客户端必须能正常接收到 Custom-Fishing 发出的标题组件数据

## 核心逻辑

这个项目的核心不是图像识别，也不是读屏，而是直接分析客户端收到的界面组件数据。

整体流程如下：

1. `FishingHookMixin` 监听本地玩家鱼钩实体的 `biting` 状态，判断是否咬钩。
2. `TitlePacketMixin` 拦截 `Gui#setTitle`、`setSubtitle`、`setOverlayMessage`，把收到的文本交给 `TitleAnalyzer`。
3. `TitleAnalyzer` 从 `Component.toString()` 里解析 Custom-Fishing 使用的自定义字体字符和偏移字符。
4. 通过偏移量反推出指针进度，再映射成当前 bar 上的逻辑格子位置。
5. 记录最近一段时间的位置变化，计算平均速度。
6. 根据不同 bar 的成功区、当前速度、配置里的“提前量曲线”计算最佳点击时机。
7. `FishingController` 用一个小状态机驱动整套流程：抛竿、等咬钩、进入小游戏、点击、收杆、重抛。

简单说就是：

`咬钩检测 + 标题解析 + 位置测速 + 提前量预测 + 状态机控制`

## 主要功能

- 河钓全流程自动化
- 自动重抛
- 超时自动收杆并重新抛竿
- 基于不同 bar 类型分别配置提前量
- 支持全局提前量微调
- 内置曲线编辑器，可按速度调提前量
- 配置界面集成 Cloth Config / Mod Menu
- 记录最近 20 次点击日志，便于调参

## 使用方式

### 运行环境

- Minecraft: `1.21.11`
- Java: `21`
- Fabric Loader: `0.18.2+`
- Fabric API: `0.139.4+1.21.11`

### 安装

1. 编译项目，或使用已构建好的 jar。
2. 将 Mod 放入客户端 `mods` 目录。
3. 安装并进入启用了 Custom-Fishing 的服务器。
4. 手持钓鱼竿。
5. 按快捷键启用自动钓鱼。

### 默认快捷键

- `R`：开关自动钓鱼
- `O`：打开配置界面

如果安装了 Mod Menu，也可以直接从 Mod Menu 打开配置页。

## 配置说明

配置文件位于：

`config/autofishing.json`

当前主要配置项：

- `modEnabled`：总开关
- `globalAdvance`：全局提前量微调，作用于所有 bar
- `autoRecastMinutes`：抛竿后超时自动收杆的时间，单位分钟，设为 `0` 可关闭
- `barCurves`：每个 bar 的提前量曲线，按“速度(ms/section) -> 提前量”配置

默认思路是：

- 低难度 bar 提前量较小
- 中高难度 bar 提前量更积极
- 速度越快，通常越需要更早点击

## 项目结构

```text
src/main/java/com/autofishing/
  AutoFishingMod.java                Mod 入口

src/client/java/com/autofishing/
  AutoFishingClient.java             客户端入口
  fishing/FishingController.java     自动钓鱼状态机
  game/TitleAnalyzer.java            Custom-Fishing UI 解析器
  game/GameState.java                游戏状态与测速数据
  mixin/FishingHookMixin.java        鱼咬钩检测
  mixin/TitlePacketMixin.java        Title / Subtitle 拦截
  config/AutoFishConfig.java         配置加载与保存
  config/AutoFishConfigScreen.java   配置界面
  config/CurveEditorScreen.java      提前量曲线编辑器
  config/FishingLog.java             点击日志
```

## 适合谁看

如果你打开这个仓库是想快速理解代码，最值得先看的几个类是：

- `FishingController`
- `TitleAnalyzer`
- `GameState`
- `FishingHookMixin`
- `TitlePacketMixin`

这几个文件基本就把核心行为串起来了。

## 构建

```bash
./gradlew build
```

Windows:

```powershell
.\gradlew.bat build
```

构建产物默认在：

`build/libs/`

## 已知限制

- 当前只做了河钓自动化
- 逻辑强依赖 Custom-Fishing 当前的标题组件结构和自定义字体偏移规则
- 无法保证“识别到正确时机后立即零延迟收杆”，目前依赖速度预测 + 提前量补偿
- 如果服务端插件后续调整 UI 编码方式，这个 Mod 可能需要同步更新
- 这是一个高度定制项目，不保证对其他钓鱼插件兼容

## License

仓库当前沿用项目内已有的 `LICENSE` 文件。
