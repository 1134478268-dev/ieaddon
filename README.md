# IE Addon 工程（Immersive Engineering 附属模组）

一个面向 **沉浸工程（Immersive Engineering）** 的 addon 骨架，目标内容是
**新工具 / 新装备 / 新物品**，并提供 1.12.2 与 1.20.1 两套可编译工程。

## 目录结构

```
IEAddon/
├─ docs/
│  ├─ notes/                     ← 中文开发知识库（先读这里）
│  ├─ IE-src-1.12.2/             ← IE 0.12-89 源码（离线查 API）
│  ├─ IE-src-1.20.1/             ← IE 10.2.0 源码（含 src/api 223 个类）
│  └─ IE-src-1.21.1/             ← IE 12.x 源码（NeoForge）
├─ libs/                         ← 编译用 IE jar（不随 addon 分发）
└─ versions/
   ├─ 1.20.1/                    ← Forge 47.3.0 + Java 17（主力）
   └─ 1.12.2/                    ← Forge 14.23.5.2859 + Java 8（兼容）
```

## 快速开始

### 1.20.1

```powershell
$env:JAVA_HOME='D:\dev\tools\jdk-17'
$env:GRADLE_USER_HOME='D:\dev\tools\gradle-home'
cd D:\dev\projects\IEAddon\versions\1.20.1
.\gradlew.bat build
```

### 1.12.2

```powershell
$env:JAVA_HOME='D:\dev\tools\jdk-8'
$env:GRADLE_USER_HOME='D:\dev\tools\gradle-home'
cd D:\dev\projects\IEAddon\versions\1.12.2
.\gradlew.bat build
```

## 进游戏测试

1. `build` 后产物在 `versions/<ver>/build/libs/`；
2. 把 **IE 正式 jar**（`libs/` 里那个）和你的 jar 一起放进 `versions/<ver>/run/mods/`；
3. `.\gradlew.bat runClient`。

## 文档

按顺序读 `docs/notes/00-总览与阅读顺序.md`。

## 改什么

| 想做的事 | 改哪里 |
|---|---|
| 改 modid / 名称 | `versions/*/gradle.properties` + lang |
| 加新物品 | `versions/1.20.1/.../registry/ModItems.java`（1.12.2 是 `Registration.java`） |
| 改工具逻辑 | `.../item/MyUpgradeableToolItem.java` |
| 加新升级 | `.../item/MyUpgradeItem.java` + 注册 |
| 换自己的 toolset 名 | `MyUpgradeableToolItem.TOOLSET` |

## 许可证提醒

- IE 采用 *Blu's License of Common Sense*：**允许做 addon**，但如果你用了 IE 的非 API 实现代码，
  你的 addon 必须开源可见；**禁止收费 / 付费墙**；**禁止重新分发 IE 本体**。
- 详见 `docs/notes/07-许可证与发布合规.md`。
