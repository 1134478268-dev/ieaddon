package com.ieaddon.template12.registry;

import com.ieaddon.template12.IEAddonTemplate;
import com.ieaddon.template12.item.MyUpgradeItem;
import com.ieaddon.template12.item.MyUpgradeableToolItem;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 * 1.12.2 的物品注册。
 *
 * <p>必须用 {@link RegistryEvent.Register}：在事件外 new Item 会导致注册名/本地化失效。
 */
@Mod.EventBusSubscriber(modid = IEAddonTemplate.MODID)
public final class Registration {
    @GameRegistry.ObjectHolder(IEAddonTemplate.MODID + ":my_tool")
    public static final Item MY_TOOL = null;

    @GameRegistry.ObjectHolder(IEAddonTemplate.MODID + ":my_upgrade")
    public static final Item MY_UPGRADE = null;

    private Registration() {
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
                new MyUpgradeableToolItem("my_tool"),
                new MyUpgradeItem("my_upgrade")
        );
    }
}
