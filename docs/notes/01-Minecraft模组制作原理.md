# 01 · Minecraft 模组制作原理

## 1. 本质：你写的是一段被"注入"进 Minecraft 的 Java 代码

Minecraft Java 版是一个普通 Java 程序。Forge 做三件事：

1. **加载器**：在游戏启动早期扫描 `mods/` 目录，读取每个 jar 的 `META-INF/mods.toml`（1.13+）或 `mcmod.info`（1.12.2），
   把 jar 加入类路径，然后实例化标了 `@Mod` 的类。
2. **二进制补丁**：Forge 在开发期把 Minecraft 的混淆名映射成可读名（`field_xxxx` → `posX`），
   并通过补丁（patches）在游戏代码里插入钩子，让你能拦截原版行为。
3. **注册表 + 事件总线**：所有"新东西"都必须登记到游戏注册表；所有"想插一脚"的时机都通过事件订阅。

**推论**：mod 不是"外挂程序"，它和原版代码跑在同一个 JVM、同一个类加载器树里，
所以能直接 `new ItemStack(Items.DIAMOND)`，也必须遵守原版的初始化顺序。

## 2. 三个必须分清的概念

| 概念 | 含义 | 典型误用 |
|---|---|---|
| **注册表 Registry** | 游戏启动时构建的"名字 → 对象"表，物品/方块/实体/配方都在里面 | 在注册事件之外 `new Item()`，导致 id 为空、存档损坏 |
| **事件总线 EventBus** | 观察者模式，Forge 在特定时机广播事件 | 忘记 `register` 监听器，或监听了错误的 bus |
| **侧 Side** | 物理侧（客户端/专用服务端）+ 逻辑侧（区分"是不是主世界"） | 在服务端代码里调用 `Minecraft.getInstance()` 直接崩 |

### 2.1 两条总线（1.13+）

- `ModEventBus`（每个 mod 一条）：生命周期、注册、数据生成。
- `ForgeEventBus`（全局一条）：玩家交互、方块破坏、tick、渲染。
- 1.12.2 只有 `MinecraftForge.EVENT_BUS`（全局）和 `@Mod.EventBusSubscriber`。

### 2.2 注册的两种写法

**1.12.2（命令式）**
```java
@Mod.EventBusSubscriber
public class RegistryHandler {
    @SubscribeEvent
    public static void onItemRegister(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new Item().setRegistryName("mymod", "my_item"));
    }
}
```

**1.20.1（延迟注册 DeferredRegister）**
```java
public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
public static final RegistryObject<Item> MY_ITEM = ITEMS.register("my_item", () -> new Item(new Item.Properties()));
// 在构造器里：ITEMS.register(modEventBus);
```

> 延迟注册的意义：`RegistryObject` 在被真正用到时才从注册表取值，避免类加载顺序问题。

## 3. 一个 mod 的三类文件

```
src/main/
├─ java/            → 代码（服务端 + 客户端共用）
└─ resources/
   ├─ META-INF/mods.toml        → mod 元数据与依赖声明（1.12.2 是 mcmod.info）
   ├─ assets/<modid>/           → 仅客户端：贴图、模型、lang、音效
   │   ├─ lang/zh_cn.json
   │   ├─ models/item/*.json
   │   └─ textures/item/*.png
   └─ data/<modid>/             → 服务端也读：配方、战利品表、标签、进度
       ├─ recipes/*.json
       └─ tags/items/*.json
```

**关键**：`assets` 只在客户端加载；`data` 在服务端和单人游戏都加载。
把配方放进 `assets` 是新手最常犯的错——单人游戏能合成，联机服务器就失效。

## 4. 物品从"注册"到"出现在背包"的完整链路

1. `DeferredRegister` 登记 → 游戏启动时写入 `BuiltInRegistries.ITEM`。
2. 创造模式标签页：1.20.1 用 `BuildCreativeModeTabContentsEvent` 往原版/自定义页里塞。
3. 模型：`assets/<modid>/models/item/my_item.json` 指定 `parent: item/generated` + `textures.layer0`。
4. 语言：`assets/<modid>/lang/zh_cn.json` 里 `"item.mymod.my_item": "我的物品"`。
5. 服务端下发物品时只发 **registry name + NBT**，客户端靠自己的 assets 渲染——所以两端 mod 必须一致。

## 5. NBT：物品的"随身数据库"

`ItemStack` 由 `(Item, count, CompoundTag)` 组成。所有自定义状态（能量、升级、模式）都存 NBT：

```java
// 1.20.1
CompoundTag tag = stack.getOrCreateTag();
tag.putInt("energy", 1000);
// 1.12.2
NBTTagCompound tag = stack.getOrCreateSubCompound("mymod");
tag.setInteger("energy", 1000);
```

**铁律**：NBT key 用小写+下划线；不要存 `ItemStack` 的引用，存 `ItemStack.save()` 的结果；
跨版本迁移时 NBT key 尽量保持不变，方便老存档。

## 6. Capability：给"别人的对象"挂能力

Forge 的 Capability 系统让你不修改原版类就能给 `ItemStack` / `TileEntity` / `Entity` 附加功能。

- **能量**：`IEnergyStorage`（FE/RF 通用）。IE 的机器和你的工具都靠它互通。
- **物品栏**：`IItemHandler`。IE 的工程师工作台通过它读取工具里的升级槽。
- **流体**：`IFluidHandler`。

1.20.1 获取：
```java
stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(e -> e.receiveEnergy(100, false));
```

1.12.2 获取：
```java
stack.getCapability(CapabilityEnergy.ENERGY, null).ifPresent(e -> e.receiveEnergy(100, false));
```

## 7. 客户端/服务端分离（最容易崩的地方）

- 服务端**没有** `Minecraft` 类、没有贴图、没有 `Screen`。
- 任何 `net.minecraft.client.*` 的引用都必须放在只在客户端加载的类里，
  并用 `@OnlyIn(Dist.CLIENT)`（1.12.2）或 `DistExecutor` / 客户端事件（1.20.1）隔离。
- 判断逻辑侧用 `level.isClientSide()`，不要用 `FMLEnvironment.dist` 判断"现在是不是客户端逻辑"。

## 8. 渲染的两条路线

| 版本 | 静态物品 | 动态/复杂物品 |
|---|---|---|
| 1.12.2 | JSON 模型 + `ModelLoader` | `TileEntitySpecialRenderer` / `ItemStackRenderer` |
| 1.20.1 | JSON 模型（`item/generated`） | `BlockEntityRenderer` / `ItemRenderer`（BEWLR） |

IE 的很多工具（钻头、左轮）用自定义 `ItemRenderer` 做 3D 模型。你的工具如果只是平面图标，JSON 模型就够。

## 9. 数据生成（Datagen，1.16+）

1.20.1 推荐用 `DataGenerator` 自动产出 `models/`、`blockstates/`、`recipes/`、`tags/`，
避免手写几百个 JSON。IE 自己就在用（`src/datagen`）。骨架工程里已预留。

## 10. 一句话总结

> **注册**决定"游戏里有没有这个东西"，**事件**决定"它什么时候做事"，
> **NBT/Capability** 决定"它记住什么、能和谁互通"，**Side** 决定"这段代码会不会在服务端炸"。
