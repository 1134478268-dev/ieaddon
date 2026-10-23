package com.ieaddon.template12.client;

import blusunrize.immersiveengineering.api.MultiblockHandler;
import blusunrize.immersiveengineering.api.MultiblockHandler.IMultiblock;
import com.ieaddon.template12.multiblock.ProjectionMath;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 1.12.2 的投影状态：半透明预览 + 自动搭建的位置计算。
 *
 * <p>位置规则：
 * <ul>
 *   <li>以准星命中的方块为结构中心；</li>
 *   <li>如果结构会与已有方块重叠，整体向上抬，直到没有阻挡。</li>
 * </ul>
 */
public final class ClientProjection {
    private static final int MAX_LIFT = 16;

    private static IMultiblock multiblock;
    private static int index = -1;
    private static ItemStack[][][] structure;
    private static BlockPos center;
    private static EnumFacing facing = EnumFacing.NORTH;
    private static boolean fixed;
    private static int lift;

    private ClientProjection() {
    }

    public static void begin(IMultiblock mb) {
        java.util.List<IMultiblock> all = MultiblockHandler.getMultiblocks();
        String name = mb.getUniqueName();
        IMultiblock resolved = null;
        int idx = -1;

        // 手册里的"完整展示"结构（如 IE:ExcavatorDemo）不在服务端注册列表里，
        // 去掉 Demo 后缀映射到真实结构
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getUniqueName().equals(name)) {
                resolved = all.get(i);
                idx = i;
                break;
            }
        }
        if (resolved == null && name.endsWith("Demo")) {
            String base = name.substring(0, name.length() - "Demo".length());
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).getUniqueName().equals(base)) {
                    resolved = all.get(i);
                    idx = i;
                    break;
                }
            }
        }
        if (resolved == null) {
            resolved = mb; // 保底：至少能显示投影
        }

        multiblock = resolved;
        index = idx;
        // 投影用手册页自己的完整结构（斗轮挖掘机页是 exca+斗轮的合并造型），
        // 自动搭建时服务端再用真实结构分别摆
        structure = mb.getStructureManual();
        if (structure == null) {
            structure = resolved.getStructureManual();
        }
        center = null;
        fixed = false;
        lift = 0;
        Minecraft mc = Minecraft.getMinecraft();
        facing = mc.player != null
                ? mc.player.getHorizontalFacing()
                : EnumFacing.NORTH;
    }

    public static boolean isActive() {
        return structure != null;
    }

    public static boolean isFixed() {
        return fixed;
    }

    public static String multiblockName() {
        return multiblock == null ? "" : multiblock.getUniqueName();
    }

    public static int multiblockIndex() {
        return index;
    }

    public static BlockPos center() {
        return center;
    }

    public static EnumFacing facing() {
        return facing;
    }

    public static int lift() {
        return lift;
    }

    public static void update() {
        if (structure == null || fixed) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != RayTraceResult.Type.BLOCK) {
            center = null;
            return;
        }
        // 基点用准星命中的方块（IE 的 pos 语义），朝向用玩家朝向（IE 的 side）
        center = mc.objectMouseOver.getBlockPos();
        if (mc.player != null) {
            facing = mc.player.getHorizontalFacing();
        }
        lift = computeLift();
    }

    public static void fix() {
        if (center != null) {
            fixed = true;
        }
    }

    public static void cancel() {
        multiblock = null;
        structure = null;
        center = null;
        fixed = false;
        lift = 0;
    }

    /** 当前需要渲染的半透明方块（跳过放对的格子）。 */
    public static List<Entry> entries() {
        List<Entry> out = new ArrayList<>();
        if (structure == null || center == null) {
            return out;
        }
        Minecraft mc = Minecraft.getMinecraft();
        int sizeY = structure.length;
        int sizeX = structure[0].length;
        int sizeZ = structure[0][0].length;
        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                for (int z = 0; z < sizeZ; z++) {
                    ItemStack stack = structure[y][x][z];
                    if (stack == null || stack.isEmpty()) {
                        continue;
                    }
                    IBlockState expected = expectedState(stack);
                    if (expected == null) {
                        continue;
                    }
                    BlockPos pos = ProjectionMath.worldPos(center, facing,
                            x - sizeX / 2, y + lift, z - sizeZ / 2);
                    if (mc.world != null && mc.world.getBlockState(pos) == expected) {
                        continue;
                    }
                    out.add(new Entry(pos, expected));
                }
            }
        }
        return out;
    }

    private static int computeLift() {
        for (int offset = 0; offset <= MAX_LIFT; offset++) {
            if (!collides(offset)) {
                return offset;
            }
        }
        return MAX_LIFT;
    }

    private static boolean collides(int offset) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || structure == null || center == null) {
            return false;
        }
        int sizeY = structure.length;
        int sizeX = structure[0].length;
        int sizeZ = structure[0][0].length;
        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                for (int z = 0; z < sizeZ; z++) {
                    ItemStack stack = structure[y][x][z];
                    if (stack == null || stack.isEmpty()) {
                        continue;
                    }
                    BlockPos pos = ProjectionMath.worldPos(center, facing,
                            x - sizeX / 2, y + offset, z - sizeZ / 2);
                    IBlockState expected = expectedState(stack);
                    IBlockState actual = mc.world.getBlockState(pos);
                    if (actual == expected) {
                        continue;
                    }
                    if (!actual.getBlock().isAir(actual, mc.world, pos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Nullable
    public static IBlockState expectedState(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Block block = Block.getBlockFromItem(stack.getItem());
        if (block == null) {
            return null;
        }
        try {
            return block.getStateFromMeta(stack.getMetadata());
        } catch (RuntimeException ex) {
            return block.getDefaultState();
        }
    }

    /** 一个待渲染的方块。 */
    public static final class Entry {
        public final BlockPos pos;
        public final IBlockState state;

        public Entry(BlockPos pos, IBlockState state) {
            this.pos = pos;
            this.state = state;
        }
    }
}
