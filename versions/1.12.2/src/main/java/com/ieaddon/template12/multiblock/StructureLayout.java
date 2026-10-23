package com.ieaddon.template12.multiblock;

import blusunrize.immersiveengineering.api.MultiblockHandler.IMultiblock;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * 多方块结构的<b>唯一坐标来源</b>：客户端投影和服务端搭建都从这里取方块位置。
 *
 * <p>之前客户端和服务端各写了一套 {@code addExcavator/addWheel/addGeneric}，
 * 两套逻辑不同步会直接导致"投影和搭建不一致"（ histórico：挤压机 lift 漏传）。
 *
 * <p>坐标规则严格照抄 IE 的 {@code createStructure}：
 * <ul>
 *   <li>挖掘机：{@code pos + side*l + side.rotateY()*w + (0,h,0)}；</li>
 *   <li>斗轮：中心 {@code pos + side*4}，w 沿 {@code side.rotateYCCW()}；</li>
 *   <li>其他：数组中心对齐。</li>
 * </ul>
 */
public final class StructureLayout {
    private StructureLayout() {
    }

    /** 一个待放置的方块：坐标 + 状态 + 原始物品（用于扣材料/判断传送带）。 */
    public static final class Placement {
        public final BlockPos pos;
        public final IBlockState state;
        public final ItemStack stack;

        Placement(BlockPos pos, IBlockState state, ItemStack stack) {
            this.pos = pos;
            this.state = state;
            this.stack = stack;
        }

        public boolean isConveyor() {
            if (stack == null || stack.isEmpty()) {
                return false;
            }
            ResourceLocation id = stack.getItem().getRegistryName();
            return id != null && id.getPath().contains("conveyor");
        }
    }

    /**
     * 生成整个结构的方块列表。
     *
     * @param main         主结构
     * @param wheel        斗轮结构（仅挖掘机，可为 null）
     * @param center       触发点（准星方块）
     * @param facing       朝向
     * @param lift         避让上移格数
     * @param skipConveyor 是否跳过传送带（投影和搭建都跳过，交给玩家手动放）
     */
    public static List<Placement> layout(IMultiblock main, IMultiblock wheel, BlockPos center,
                                         EnumFacing facing, int lift, boolean skipConveyor) {
        List<Placement> out = new ArrayList<>();
        if (main == null || center == null) {
            return out;
        }
        if (main.getUniqueName().contains("Excavator")) {
            addExcavator(out, main, center, facing, lift, skipConveyor);
            if (wheel != null) {
                addWheel(out, wheel, center, facing, lift, skipConveyor);
            }
        } else {
            addGeneric(out, main, center, facing, lift, skipConveyor);
        }
        return out;
    }

    private static void addExcavator(List<Placement> out, IMultiblock mb, BlockPos pos, EnumFacing side,
                                     int lift, boolean skipConveyor) {
        ItemStack[][][] s = mb.getStructureManual();
        if (s == null) {
            return;
        }
        int sizeH = s.length;
        int sizeL = s[0].length;
        int sizeW = s[0][0].length;
        int slot = 0;
        for (int l = 0; l < sizeL; l++) {
            for (int w = 0; w < sizeW; w++) {
                for (int h = 0; h < sizeH; h++) {
                    ItemStack stack = s[h][l][w];
                    slot++;
                    if (l > 0 && w == sizeW / 2) {
                        continue;
                    }
                    add(out, mb, stack, slot,
                            ProjectionMath.worldPos(pos, side, l, h - sizeH / 2 + lift, w - sizeW / 2),
                            skipConveyor);
                }
            }
        }
    }

    private static void addWheel(List<Placement> out, IMultiblock mb, BlockPos pos, EnumFacing side,
                                 int lift, boolean skipConveyor) {
        ItemStack[][][] s = mb.getStructureManual();
        if (s == null) {
            return;
        }
        BlockPos wheelPos = pos.offset(side, 4);
        int sizeH = s.length;
        int sizeW = s[0].length;
        // IE 调用斗轮时传的是 side.rotateYCCW()
        EnumFacing wheelSide = side.rotateYCCW();
        int wx = wheelSide == EnumFacing.NORTH ? 1 : wheelSide == EnumFacing.SOUTH ? -1 : 0;
        int wz = wheelSide == EnumFacing.WEST ? 1 : wheelSide == EnumFacing.EAST ? -1 : 0;
        for (int h = 0; h < sizeH; h++) {
            for (int w = 0; w < sizeW; w++) {
                ItemStack stack = s[h][w][0];
                int off = w - sizeW / 2;
                BlockPos p = wheelPos.add(wx * off, h - sizeH / 2 + lift, wz * off);
                add(out, mb, stack, 0, p, skipConveyor);
            }
        }
    }

    private static void addGeneric(List<Placement> out, IMultiblock mb, BlockPos pos, EnumFacing side,
                                   int lift, boolean skipConveyor) {
        ItemStack[][][] s = mb.getStructureManual();
        if (s == null) {
            return;
        }
        int sizeH = s.length;
        int sizeL = s[0].length;
        int sizeW = s[0][0].length;
        for (int h = 0; h < sizeH; h++) {
            for (int l = 0; l < sizeL; l++) {
                for (int w = 0; w < sizeW; w++) {
                    ItemStack stack = s[h][l][w];
                    add(out, mb, stack, 0,
                            ProjectionMath.worldPos(pos, side, l - sizeL / 2, h - sizeH / 2 + lift, w - sizeW / 2),
                            skipConveyor);
                }
            }
        }
    }

    private static void add(List<Placement> out, IMultiblock mb, ItemStack stack, int slot,
                            BlockPos pos, boolean skipConveyor) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (skipConveyor) {
            ResourceLocation id = stack.getItem().getRegistryName();
            if (id != null && id.getPath().contains("conveyor")) {
                return;
            }
        }
        IBlockState state = mb.getBlockstateFromStack(slot, stack);
        if (state == null) {
            return;
        }
        out.add(new Placement(pos, state, stack));
    }
}
