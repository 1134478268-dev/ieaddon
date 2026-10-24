package com.ieaddon.template12.client;

import blusunrize.immersiveengineering.api.MultiblockHandler;
import blusunrize.immersiveengineering.api.MultiblockHandler.IMultiblock;
import com.ieaddon.template12.multiblock.ProjectionMath;
import com.ieaddon.template12.multiblock.StructureLayout;
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
 * 1.12.2 的投影状态（半透明预览）。
 *
 * <p>坐标严格照抄 IE 的 createStructure：
 * <ul>
 *   <li>挖掘机：{@code pos + side*l + side.rotateY()*w + (0,h,0)}；</li>
 *   <li>斗轮：中心 {@code pos + side*4}，{@code + (0,h,0) + rotateYCCW 方向*w}；</li>
 *   <li>其他结构：按数组中心映射。</li>
 * </ul>
 *
 * <p>避让规则：投影如果与已有方块重叠，整体向上抬，直到没有阻挡。
 */
public final class ClientProjection {
    private static final int MAX_LIFT = 8;

    private static IMultiblock multiblock;
    private static IMultiblock wheelMultiblock;
    private static int index = -1;
    private static ItemStack[][][] structure;
    private static ItemStack[][][] wheelStructure;
    private static String name = "";
    private static BlockPos center;
    private static EnumFacing baseFacing = EnumFacing.NORTH;
    private static int rotation = 1; // 默认 90°（横着）
    private static boolean fixed;
    private static int lift;

    private ClientProjection() {
    }

    public static void begin(IMultiblock mb) {
        name = mb.getUniqueName();
        List<IMultiblock> all = MultiblockHandler.getMultiblocks();
        IMultiblock resolved = find(all, name);
        if (resolved == null && name.endsWith("Demo")) {
            resolved = find(all, name.substring(0, name.length() - 4));
        }
        if (resolved == null) {
            resolved = mb;
        }
        multiblock = resolved;
        index = all.indexOf(resolved);

        if (name.contains("Excavator")) {
            IMultiblock exca = find(all, "IE:Excavator");
            IMultiblock wheel = find(all, "IE:BucketWheel");
            structure = exca != null ? exca.getStructureManual() : resolved.getStructureManual();
            wheelStructure = wheel != null ? wheel.getStructureManual() : null;
            wheelMultiblock = wheel;
        } else {
            structure = resolved.getStructureManual();
            wheelStructure = null;
            wheelMultiblock = null;
        }
        center = null;
        fixed = false;
        lift = 0;
        Minecraft mc = Minecraft.getMinecraft();
        baseFacing = mc.player != null ? mc.player.getHorizontalFacing() : EnumFacing.NORTH;
    }

    private static IMultiblock find(List<IMultiblock> all, String key) {
        for (IMultiblock candidate : all) {
            if (candidate.getUniqueName().equals(key)) {
                return candidate;
            }
        }
        return null;
    }

    public static boolean isActive() {
        return structure != null;
    }

    public static boolean isFixed() {
        return fixed;
    }

    public static String multiblockName() {
        return name;
    }

    public static int multiblockIndex() {
        return index;
    }

    public static BlockPos center() {
        return center;
    }

    public static EnumFacing facing() {
        EnumFacing f = baseFacing;
        for (int i = 0; i < rotation; i++) {
            f = f.rotateY();
        }
        return f;
    }

    public static int rotation() {
        return rotation;
    }

    public static void cycleRotation() {
        rotation = (rotation + 1) % 4;
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
        // 30 格的射线（原版 objectMouseOver 只有 4.5 格）
        net.minecraft.util.math.Vec3d eyes = mc.player.getPositionEyes(1.0F);
        net.minecraft.util.math.Vec3d end = eyes.add(mc.player.getLookVec().scale(15.0D));
        RayTraceResult hit = mc.world.rayTraceBlocks(eyes, end, false, false, true);
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
            center = null;
            return;
        }
        center = hit.getBlockPos();
        // 投影相对玩家再转 90 度
        baseFacing = mc.player.getHorizontalFacing();
        lift = computeLift();
    }

    /** 从 0 开始找最小的上移量，使投影不碰到已有方块。 */
    private static int computeLift() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || center == null) {
            return 0;
        }
        for (int offset = 0; offset <= MAX_LIFT; offset++) {
            boolean blocked = false;
            for (StructureLayout.Placement p
                    : StructureLayout.layout(multiblock, wheelMultiblock, center, facing(), offset, true)) {
                IBlockState actual = mc.world.getBlockState(p.pos);
                if (!actual.getBlock().isAir(actual, mc.world, p.pos)) {
                    blocked = true;
                    break;
                }
            }
            if (!blocked) {
                return offset;
            }
        }
        return MAX_LIFT;
    }

    public static void fix() {
        if (center != null) {
            fixed = true;
        }
    }

    public static void cancel() {
        multiblock = null;
        structure = null;
        wheelStructure = null;
        center = null;
        fixed = false;
        lift = 0;
    }

    public static List<Entry> entries() {
        List<Entry> out = new ArrayList<>();
        if (multiblock == null || center == null) {
            return out;
        }
        Minecraft mc = Minecraft.getMinecraft();
        for (StructureLayout.Placement p
                : StructureLayout.layout(multiblock, wheelMultiblock, center, facing(), lift, true)) {
            if (mc.world != null && mc.world.getBlockState(p.pos) == p.state) {
                continue;
            }
            out.add(new Entry(p.pos, p.state));
        }
        return out;
    }

    public static final class Entry {
        public final BlockPos pos;
        public final IBlockState state;

        public Entry(BlockPos pos, IBlockState state) {
            this.pos = pos;
            this.state = state;
        }
    }
}
