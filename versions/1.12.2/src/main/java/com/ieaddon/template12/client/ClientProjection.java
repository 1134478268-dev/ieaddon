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
    private static int index = -1;
    private static ItemStack[][][] structure;
    private static ItemStack[][][] wheelStructure;
    private static String name = "";
    private static BlockPos center;
    private static EnumFacing facing = EnumFacing.NORTH;
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
        } else {
            structure = resolved.getStructureManual();
            wheelStructure = null;
        }
        center = null;
        fixed = false;
        lift = 0;
        Minecraft mc = Minecraft.getMinecraft();
        facing = mc.player != null ? mc.player.getHorizontalFacing() : EnumFacing.NORTH;
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
        center = mc.objectMouseOver.getBlockPos();
        if (mc.player != null) {
            facing = mc.player.getHorizontalFacing();
        }
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
            for (Entry entry : entries(offset)) {
                IBlockState actual = mc.world.getBlockState(entry.pos);
                if (!actual.getBlock().isAir(actual, mc.world, entry.pos)) {
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
        return entries(lift);
    }

    private static List<Entry> entries(int yOffset) {
        List<Entry> out = new ArrayList<>();
        if (structure == null || center == null) {
            return out;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (name.contains("Excavator")) {
            addExcavator(out, mc, yOffset);
            addWheel(out, mc, yOffset);
        } else {
            addGeneric(out, mc, yOffset);
        }
        return out;
    }

    private static void addExcavator(List<Entry> out, Minecraft mc, int yOffset) {
        int sizeH = structure.length;
        int sizeL = structure[0].length;
        int sizeW = structure[0][0].length;
        for (int l = 0; l < sizeL; l++) {
            for (int w = 0; w < sizeW; w++) {
                for (int h = 0; h < sizeH; h++) {
                    if (l > 0 && w == sizeW / 2) {
                        continue;
                    }
                    ItemStack stack = structure[h][l][w];
                    addIfMissing(out, mc, stack, l, h - sizeH / 2 + yOffset, w - sizeW / 2, center);
                }
            }
        }
    }

    private static void addWheel(List<Entry> out, Minecraft mc, int yOffset) {
        if (wheelStructure == null) {
            return;
        }
        BlockPos wheelCenter = center.offset(facing, 4);
        int sizeH = wheelStructure.length;
        int sizeW = wheelStructure[0].length;
        EnumFacing wheelSide = facing.rotateYCCW();
        int wx = wheelSide == EnumFacing.NORTH ? 1 : wheelSide == EnumFacing.SOUTH ? -1 : 0;
        int wz = wheelSide == EnumFacing.WEST ? 1 : wheelSide == EnumFacing.EAST ? -1 : 0;
        for (int h = 0; h < sizeH; h++) {
            for (int w = 0; w < sizeW; w++) {
                ItemStack stack = wheelStructure[h][w][0];
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                IBlockState expected = expectedState(stack);
                if (expected == null) {
                    continue;
                }
                int off = w - sizeW / 2;
                BlockPos pos = wheelCenter.add(wx * off, h - sizeH / 2 + yOffset, wz * off);
                if (mc.world != null && mc.world.getBlockState(pos) == expected) {
                    continue;
                }
                out.add(new Entry(pos, expected));
            }
        }
    }

    private static void addGeneric(List<Entry> out, Minecraft mc, int yOffset) {
        int sizeH = structure.length;
        int sizeL = structure[0].length;
        int sizeW = structure[0][0].length;
        for (int h = 0; h < sizeH; h++) {
            for (int l = 0; l < sizeL; l++) {
                for (int w = 0; w < sizeW; w++) {
                    ItemStack stack = structure[h][l][w];
                    addIfMissing(out, mc, stack, l - sizeL / 2, h - sizeH / 2 + yOffset, w - sizeW / 2, center);
                }
            }
        }
    }

    /** 传送带不投影，由玩家自己放（IE 的 conveyor 状态在 NBT 里，自动摆容易朝向错）。 */
    private static boolean isConveyor(ItemStack stack) {
        net.minecraft.util.ResourceLocation id = stack.getItem().getRegistryName();
        return id != null && id.getPath().contains("conveyor");
    }

    private static void addIfMissing(List<Entry> out, Minecraft mc, ItemStack stack,
                                     int dx, int dy, int dz, BlockPos base) {
        if (stack == null || stack.isEmpty() || isConveyor(stack)) {
            return;
        }
        IBlockState expected = expectedState(stack);
        if (expected == null) {
            return;
        }
        BlockPos pos = ProjectionMath.worldPos(base, facing, dx, dy, dz);
        if (mc.world != null && mc.world.getBlockState(pos) == expected) {
            return;
        }
        out.add(new Entry(pos, expected));
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

    public static final class Entry {
        public final BlockPos pos;
        public final IBlockState state;

        public Entry(BlockPos pos, IBlockState state) {
            this.pos = pos;
            this.state = state;
        }
    }
}
