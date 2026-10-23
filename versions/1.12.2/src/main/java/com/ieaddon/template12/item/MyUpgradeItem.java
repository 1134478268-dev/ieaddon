package com.ieaddon.template12.item;

import blusunrize.immersiveengineering.api.tool.IUpgrade;
import com.google.common.collect.ImmutableSet;
import com.ieaddon.template12.IEAddonTemplate;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/** 1.12.2 版的升级物品。 */
public class MyUpgradeItem extends Item implements IUpgrade {
    public MyUpgradeItem(String name) {
        setRegistryName(IEAddonTemplate.MODID, name);
        setTranslationKey(IEAddonTemplate.MODID + "." + name);
        setCreativeTab(CreativeTabs.TOOLS);
        setMaxStackSize(1);
    }

    @Override
    public Set<String> getUpgradeTypes(ItemStack upgrade) {
        return ImmutableSet.of(MyUpgradeableToolItem.TOOLSET);
    }

    @Override
    public boolean canApplyUpgrades(ItemStack target, ItemStack upgrade) {
        return true;
    }

    @Override
    public void applyUpgrades(ItemStack target, ItemStack upgrade, NBTTagCompound modifications) {
        // 只改 modifications，不要动 target
        modifications.setBoolean("my_flag", true);
        modifications.setFloat("my_speed", upgrade.getCount() * 1.5f);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GRAY + I18n.format("item.ieaddon.my_upgrade.desc"));
    }
}
