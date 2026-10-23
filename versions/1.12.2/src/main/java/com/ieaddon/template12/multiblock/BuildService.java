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
 * 服务端执行"自动摆放方块"（不负责成型）。
 *
 * <p>坐标严格照抄 IE 的 createStructure：
 * <ul>
 *   <li>挖掘机：{@code pos + side*l + side.rotateY()*w + (0,h,0)}；</li>
 *   <li>斗轮：中心 {@code pos + side*4}，{@code + (0,h,0) + rotateY*w}；</li>
 *   <li>其他结构：按数组中心映射。</li>
 * </ul>
 */
public final class BuildService {
    private BuildService() {
    }

    public static void build(EntityPlayerMP player, int index, String requestedName,
                             BlockPos center, EnumFacing facing, int lift) {
        World world = player.world;
        List<IMultiblock> all = MultiblockHandler.getMultiblocks();

        Map<BlockPos, IBlockState> toPlace = new LinkedHashMap<>();
        List<CostEntry> cost = new ArrayList<>();

        if (requestedName != null && requestedName.contains("Excavator")) {
            IMultiblock exca = findByName("IE:Excavator");
            IMultiblock wheel = findByName("IE:BucketWheel");
            if (exca == null) {
                player.sendMessage(new TextComponentString(TextFormatting.RED + "服务端找不到挖掘机结构"));
                return;
            }
            // 先摆斗轮（中心是钢块），再摆机器主体，视觉上"从钢块往上长"
            if (wheel != null && !addWheel(player, world, wheel, center, facing, toPlace, cost)) {
                return;
            }
            if (!addExcavator(player, world, exca, center, facing, toPlace, cost)) {
                return;
            }
        } else {
            IMultiblock multiblock = null;
            if (requestedName != null && !requestedName.isEmpty()) {
                multiblock = findByName(requestedName);
            }
            if (multiblock == null && index >= 0 && index < all.size()) {
                multiblock = all.get(index);
            }
            if (multiblock == null) {
                player.sendMessage(new TextComponentString(TextFormatting.RED + "找不到结构 [" + requestedName + "]"));
                return;
            }
            if (!addGeneric(player, world, multiblock, center, facing, toPlace, cost)) {
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

        BuildTask task = new BuildTask(player.getUniqueID(), world);
        for (Map.Entry<BlockPos, IBlockState> entry : toPlace.entrySet()) {
            task.enqueue(entry.getKey(), entry.getValue());
        }
        BuildTasks.add(task);
        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "开始搭建 " + toPlace.size() + " 个方块（逐块进行）"));
    }

    private static boolean addExcavator(EntityPlayerMP player, World world, IMultiblock multiblock,
                                        BlockPos pos, EnumFacing side,
                                        Map<BlockPos, IBlockState> toPlace, List<CostEntry> cost) {
        ItemStack[][][] s = multiblock.getStructureManual();
        if (s == null) {
            return true;
        }
        int sizeH = s.length;
        int sizeL = s[0].length;
        int sizeW = s[0][0].length;
        for (int l = 0; l < sizeL; l++) {
            for (int w = 0; w < sizeW; w++) {
                for (int h = 0; h < sizeH; h++) {
                    if (l > 0 && w == sizeW / 2) {
                        continue;
                    }
                    ItemStack stack = s[h][l][w];
                    if (stack == null || stack.isEmpty()) {
                        continue;
                    }
                    IBlockState state = multiblock.getBlockstateFromStack(0, stack);
                    if (state == null) {
                        continue;
                    }
                    BlockPos p = ProjectionMath.worldPos(pos, side, l, h - sizeH / 2, w - sizeW / 2);
                    if (!addOne(player, world, p, state, stack, toPlace, cost)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean addWheel(EntityPlayerMP player, World world, IMultiblock multiblock,
                                    BlockPos pos, EnumFacing side,
                                    Map<BlockPos, IBlockState> toPlace, List<CostEntry> cost) {
        ItemStack[][][] s = multiblock.getStructureManual();
        if (s == null) {
            return true;
        }
        BlockPos wheelPos = pos.offset(side, 4).offset(side.rotateY(), 1).up().add(1, 1, 0);
        int sizeH = s.length;
        int sizeW = s[0].length;
        // IE 调用斗轮时传的是 side.rotateYCCW()
        EnumFacing wheelSide = side.rotateYCCW();
        int wx = wheelSide == EnumFacing.NORTH ? 1 : wheelSide == EnumFacing.SOUTH ? -1 : 0;
        int wz = wheelSide == EnumFacing.WEST ? 1 : wheelSide == EnumFacing.EAST ? -1 : 0;
        for (int h = 0; h < sizeH; h++) {
            for (int w = 0; w < sizeW; w++) {
                ItemStack stack = s[h][w][0];
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                IBlockState state = multiblock.getBlockstateFromStack(0, stack);
                if (state == null) {
                    continue;
                }
                int off = w - sizeW / 2;
                BlockPos p = wheelPos.add(wx * off, h - sizeH / 2, wz * off);
                if (!addOne(player, world, p, state, stack, toPlace, cost)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean addGeneric(EntityPlayerMP player, World world, IMultiblock multiblock,
                                      BlockPos pos, EnumFacing side,
                                      Map<BlockPos, IBlockState> toPlace, List<CostEntry> cost) {
        ItemStack[][][] s = multiblock.getStructureManual();
        if (s == null) {
            return true;
        }
        int sizeH = s.length;
        int sizeL = s[0].length;
        int sizeW = s[0][0].length;
        for (int h = 0; h < sizeH; h++) {
            for (int l = 0; l < sizeL; l++) {
                for (int w = 0; w < sizeW; w++) {
                    ItemStack stack = s[h][l][w];
                    if (stack == null || stack.isEmpty()) {
                        continue;
                    }
                    IBlockState state = multiblock.getBlockstateFromStack(0, stack);
                    if (state == null) {
                        continue;
                    }
                    BlockPos p = ProjectionMath.worldPos(pos, side,
                            l - sizeL / 2, h - sizeH / 2, w - sizeW / 2);
                    if (!addOne(player, world, p, state, stack, toPlace, cost)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean addOne(EntityPlayerMP player, World world, BlockPos pos, IBlockState state,
                                  ItemStack stack, Map<BlockPos, IBlockState> toPlace, List<CostEntry> cost) {
        if (world.getBlockState(pos) == state) {
            return true;
        }
        toPlace.put(pos, state);
        addCost(cost, new ItemStack(stack.getItem(), 1, stack.getMetadata()));
        return true;
    }

    private static IMultiblock findByName(String name) {
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
