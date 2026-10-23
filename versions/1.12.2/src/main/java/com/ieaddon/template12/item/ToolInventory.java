package com.ieaddon.template12.item;

import blusunrize.immersiveengineering.api.tool.IUpgrade;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * 1.12.2 的工具内部物品栏。
 *
 * <p>1.12.2 的 {@code Slot} 只接受 {@link IInventory}，而 IE 的工作台通过
 * {@code CapabilityItemHandler.ITEM_HANDLER_CAPABILITY} 访问槽位。
 * 所以这里直接实现 IInventory，再在工具类里用 {@code InvWrapper} 包成 IItemHandler。
 */
public class ToolInventory implements IInventory {
    private static final String TAG_INV = "inv";

    private final ItemStack toolStack;
    private final int size;

    public ToolInventory(ItemStack toolStack, int size) {
        this.toolStack = toolStack;
        this.size = size;
    }

    private NBTTagList list() {
        NBTTagCompound tag = toolStack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            toolStack.setTagCompound(tag);
        }
        if (!tag.hasKey(TAG_INV, 9)) {
            NBTTagList created = new NBTTagList();
            for (int i = 0; i < size; i++) {
                created.appendTag(new NBTTagCompound());
            }
            tag.setTag(TAG_INV, created);
        }
        return tag.getTagList(TAG_INV, 10);
    }

    @Override
    public int getSizeInventory() {
        return size;
    }

    @Override
    public String getName() {
        return "ieaddon_tool_inventory";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public net.minecraft.util.text.ITextComponent getDisplayName() {
        return new net.minecraft.util.text.TextComponentString(getName());
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < size; i++) {
            if (!getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        if (index < 0 || index >= size) {
            return ItemStack.EMPTY;
        }
        NBTTagCompound entry = list().getCompoundTagAt(index);
        return entry.isEmpty() ? ItemStack.EMPTY : new ItemStack(entry);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack current = getStackInSlot(index);
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = current.splitStack(count);
        setInventorySlotContents(index, current);
        return taken;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        ItemStack current = getStackInSlot(index);
        setInventorySlotContents(index, ItemStack.EMPTY);
        return current;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index < 0 || index >= size) {
            return;
        }
        list().set(index, stack.isEmpty() ? new NBTTagCompound() : stack.writeToNBT(new NBTTagCompound()));
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public void markDirty() {
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory(EntityPlayer player) {
    }

    @Override
    public void closeInventory(EntityPlayer player) {
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof IUpgrade;
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        for (int i = 0; i < size; i++) {
            list().set(i, new NBTTagCompound());
        }
    }
}
