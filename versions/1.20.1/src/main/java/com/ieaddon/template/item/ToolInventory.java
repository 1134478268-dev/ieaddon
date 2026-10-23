package com.ieaddon.template.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/**
 * 把「工具里的升级槽」直接存在 ItemStack 的 NBT 里。
 *
 * <p>IE 的工作台会通过 {@code IItemHandler} capability 读写工具内部槽位，
 * 所以 addon 必须提供一个 handler。这里用最直接的方式：NBT 列表 + 每次读写都反射回 NBT，
 * 这样工具掉在地上、放进箱子、跨维度都不会丢升级。
 */
public class ToolInventory implements IItemHandler {
    private static final String TAG_INV = "inv";

    private final ItemStack stack;
    private final int size;

    public ToolInventory(ItemStack stack, int size) {
        this.stack = stack;
        this.size = size;
    }

    private ListTag list() {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(TAG_INV, Tag.TAG_LIST)) {
            ListTag created = new ListTag();
            for (int i = 0; i < size; i++) {
                created.add(new CompoundTag());
            }
            tag.put(TAG_INV, created);
        }
        return tag.getList(TAG_INV, Tag.TAG_COMPOUND);
    }

    @Override
    public int getSlots() {
        return size;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= size) {
            return ItemStack.EMPTY;
        }
        CompoundTag entry = list().getCompound(slot);
        return entry.isEmpty() ? ItemStack.EMPTY : ItemStack.of(entry);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || slot < 0 || slot >= size) {
            return stack;
        }
        ItemStack current = getStackInSlot(slot);
        if (!current.isEmpty()) {
            return stack;
        }
        if (!simulate) {
            list().set(slot, stack.copyWithCount(1).save(new CompoundTag()));
        }
        return stack.copyWithCount(stack.getCount() - 1);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0 || slot < 0 || slot >= size) {
            return ItemStack.EMPTY;
        }
        ItemStack current = getStackInSlot(slot);
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (!simulate) {
            list().set(slot, new CompoundTag());
        }
        return current;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return ModSlotRules.isUpgrade(stack);
    }
}
