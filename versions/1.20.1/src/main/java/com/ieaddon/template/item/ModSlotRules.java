package com.ieaddon.template.item;

import blusunrize.immersiveengineering.api.tool.IUpgrade;
import net.minecraft.world.item.ItemStack;

/** 槽位规则集中在这里，避免 ToolInventory 与工具类互相 import。 */
public final class ModSlotRules {
    private ModSlotRules() {
    }

    public static boolean isUpgrade(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof IUpgrade;
    }
}
