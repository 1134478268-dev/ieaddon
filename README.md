# IE Addon 工程（Immersive Engineering 附属模组）

一个面向 **沉浸工程（Immersive Engineering）** 的 addon 骨架，目标内容是
—————投影自动搭建————— 

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

## 版权与许可

本项目是 **Immersive Engineering 的非官方附属模组**（unofficial addon），与 Immersive Engineering 官方团队无关。

### 原作者署名

- **Immersive Engineering**：作者 **BluSunrize** 及贡献者
- 原项目：<https://github.com/BluSunrize/ImmersiveEngineering>
- 本项目使用了 IE 的公开 API（`IMultiblock`、`ManualPageMultiblock`、`MultiblockHandler` 等），并参考了其公开源码的实现方式，在此致谢原作者。

### 许可证

本项目采用 **Blu's License of Common Sense**（全文见 [LICENSE](LICENSE)），要点：

- 可以查看、修改、作为 addon 分发
- **必须保留原作者署名**
- 不得声明代码为自己所有
- 不得商业化，不得设置付费墙或限制访问
- 不得重新分发 Immersive Engineering 本体（源码或编译产物）
- **衍生作品必须公开源码**（本项目已开源）

### 本仓库不包含的内容

以下目录/文件**有意不入库**，克隆后不存在，属于本地可选内容：

| 路径 | 说明 |
|---|---|
| `libs/` | 编译用 IE jar，请自行从 CurseForge 下载 IE 0.12-98 |
| `docs/IE-src-*/` | IE 源码副本，用于离线查 API，请自行 clone 原仓库 |
| `run-1.12.2/` | 本地游戏运行环境（含存档、日志） |
| `repo/` | 本地 Maven 仓库（IE 依赖），请自行安装或改用 mavenLocal |
| `dist/` | 构建产物目录 |

### 构建前提

- JDK 8
- 需要本地可用的 Immersive Engineering `0.12-98` 依赖（见 `versions/1.12.2/build.gradle` 注释）
