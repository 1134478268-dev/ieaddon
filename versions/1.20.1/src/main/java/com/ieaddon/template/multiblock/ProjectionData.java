package com.ieaddon.template.multiblock;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 一次"投影"：某个多方块在世界上的一组目标位置 + 期望方块状态。
 *
 * <p>投影不做任何方块修改，只用来：
 * <ul>
 *   <li>客户端画出半透明轮廓；</li>
 *   <li>服务端在玩家放置方块时判断"这一格是不是投影的一部分"；</li>
 *   <li>全部就位后调用 {@code createStructure} 成型。</li>
 * </ul>
 */
public final class ProjectionData {
    private final IMultiblock multiblock;
    private final BlockPos origin;
    private final Rotation rotation;
    private final Mirror mirror;
    private final Direction face;
    private final List<BlockPos> positions = new ArrayList<>();
    private final List<BlockState> expected = new ArrayList<>();

    private ProjectionData(IMultiblock multiblock, BlockPos origin, Rotation rotation, Mirror mirror, Direction face) {
        this.multiblock = multiblock;
        this.origin = origin;
        this.rotation = rotation;
        this.mirror = mirror;
        this.face = face;
    }

    /** 由结构 + 原点 + 朝向构造投影。会读取 IE 的 NBT 模板。 */
    public static ProjectionData create(IMultiblock multiblock, BlockPos origin, Rotation rotation,
                                        Mirror mirror, Direction face, Level level) {
        ProjectionData data = new ProjectionData(multiblock, origin, rotation, mirror, face);
        StructurePlaceSettings settings = new StructurePlaceSettings().setMirror(mirror).setRotation(rotation);
        for (StructureBlockInfo info : multiblock.getStructure(level)) {
            data.positions.add(origin.offset(StructureTemplate.calculateRelativePosition(settings, info.pos())));
            data.expected.add(info.state().mirror(mirror).rotate(rotation));
        }
        return data;
    }

    public boolean contains(BlockPos pos) {
        return positions.contains(pos);
    }

    public int remaining(Level level) {
        int count = 0;
        for (int i = 0; i < positions.size(); i++) {
            if (!level.getBlockState(positions.get(i)).equals(expected.get(i))) {
                count++;
            }
        }
        return count;
    }

    public boolean isComplete(Level level) {
        return remaining(level) == 0;
    }

    /** 全部就位后交给 IE 成型。 */
    public void form(Level level, Player player) {
        StructurePlaceSettings settings = new StructurePlaceSettings().setMirror(mirror).setRotation(rotation);
        BlockPos trigger = origin.offset(
                StructureTemplate.calculateRelativePosition(settings, multiblock.getTriggerOffset()));
        multiblock.createStructure(level, trigger, face, player);
    }

    public IMultiblock multiblock() {
        return multiblock;
    }

    public BlockPos origin() {
        return origin;
    }

    public Rotation rotation() {
        return rotation;
    }

    public Mirror mirror() {
        return mirror;
    }

    public Direction face() {
        return face;
    }

    public List<BlockPos> positions() {
        return positions;
    }

    public List<BlockState> expected() {
        return expected;
    }
}
