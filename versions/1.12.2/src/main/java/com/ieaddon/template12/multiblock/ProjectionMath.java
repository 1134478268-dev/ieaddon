package com.ieaddon.template12.multiblock;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/**
 * 投影/搭建共用的坐标映射，客户端和服务端必须完全一致。
 */
public final class ProjectionMath {
    private ProjectionMath() {
    }

    /** 把结构数组里的相对坐标，按朝向旋转后映射到世界坐标。 */
    public static BlockPos worldPos(BlockPos center, EnumFacing facing, int dx, int dy, int dz) {
        int rx;
        int rz;
        switch (facing) {
            case SOUTH:
                rx = -dx;
                rz = -dz;
                break;
            case EAST:
                rx = dz;
                rz = -dx;
                break;
            case WEST:
                rx = -dz;
                rz = dx;
                break;
            case NORTH:
            default:
                rx = dx;
                rz = dz;
                break;
        }
        return center.add(rx, dy, rz);
    }
}
