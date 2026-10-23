package com.ieaddon.template;

import com.ieaddon.template.network.ModNetwork;
import com.ieaddon.template.registry.ModCreativeTabs;
import com.ieaddon.template.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * IE Addon 骨架主类。
 *
 * <p>这个类只做三件事：
 * <ol>
 *   <li>把 DeferredRegister 挂到 mod 事件总线；</li>
 *   <li>监听生命周期事件（setup）；</li>
 *   <li>把游戏内事件监听器注册到 Forge 总线。</li>
 * </ol>
 */
@Mod(IEAddonTemplate.MODID)
public class IEAddonTemplate {
    public static final String MODID = "ieaddon";
    public static final Logger LOGGER = LogUtils.getLogger();

    public IEAddonTemplate() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);

        // 网络通道（手册"投影"按钮 → 服务端）
        ModNetwork.register();

        modBus.addListener(this::commonSetup);

        // 游戏内事件（玩家、方块、tick……）
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("[{}] common setup done", MODID);
    }
}
