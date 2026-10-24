package com.ieaddon.template12;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 1.12.2 版 addon 骨架主类。
 *
 * <p>1.12.2 没有 DeferredRegister，物品注册走 {@link com.ieaddon.template12.registry.Registration}
 * 里的 RegistryEvent。
 *
 * <p>依赖声明写在 {@code @Mod(dependencies=...)} 里，IE 的类在 preInit 之后才保证可用。
 */
@Mod(
        modid = IEAddonTemplate.MODID,
        name = "IE Addon Template",
        version = "1.0.0",
        dependencies = "required-after:immersiveengineering",
        acceptedMinecraftVersions = "[1.12,1.13)"
)
public class IEAddonTemplate {
    public static final String MODID = "ieaddon";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("[{}] preInit", MODID);
        com.ieaddon.template12.network.ModNetwork.register();
        if (event.getSide().isClient()) {
            com.ieaddon.template12.client.ClientKeybindings.register();
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("[{}] init", MODID);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        LOGGER.info("[{}] postInit", MODID);
    }
}
