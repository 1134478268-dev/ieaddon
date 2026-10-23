package com.ieaddon.template12.item;

import blusunrize.immersiveengineering.api.tool.IUpgrade;
import blusunrize.immersiveengineering.api.tool.IUpgradeableTool;
import com.ieaddon.template12.IEAddonTemplate;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

import javax.annotation.Nullable;

/**
 * 1.12.2 版的可升级工具。
 *
 * <p>与 1.20.1 的差异：
 * <ul>
 *   <li>{@code recalculateUpgrades(ItemStack)} 没有 level/player 参数；</li>
 *   <li>{@code getWorkbenchSlots(Container, ItemStack)} 没有 capability 参数；</li>
 *   <li>客户端判断用 {@code FMLCommonHandler.getEffectiveSide()}。</li>
 * </ul>
 */
public class MyUpgradeableToolItem extends Item implements IUpgradeableTool {
    public static final String TOOLSET = "MY_TOOL";
    public static final int SLOT_COUNT = 3;
    private static final String TAG_UPGRADES = "upgrades";

    public MyUpgradeableToolItem(String name) {
        setRegistryName(IEAddonTemplate.MODID, name);
        setTranslationKey(IEAddonTemplate.MODID + "." + name);
        setCreativeTab(CreativeTabs.TOOLS);
        setMaxStackSize(1);
    }

    /** 把内部 IInventory 暴露成 IItemHandler，IE 的工作台靠这个访问槽位。 */
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new ICapabilityProvider() {
            @Override
            public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
                return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;
            }

            @Override
            public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
                if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
                    IItemHandler handler = new InvWrapper(new ToolInventory(stack, SLOT_COUNT));
                    return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(handler);
                }
                return null;
            }
        };
    }

    // ------------------------------------------------------------------
    // IUpgradeableTool
    // ------------------------------------------------------------------

    @Override
    public NBTTagCompound getUpgrades(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        if (!tag.hasKey(TAG_UPGRADES, 10)) {
            tag.setTag(TAG_UPGRADES, new NBTTagCompound());
        }
        return tag.getCompoundTag(TAG_UPGRADES);
    }

    @Override
    public void clearUpgrades(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            tag.removeTag(TAG_UPGRADES);
        }
    }

    @Override
    public void finishUpgradeRecalculation(ItemStack stack) {
        // 根据新升级重算缓存
    }

    @Override
    public void recalculateUpgrades(ItemStack stack) {
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            return;
        }
        clearUpgrades(stack);
        NBTTagCompound applied = new NBTTagCompound();
        IItemHandler inv = stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        if (inv != null) {
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack upgradeStack = inv.getStackInSlot(i);
                if (!upgradeStack.isEmpty() && upgradeStack.getItem() instanceof IUpgrade) {
                    IUpgrade upgrade = (IUpgrade) upgradeStack.getItem();
                    if (upgrade.getUpgradeTypes(upgradeStack).contains(TOOLSET)
                            && upgrade.canApplyUpgrades(stack, upgradeStack)) {
                        upgrade.applyUpgrades(stack, upgradeStack, applied);
                    }
                }
            }
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setTag(TAG_UPGRADES, applied);
        finishUpgradeRecalculation(stack);
    }

    @Override
    public boolean canTakeFromWorkbench(ItemStack stack) {
        return true;
    }

    @Override
    public void removeFromWorkbench(EntityPlayer player, ItemStack stack) {
    }

    @Override
    public boolean canModify(ItemStack stack) {
        return true;
    }

    @Override
    public Slot[] getWorkbenchSlots(Container container, ItemStack stack) {
        IInventory inv = new ToolInventory(stack, SLOT_COUNT);
        return new Slot[]{
                new UpgradeSlot(inv, 0, 80, 20),
                new UpgradeSlot(inv, 1, 100, 20),
                new UpgradeSlot(inv, 2, 120, 20)
        };
    }

    /** 只接受本工具 toolset 的 IUpgrade。 */
    public static class UpgradeSlot extends Slot {
        public UpgradeSlot(IInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return !stack.isEmpty()
                    && stack.getItem() instanceof IUpgrade
                    && ((IUpgrade) stack.getItem()).getUpgradeTypes(stack).contains(TOOLSET);
        }
    }
}
