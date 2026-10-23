# 03 · IE Addon 架构与注册

## 1. IE 对 addon 暴露了什么

IE 的源码里有一个**独立的 API 源集**（1.20.1 是 `src/api`，223 个 Java 文件；
1.12.2 是 `src/main/java/.../api` 包）。发布 jar 时这些 API 类会被打进去，
所以 addon 只要 `compileOnly` 依赖 IE jar，就能直接 `import blusunrize.immersiveengineering.api.*`。

**你的 addon 只应该依赖 `api` 包**。`common` / `client` 包是内部实现：
- 版本升级时随时改签名；
- 按许可证，用了非 API 代码你的项目必须开源可见（见 `07`）。

### 1.1 API 包地图（1.20.1）

```
api/
├─ IEApi.java                ← 全局静态工具：物品优先级、mod 偏好
├─ ApiUtils.java             ← 常用工具方法（NBT、方向、比较器…）
├─ Lib.java                  ← 常量（MODID、GUI ID 等）
├─ IEEnums.java              ← 枚举（方向、连接类型…）
├─ IEProperties.java         ← 方块状态属性
├─ IETags.java               ← 物品/方块标签
├─ ManualHelper.java         ← 手册相关
├─ MultiblockHandler.java    ← 多方块注册入口（1.20.1 在 api/multiblocks 下）
├─ crafting/                 ← 配方类型与 builder（粉碎机、高炉、合金窑…）
├─ energy/                   ← 能量相关接口与工具
├─ multiblocks/              ← 多方块 API（IMultiblock、组件、逻辑、注册）
├─ tool/                     ← ★ 工具/装备扩展点（IUpgrade、IUpgradeableTool…）
├─ wires/                    ← 电线/接线器 API
├─ fluid/                    ← 流体相关
├─ excavator/                ← 斗轮挖掘机矿脉 API
├─ shader/                   ← 着色器背包
└─ utils/                    ← 通用工具类
```

### 1.2 1.12.2 的差异

1.12.2 的 `api` 包多出：`ITool`、`IInternalStorageItem`、`ITeslaEquipment`、
`AssemblerHandler`、`BelljarHandler`、`ConveyorHandler`、`ExcavatorHandler`、`MultiblockHandler`（顶层）。
少掉：`IETags`（1.12.2 用 OreDictionary）、`multiblocks/` 包（1.12.2 多方块 API 是 `MultiblockHandler` + `IMultiblock` 顶层类）。
`ManualHelper` 在 1.12.2 有 `addEntry(...)` 可直接注册手册页；1.20.1 没有这个方法（手册改为数据驱动）。

## 2. addon 依赖 IE 的三种方式

### 2.1 本地 jar + deobf（最稳，推荐离线用）

```groovy
// 1.20.1 / ForgeGradle 6
dependencies {
    minecraft "net.minecraftforge:forge:1.20.1-47.3.0"
    compileOnly fg.deobf(files("../../libs/ImmersiveEngineering-1.20.1-10.2.0-183.jar"))
}
```

```groovy
// 1.12.2 / ForgeGradle 2.3
dependencies {
    deobfCompile files("../../libs/ImmersiveEngineering-0.12-98.jar")
    // 或 deobfCompile "curse.maven:immersive-engineering-231951:<fileId>"
}
```

> `deobf` / `deobfCompile` 的作用：IE 发布 jar 里的类名是 **SRG 名**（`func_xxx` / `field_xxx`），
> 你的开发环境用 **official 名**（`getItem` / `posX`）。不 deobf 就会报 `cannot find symbol`。

### 2.2 CurseMaven（联网）

```groovy
repositories { maven { url = "https://cfa2.cursemaven.com"; content { includeGroup "curse.maven" } } }
dependencies { compileOnly fg.deobf("curse.maven:immersive-engineering-231951:<fileId>") }
```

fileId 在 CurseForge 文件页的 URL 里（`/files/1234567`）。

### 2.3 运行时依赖

开发期 `compileOnly` 就够（不把 IE 打进你的 jar）。
**进游戏测试时必须**把 IE 的正式 jar 放进 `run/mods/`，并在 `mods.toml` 声明 `immersiveengineering` 依赖。

## 3. 扩展点清单（工具/装备相关）

