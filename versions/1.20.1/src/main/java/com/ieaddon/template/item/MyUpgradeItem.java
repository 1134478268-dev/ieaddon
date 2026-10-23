package com.ieaddon.template.item;

import blusunrize.immersiveengineering.api.tool.IUpgrade;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * 一个最小的 IE「升级物品」实现。
 *
 * <p>IE 的升级系统靠字符串标签匹配：工具声明 toolset（如 "MY_TOOL"），
 * 升级物品的 {@link #getUpgradeTypes} 返回同一个 toolset，玩家就能在
 * 工程师工作台里把它装到工具上。
 *
 * <p><b>注意</b>：{@link #applyUpgrades} 只允许修改传入的 {@code modifications}，
 * 绝对不要直接改 {@code target}，否则移除升级后无法还原。
 */
public class MyUpgradeItem extends Item implements IUpgrade {
    private final String toolset;
    private final int maxStack;

    public MyUpgradeItem(String toolset, int maxStack, Properties properties) {
        super(properties);
        this.toolset = toolset;
        this.maxStack = maxStack;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return maxStack;
    }

    @Override
    public Set<String> getUpgradeTypes(ItemStack upgrade) {
        return Set.of(toolset);
    }

    @Override
    public boolean canApplyUpgrades(ItemStack target, ItemStack upgrade) {
        // 在这里做互斥检查，例如：
        // return !((IUpgradeableTool) target.getItem()).getUpgrades(target).contains("my_flag");
        return true;
    }

    @Override
    public void applyUpgrades(ItemStack target, ItemStack upgrade, CompoundTag modifications) {
        modifications.putBoolean("my_flag", true);
        modifications.putFloat("my_speed", upgrade.getCount() * 1.5f);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tips, TooltipFlag flag) {
        tips.add(Component.translatable("item.ieaddon.my_upgrade.desc").withStyle(ChatFormatting.GRAY));
    }
}
