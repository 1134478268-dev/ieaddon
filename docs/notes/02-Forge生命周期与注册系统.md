# 02 · Forge 生命周期与注册系统（1.12.2 vs 1.20.1）

## 1. 启动顺序（简化）

```
JVM 启动
 └─ Forge 加载器扫描 mods/
     └─ 构造每个 @Mod 类的实例                ← 1.12.2 与 1.20.1 都在这里注册 DeferredRegister / 事件监听
         ├─ [1.12.2] preInit → init → postInit（FMLPreInitializationEvent 等）
         └─ [1.20.1] 构造 → FMLCommonSetupEvent → FMLClientSetupEvent → FMLDedicatedServerSetupEvent
             └─ RegisterEvent（注册表填充）→ 数据包加载 → 进入主菜单/世界
```

**最重要的顺序事实**：`RegisterEvent` 发生在 `FMLCommonSetupEvent` **之前**。
所以注册表相关代码只能在注册事件里做；而"读取配置""注册网络通道"要在 setup 事件里做。

## 2. 1.12.2 的生命周期写法

```java
@Mod(modid = "mymod", name = "My Mod", version = "1.0.0",
     dependencies = "required-after:immersiveengineering")
public class MyMod {
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) { /* 配置、注册表前的准备 */ }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) { /* 配方、世界生成 */ }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent e) { /* 跨 mod 交互 */ }
}
```

注册物品（必须用 `RegistryEvent.Register`，不要在 preInit 里 `new` 完就完事）：

```java
@Mod.EventBusSubscriber
public class Registration {
    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> e) {
        e.getRegistry().register(new Item()
            .setRegistryName("mymod", "wrench")
            .setUnlocalizedName("mymod.wrench")
            .setCreativeTab(CreativeTabs.TOOLS));
    }
}
```

## 3. 1.20.1 的生命周期写法

```java
@Mod(MyMod.MODID)
public class MyMod {
    public static final String MODID = "mymod";
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public MyMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        bus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent e) { /* 网络通道等 */ }
}
```

创造标签页（1.20.1）：

```java
public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main",
    () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.mymod"))
        .icon(() -> new ItemStack(MY_ITEM.get()))
        .displayItems((params, output) -> output.accept(MY_ITEM.get()))
        .build());

// 或往原版页里加：
@SubscribeEvent
public static void addToVanillaTabs(BuildCreativeModeTabContentsEvent e) {
    if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES)
        e.accept(MY_ITEM.get());
}
```

## 4. `mods.toml` 与 IE 依赖

`src/main/resources/META-INF/mods.toml`（1.20.1）：

```toml
modLoader="javafml"
loaderVersion="[47,)"
license="All Rights Reserved"
[[mods]]
modId="mymod"
version="1.0.0"
displayName="My IE Addon"
description='''An addon for Immersive Engineering.'''

[[dependencies.mymod]]
modId="forge"
mandatory=true
versionRange="[47.3.0,)"
ordering="NONE"
side="BOTH"

[[dependencies.mymod]]
modId="immersiveengineering"
mandatory=true
versionRange="[10.2.0,)"
ordering="AFTER"
side="BOTH"
```

1.12.2 用 `mcmod.info`：

```json
[{
  "modid": "mymod",
  "name": "My IE Addon",
  "version": "1.0.0",
  "dependencies": ["immersiveengineering"]
}]
```

> `ordering="AFTER"` 很重要：IE 的 API 类要在你的 mod 初始化前可用。

## 5. 事件订阅的两种风格

**1.12.2**：`@Mod.EventBusSubscriber` + 静态 `@SubscribeEvent` 方法（自动注册）。

**1.20.1**：`@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)`，
或手动 `MinecraftForge.EVENT_BUS.register(this)`。

注意 1.20.1 的 `Bus.MOD` 与 `Bus.FORGE` 分开：注册/生命周期事件在 MOD 总线，
游戏内事件（`PlayerInteractEvent`、`BlockEvent.BreakEvent`）在 FORGE 总线。

## 6. 跨 mod 通信：IMC 与依赖注入

- **IMC（InterModComms）**：给已加载的 mod 发一次性消息，是 addon 与 IE 通信的官方渠道。
  IE 在 `IEIMCHandler` 里注册了若干 `MESSAGE_HANDLERS`（例如 `fluidpipe_cover`）。
- **直接调用 API**：`compileOnly` 依赖 IE 后，可直接 `import blusunrize.immersiveengineering.api.*`。
  这是做工具/装备扩展的主要方式。
- **软依赖**：想"有 IE 就联动、没有也能跑"，用 `ModList.get().isLoaded("immersiveengineering")` 判断，
  并把所有 IE 调用隔离在一个单独的类里（避免类加载期 NoClassDefFoundError）。

## 7. 常见生命周期踩坑

| 现象 | 原因 |
|---|---|
| 物品 id 为 `minecraft:air` | 在注册事件外创建并持有 `new Item()` |
| 服务端启动崩 `NoClassDefFoundError: net/minecraft/client/...` | 客户端类被主类静态引用 |
| 配方只在单人有效 | 配方 JSON 放错到 `assets/` |
| `RegistryObject` 为 null | 在 `DeferredRegister.register(bus)` 之前调用了 `.get()` |
| IE 的类找不到 | 没加 IE 依赖，或 `ordering` 不是 `AFTER` |
| 1.12.2 编译报 `cannot find symbol` | 用了 `compileOnly` 但没 `deobf`，名字还是 SRG |
