package com.ieaddon.template.item;

import blusunrize.immersiveengineering.api.tool.IUpgrade;
import blusunrize.immersiveengineering.api.tool.IUpgradeableTool;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;

import java.util.function.Supplier;

/**
 * 一个最小的「可升级工具」。
 *
 * <p>它演示了 addon 接入 IE 工作台的完整链路：
 * <ol>
 *   <li>实现 {@link IUpgradeableTool}，声明自己的 toolset；</li>
 *   <li>用 {@link ToolInventory} 提供内部槽位（并通过 capability 暴露给 IE 的工作台）；</li>
 *   <li>{@link #getWorkbenchSlots} 返回工作台里显示的 3 个升级槽；</li>
 *   <li>{@link #recalculateUpgrades} 在服务端把所有升级应用到 NBT。</li>
 * </ol>
 *
 * <p>真正的工具还应该继承 {@code DiggerItem}/{@code SwordItem} 之类，
 * 并在挖掘/攻击时读取 {@code getUpgrades(stack)} 的结果。
 */
public class MyUpgradeableToolItem extends Item implements IUpgradeableTool {
    /** 与升级物品的 getUpgradeTypes 对应。改成你自己的名字。 */
    public static final String TOOLSET = "MY_TOOL";
    public static final int SLOT_COUNT = 3;
    private static final String TAG_UPGRADES = "upgrades";

    public MyUpgradeableToolItem(Properties properties) {
        super(properties);
    }

    public static IItemHandler getInventory(ItemStack stack) {
        return new ToolInventory(stack, SLOT_COUNT);
    }

    // ------------------------------------------------------------------
    // IUpgradeableTool
    // ------------------------------------------------------------------

    @Override
    public CompoundTag getUpgrades(ItemStack stack) {
        return stack.getOrCreateTagElement(TAG_UPGRADES);
    }

    @Override
    public void clearUpgrades(ItemStack stack) {
        stack.removeTagKey(TAG_UPGRADES);
    }

    @Override
    public void finishUpgradeRecalculation(ItemStack stack) {
        // 例如：根据新的升级重算能量上限、挖掘速度缓存
    }

    @Override
    public void recalculateUpgrades(ItemStack stack, Level level, Player player) {
        // 只在服务端算；客户端靠同步
        if (level.isClientSide) {
            return;
        }
        clearUpgrades(stack);
        CompoundTag applied = new CompoundTag();
        IItemHandler inv = getInventory(stack);
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack upgradeStack = inv.getStackInSlot(i);
            if (upgradeStack.getItem() instanceof IUpgrade upgrade
                    && upgrade.getUpgradeTypes(upgradeStack).contains(TOOLSET)
                    && upgrade.canApplyUpgrades(stack, upgradeStack)) {
                upgrade.applyUpgrades(stack, upgradeStack, applied);
            }
        }
        stack.getOrCreateTag().put(TAG_UPGRADES, applied);
        finishUpgradeRecalculation(stack);
    }

    @Override
    public boolean canTakeFromWorkbench(ItemStack stack) {
        return true;
    }

    @Override
    public void removeFromWorkbench(Player player, ItemStack stack) {
        // 可在此解锁进度
    }

    @Override
    public boolean canModify(ItemStack stack) {
        return true;
    }

    @Override
    public Slot[] getWorkbenchSlots(AbstractContainerMenu container, ItemStack stack, Level level,
                                    Supplier<Player> getPlayer, IItemHandler toolInventory) {
        return new Slot[]{
                new UpgradeSlot(toolInventory, 0, 80, 20),
                new UpgradeSlot(toolInventory, 1, 100, 20),
                new UpgradeSlot(toolInventory, 2, 120, 20)
        };
    }

    /** 只接受本工具 toolset 的 IUpgrade。 */
    public static class UpgradeSlot extends Slot {
        public UpgradeSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof IUpgrade upgrade
                    && upgrade.getUpgradeTypes(stack).contains(TOOLSET);
        }
    }
}