| 接口 / 类 | 作用 | 你要实现什么 |
|---|---|---|
| `api.tool.IUpgrade` | 定义一个"升级物品"（速度、润滑、防水…） | `getUpgradeTypes` / `canApplyUpgrades` / `applyUpgrades` |
| `api.tool.IUpgradeableTool` | 让工具能装升级、能在工程师工作台里改装 | `getUpgrades` / `recalculateUpgrades` / `getWorkbenchSlots` |
| `api.tool.IElectricEquipment` | 装备能"防电"（被电弧/特斯拉击中时改伤害） | `onStrike(...)` |
| `api.tool.IDrillHead` | 自定义钻头（挖掘等级、耐久、特殊掉落） | `getMiningSpeed` / `getDurability` / `getHeadDamage` |
| `api.tool.IConfigurableTool` | 工具可在工作台里切换模式（类似 IE 钻头的 3 种模式） | `getConfig` / `applyConfig` |
| `api.tool.ExternalHeaterHandler` | 你的机器能被 IE 的外部加热器加热 | 注册 `IExternalHeater`（1.12.2）/ 对应 handler |
| `api.tool.BulletHandler` | 左轮子弹类型 | 1.12.2 用 `BulletHandler.registerBullet` |
| `api.energy` | 能量接口（FE） | 用 Forge `IEnergyStorage`，IE 机器只认 FE |
| `api.wires` | 电线/接线器 | 一般 addon 不需要 |

> 完整的 1.20.1 `api/tool` 类列表：
> `IUpgrade, IUpgradeableTool, IElectricEquipment, IDrillHead, IConfigurableTool,
> ExternalHeaterHandler, BulletHandler, ChemthrowerHandler, RailgunHandler,
> ShieldDisablingHandler, ToolboxHandler, ZoomHandler, LogicCircuitHandler, ITeslaEntity`。

## 4. 手册（工程师手册）注册

### 4.1 1.12.2（代码注册）

```java
ManualHelper.addEntry("my_wrench", ManualHelper.CAT_TOOLS, new ManualPageMyWrench());
// ManualPageMyWrench 实现 IManualPage
```

### 4.2 1.20.1（数据驱动）

手册页是 `assets/immersiveengineering/manual/*.json`（IE 自带几十个）。
IE 在 `src/main/resources/assets/immersiveengineering/manual/autoload.json` 里声明加载列表。
addon 想加条目，需要：
1. 在**你的** `assets/<modid>/manual/` 下提供页面 JSON；
2. 研究 IE 的 `autoload` / `ManualHelper` 扫描逻辑，确认是否支持多命名空间；
3. 或干脆不做手册页——工具 addon 不做手册完全可以发布。

> 结论：**手册不是必须的**。先把物品/工具做出来，手册后补。

## 5. IMC（InterModComms）——addon 与 IE 的官方消息通道

IE 在 `IEIMCHandler` 里登记了处理器（1.20.1 例子）：

```java
MESSAGE_HANDLERS.put("fluidpipe_cover", imcMessage -> { ... });
MESSAGE_HANDLERS.put("fluidpipe_cover_climb", imcMessage -> { ... });
```

你的 addon 可以：

```java
InterModComms.sendTo("immersiveengineering", "fluidpipe_cover", () -> myCoverSupplier);
```

1.12.2 用 `InterModComms.sendMessage("immersiveengineering", "fluidpipe_cover", ...)`。

> 用途举例：让你的 mod 的流体管道"能被 IE 的泵连接"。
> IMC 只是补充手段，做工具/装备主要靠直接调 API。

## 6. 多方块注册（了解即可）

```java
// 1.20.1
MultiblockHandler.registerMultiblock(new MyMultiblock());
// 1.12.2
MultiblockHandler.registerMultiblock(new MyMultiblock());
```

`IMultiblock` 需要提供结构模板（`.nbt` 文件）、形成逻辑、tick 逻辑、GUI。
这是做"新机器"的核心，工作量远大于做工具。你这次选的是工具/装备，先跳过。

## 7. 版本兼容的现实结论

| 代码 | 1.12.2 | 1.20.1 |
|---|---|---|
| `IUpgrade` 参数 | `NBTTagCompound` | `CompoundTag` |
| `IUpgradeableTool.recalculateUpgrades` | `(ItemStack)` | `(ItemStack, Level, Player)` |
| 注册方式 | `RegistryEvent` | `DeferredRegister` |
| 能量 | `CapabilityEnergy.ENERGY` | `ForgeCapabilities.ENERGY` |
| 物品类 | `Item` + `setUnlocalizedName` | `Item` + `Properties` |

**同一份 Java 源码无法同时编译到 1.12.2 和 1.20.1。**
多版本 = 多工程 + 共享资源/文档（见 `04`）。
