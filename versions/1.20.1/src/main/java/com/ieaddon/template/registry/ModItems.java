package com.ieaddon.template.registry;

import com.ieaddon.template.IEAddonTemplate;
import com.ieaddon.template.item.MyUpgradeableToolItem;
import com.ieaddon.template.item.MyUpgradeItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 所有物品的注册入口。
 *
 * <p>多方块搭建不再提供独立物品：入口是 IE 自带的工程师手册（Shift + 右键核心方块）。
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(IEAddonTemplate.MODID);

    /** 示例升级物品：只对 "MY_TOOL" 生效。 */
    public static final RegistryObject<Item> MY_UPGRADE = ITEMS.register("my_upgrade",
            () -> new MyUpgradeItem("MY_TOOL", 1, new Item.Properties()));

    /** 示例可升级工具：实现 IE 的 IUpgradeableTool。 */
    public static final RegistryObject<Item> MY_TOOL = ITEMS.register("my_tool",
            () -> new MyUpgradeableToolItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }
}
