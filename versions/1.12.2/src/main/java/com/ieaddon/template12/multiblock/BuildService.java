package com.ieaddon.template12.multiblock;

import blusunrize.immersiveengineering.api.MultiblockHandler;
import blusunrize.immersiveengineering.api.MultiblockHandler.IMultiblock;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务端搭建：材料校验/扣除，然后交给 {@link BuildTasks} 逐块放置。
 *
 * <p>方块位置全部来自 {@link StructureLayout}，与客户端投影同源。
 */
public final class BuildService {
    private static int skippedConveyors;

    private BuildService() {
    }

    public static void build(EntityPlayerMP player, int index, String requestedName,
                             BlockPos center, EnumFacing facing, int lift) {
        skippedConveyors = 0;
        World world = player.world;
        List<IMultiblock> all = MultiblockHandler.getMultiblocks();

        IMultiblock main;
        IMultiblock wheel = null;
        if (requestedName != null && requestedName.contains("Excavator")) {
            main = findByName("IE:Excavator");
            wheel = findByName("IE:BucketWheel");
        } else {
            main = findByName(requestedName);
            if (main == null && index >= 0 && index < all.size()) {
                main = all.get(index);
            }
        }
        if (main == null) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "找不到结构 [" + requestedName + "]"));
            return;
        }

        Map<BlockPos, IBlockState> toPlace = new LinkedHashMap<>();
        List<CostEntry> cost = new ArrayList<>();
        for (StructureLayout.Placement p : StructureLayout.layout(main, wheel, center, facing, lift, false)) {
            if (p.isConveyor()) {
                skippedConveyors++;
                continue;
            }
            if (world.getBlockState(p.pos) == p.state) {
                continue;
            }
            toPlace.put(p.pos, p.state);
            addCost(cost, new ItemStack(p.stack.getItem(), 1, p.stack.getMetadata()));
        }

        if (toPlace.isEmpty()) {
            player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "结构已经完整，不需要再放方块"));
            return;
        }

        List<CostEntry> missing = new ArrayList<>();
        for (CostEntry entry : cost) {
            int have = countInInventory(player, entry.stack);
            if (have < entry.count) {
                missing.add(new CostEntry(entry.stack, entry.count - have));
            }
        }
        if (!missing.isEmpty()) {
            List<ItemStack> stacks = new ArrayList<>();
            List<Integer> counts = new ArrayList<>();
            for (CostEntry entry : missing) {
                stacks.add(entry.stack);
                counts.add(entry.count);
            }
            com.ieaddon.template12.network.ModNetwork.CHANNEL.sendTo(
                    new com.ieaddon.template12.network.MissingMessage(stacks, counts), player);
            return;
        }

        for (CostEntry entry : cost) {
            int remaining = entry.count;
            for (int i = 0; i < player.inventory.getSizeInventory() && remaining > 0; i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (!stack.isEmpty() && matches(stack, entry.stack)) {
                    int take = Math.min(remaining, stack.getCount());
                    stack.shrink(take);
                    remaining -= take;
                }
            }
        }

        BuildTask task = new BuildTask(player.getUniqueID(), world, center.up(lift));
        for (Map.Entry<BlockPos, IBlockState> entry : toPlace.entrySet()) {
            task.enqueue(entry.getKey(), entry.getValue());
        }
        BuildTasks.add(task);
        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "开始搭建 " + toPlace.size() + " 个方块（逐块进行）"));
        if (skippedConveyors > 0) {
            player.sendMessage(new TextComponentString(TextFormatting.YELLOW
                    + "有 " + skippedConveyors + " 个传送带需要你自己放置（投影已隐藏传送带）"));
        }
    }

    private static IMultiblock findByName(String name) {
        if (name == null) {
            return null;
        }
        for (IMultiblock candidate : MultiblockHandler.getMultiblocks()) {
            if (candidate.getUniqueName().equals(name)) {
                return candidate;
            }
        }
        return null;
    }

    private static void addCost(List<CostEntry> cost, ItemStack stack) {
        for (CostEntry entry : cost) {
            if (matches(entry.stack, stack)) {
                entry.count++;
                return;
            }
        }
        cost.add(new CostEntry(stack, 1));
    }

    private static boolean matches(ItemStack a, ItemStack b) {
        return ItemStack.areItemsEqual(a, b);
    }

    private static int countInInventory(EntityPlayerMP player, ItemStack wanted) {
        int total = 0;
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && matches(stack, wanted)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static final class CostEntry {
        final ItemStack stack;
        int count;

        CostEntry(ItemStack stack, int count) {
            this.stack = stack;
            this.count = count;
        }
    }
}
