package com.ieaddon.template12.multiblock;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/**
 * 一个玩家的"逐块搭建"任务：每 tick 放 1~2 个方块，而不是瞬间全部放完。
 */
public final class BuildTask {
    public final UUID playerId;
    public final World world;
    private final Deque<Placement> queue = new ArrayDeque<>();
    public int placed;
    public int skipped;
    public final int total;
    public final net.minecraft.util.math.BlockPos anchor;

    public BuildTask(UUID playerId, World world, net.minecraft.util.math.BlockPos anchor) {
        this.playerId = playerId;
        this.world = world;
        this.total = 0;
        this.anchor = anchor;
    }

    public void enqueue(BlockPos pos, IBlockState state) {
        queue.addLast(new Placement(pos, state));
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /** 放一个方块；被非空气方块挡住时跳过，不覆盖。返回是否真的放置了。 */
    public boolean step() {
        Placement p = queue.pollFirst();
        if (p == null) {
            return false;
        }
        if (world.getBlockState(p.pos) == p.state) {
            return false;
        }
        if (!world.getBlockState(p.pos).getBlock().isReplaceable(world, p.pos)) {
            skipped++;
            return false;
        }
        world.setBlockState(p.pos, p.state, 3);
        placed++;
        return true;
    }

    public static final class Placement {
        public final BlockPos pos;
        public final IBlockState state;

        Placement(BlockPos pos, IBlockState state) {
            this.pos = pos;
            this.state = state;
        }
    }
}
