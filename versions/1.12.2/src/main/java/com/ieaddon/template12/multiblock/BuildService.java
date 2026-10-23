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
 * 服务端执行"自动摆放方块"（不负责成型，成型仍由玩家用工程师锤敲）。
 *
 * <p>材料统计用 {@link ItemStack#areItemsEqual} 匹配，提示时用
 * {@link ItemStack#getDisplayName()}，所以会显示游戏内本地化名称。
 */
public final class BuildService {
    private BuildService() {
    }

    public static void build(EntityPlayerMP player, int index, String requestedName,
                             BlockPos center, EnumFacing facing, int lift) {
        World world = player.world;
        java.util.List<IMultiblock> all = MultiblockHandler.getMultiblocks();
        IMultiblock multiblock = null;
        if (requestedName != null && !requestedName.isEmpty()) {
            for (IMultiblock candidate : all) {
                if (candidate.getUniqueName().equalsIgnoreCase(requestedName)) {
                    multiblock = candidate;
                    break;
                }
            }
        }
        if (multiblock == null && index >= 0 && index < all.size()) {
            multiblock = all.get(index);
        }
        if (multiblock == null) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "找不到结构 [" + requestedName + "]，索引 " + index + " / 共 " + all.size()));
            StringBuilder names = new StringBuilder();
            for (int i = 0; i < Math.min(5, all.size()); i++) {
                if (i > 0) {
                    names.append(", ");
                }
                names.append(all.get(i).getUniqueName());
            }
            player.sendMessage(new TextComponentString(TextFormatting.GRAY + "服务端: " + names));
            return;
        }
        String name = multiblock.getUniqueName();
        ItemStack[][][] structure = multiblock.getStructureManual();
        if (structure == null) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "该结构没有可用的搭建数据"));
            return;
        }

        int sizeY = structure.length;
        int sizeX = structure[0].length;
        int sizeZ = structure[0][0].length;

        Map<BlockPos, IBlockState> toPlace = new LinkedHashMap<>();
        List<CostEntry> cost = new ArrayList<>();
        int slot = 0;
        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                for (int z = 0; z < sizeZ; z++) {
                    ItemStack stack = structure[y][x][z];
                    slot++;
                    if (stack == null || stack.isEmpty()) {
                        continue;
                    }
                    IBlockState state = multiblock.getBlockstateFromStack(slot, stack);
                    if (state == null) {
                        continue;
                    }
                    BlockPos pos = ProjectionMath.worldPos(center, facing, x - sizeX / 2, y + lift, z - sizeZ / 2);
                    if (world.getBlockState(pos) == state) {
                        continue;
                    }
                    if (!world.getBlockState(pos).getBlock().isReplaceable(world, pos)) {
                        player.sendMessage(new TextComponentString(
                                TextFormatting.RED + "有方块阻挡（" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "）"));
                        return;
                    }
                    toPlace.put(pos, state);
                    addCost(cost, new ItemStack(stack.getItem(), 1, stack.getMetadata()));
                }
            }
        }

        // 斗轮挖掘机：同时摆"斗轮"模块，两个模块一起完成拼接
        if ("IE:Excavator".equals(name)) {
            IMultiblock wheel = findByName("IE:BucketWheel");
            if (wheel != null && !addStructure(player, world, wheel, center.offset(facing, 1), facing, 0, toPlace, cost)) {
                return;
            }
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
            player.sendMessage(new TextComponentString(TextFormatting.RED + "材料不足，还缺："));
            for (CostEntry entry : missing) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.GRAY + "  " + entry.count + "x " + entry.stack.getDisplayName()));
            }
            return;
        }

        // 扣材料
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

        for (Map.Entry<BlockPos, IBlockState> entry : toPlace.entrySet()) {
            world.setBlockState(entry.getKey(), entry.getValue(), 3);
        }
        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "已摆放 " + toPlace.size() + " 个方块，请用工程师锤敲击成型"));

        // 斗轮挖掘机：IE 在 createStructure 里会检查"前方 4 格是钢块"，
        // 是的话顺手把斗轮也拼上。这里替玩家把那个触发钢块放好。
        if ("excavator".equals(name)) {
            placeBucketWheelTrigger(player, world, center, facing, sizeX, sizeZ);
        }
    }

    private static void placeBucketWheelTrigger(EntityPlayerMP player, World world,
                                                BlockPos center, EnumFacing facing, int sizeX, int sizeZ) {
        // 挖掘机数组是 [3][6][3]，触发点 l=0，斗轮触发点在 l=4、w=中心
        BlockPos trigger = ProjectionMath.worldPos(center, facing, 4 - sizeX / 2, 0, sizeZ / 2 - sizeZ / 2);
        if (!world.getBlockState(trigger).getBlock().isReplaceable(world, trigger)) {
            return;
        }
        IBlockState steel = steelBlockState();
        if (steel == null) {
            return;
        }
        world.setBlockState(trigger, steel, 3);
        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "已在斗轮位置放好触发钢块，敲击挖掘机会自动拼接斗轮"));
    }

    private static IBlockState steelBlockState() {
        java.util.List<ItemStack> ores = net.minecraftforge.oredict.OreDictionary.getOres("blockSteel");
        if (ores.isEmpty()) {
            return null;
        }
        ItemStack stack = ores.get(0);
        net.minecraft.block.Block block = net.minecraft.block.Block.getBlockFromItem(stack.getItem());
        if (block == null || block == net.minecraft.init.Blocks.AIR) {
            return null;
        }
        return block.getStateFromMeta(stack.getMetadata());
    }

    private static IMultiblock findByName(String name) {
        for (IMultiblock candidate : MultiblockHandler.getMultiblocks()) {
            if (candidate.getUniqueName().equals(name)) {
                return candidate;
            }
        }
        return null;
    }

    /** 把某个结构的方块加入待放置列表；被阻挡时提示并返回 false。 */
    private static boolean addStructure(EntityPlayerMP player, World world, IMultiblock multiblock,
                                        BlockPos center, EnumFacing facing, int lift,
                                        Map<BlockPos, IBlockState> toPlace, List<CostEntry> cost) {
        ItemStack[][][] structure = multiblock.getStructureManual();
        if (structure == null) {
            return true;
        }
        int sizeY = structure.length;
        int sizeX = structure[0].length;
        int sizeZ = structure[0][0].length;
        int slot = 0;
        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                for (int z = 0; z < sizeZ; z++) {
                    ItemStack stack = structure[y][x][z];
                    slot++;
                    if (stack == null || stack.isEmpty()) {
                        continue;
                    }
                    IBlockState state = multiblock.getBlockstateFromStack(slot, stack);
                    if (state == null) {
                        continue;
                    }
                    BlockPos pos = ProjectionMath.worldPos(center, facing, x - sizeX / 2, y + lift, z - sizeZ / 2);
                    if (world.getBlockState(pos) == state) {
                        continue;
                    }
                    if (!world.getBlockState(pos).getBlock().isReplaceable(world, pos)) {
                        player.sendMessage(new TextComponentString(
                                TextFormatting.RED + "斗轮位置被阻挡（" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "）"));
                        return false;
                    }
                    toPlace.put(pos, state);
                    addCost(cost, new ItemStack(stack.getItem(), 1, stack.getMetadata()));
                }
            }
        }
        return true;
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
        return ItemStack.areItemsEqual(a, b) && ItemStack.areItemStackTagsEqual(a, b);
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
