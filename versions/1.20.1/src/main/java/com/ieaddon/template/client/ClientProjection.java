package com.ieaddon.template.client;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import blusunrize.immersiveengineering.api.utils.DirectionUtils;
import com.ieaddon.template.multiblock.ProjectionData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;

/**
 * 客户端投影状态机。
 *
 * <pre>
 * 手册点 [投影]
 *   → PLACING：投影跟随准星（每 tick 用 hitResult 重算位置）
 *   → 右键：固定，进入 ACTIVE，发请求给服务端
 *   → 左键：取消
 * </pre>
 *
 * <p>PLACING 阶段不通知服务端，纯客户端预览；右键固定时才发一个包。
 */
public final class ClientProjection {
    /** 与服务端的 ProjectionManager.MAX_DISTANCE 保持一致。 */
    public static final int MAX_DISTANCE = 15;

    @Nullable
    private static IMultiblock placing;
    /** 跟随准星的临时投影。 */
    @Nullable
    private static ProjectionData pending;
    /** 已固定的投影，等待玩家放方块。 */
    @Nullable
    private static ProjectionData active;

    private ClientProjection() {
    }

    public static void beginPlacing(IMultiblock multiblock) {
        placing = multiblock;
        pending = null;
        active = null;
    }

    public static boolean isPlacing() {
        return placing != null;
    }

    public static boolean isActive() {
        return active != null;
    }

    /** 每 tick 调用：用准星命中的方块重算投影位置。 */
    public static void updatePending() {
        Minecraft minecraft = Minecraft.getInstance();
        if (placing == null || minecraft.level == null) {
            pending = null;
            return;
        }
        HitResult hit = minecraft.hitResult;
        if (!(hit instanceof BlockHitResult blockHit)) {
            pending = null;
            return;
        }
        BlockPos clicked = blockHit.getBlockPos();
        Direction face = blockHit.getDirection();
        Rotation rotation = DirectionUtils.getRotationBetweenFacings(Direction.NORTH, face.getOpposite());
        if (rotation == null) {
            pending = null;
            return;
        }
        // 和 createStructure 一样：以命中方块为"触发方块"，反推模板原点
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
        BlockPos origin = clicked.subtract(
                StructureTemplate.calculateRelativePosition(settings, placing.getTriggerOffset()));
        try {
            pending = ProjectionData.create(placing, origin, rotation, Mirror.NONE, face, minecraft.level);
        } catch (RuntimeException ex) {
            pending = null; // 客户端模板未同步
        }
    }

    /** 固定当前跟随的投影。 */
    public static boolean fix() {
        if (pending == null) {
            return false;
        }
        active = pending;
        pending = null;
        placing = null;
        return true;
    }

    public static void cancel() {
        placing = null;
        pending = null;
        active = null;
    }

    public static void clear() {
        cancel();
    }

    /** 渲染用：优先画已固定的，否则画跟随中的。 */
    @Nullable
    public static ProjectionData getForRender() {
        return active != null ? active : pending;
    }

    /** 固定时用于发送请求的投影。 */
    @Nullable
    public static ProjectionData getPending() {
        return pending;
    }
}
