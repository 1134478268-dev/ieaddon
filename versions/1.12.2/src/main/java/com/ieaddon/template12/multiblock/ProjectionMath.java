package com.ieaddon.template12.multiblock;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/**
 * 投影/搭建共用的坐标映射，客户端和服务端必须完全一致。
 *
 * <p>与 IE 的 {@code createStructure} 完全对齐：
 * {@code pos.offset(side, l).offset(side.rotateY(), w).add(0, h, 0)}
 */
public final class ProjectionMath {
    private ProjectionMath() {
    }

    /** dx 沿朝向，dz 沿"右"（facing.rotateY），dy 沿 y。 */
    public static BlockPos worldPos(BlockPos center, EnumFacing facing, int dx, int dy, int dz) {
        return center.offset(facing, dx).offset(facing.rotateY(), dz).add(0, dy, 0);
    }
}
