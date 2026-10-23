package com.ieaddon.template.multiblock;

import com.ieaddon.template.IEAddonTemplate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 服务端投影状态：每个玩家最多一个投影。
 *
 * <p>投影本身是可视化的东西，客户端会自己算一份用于渲染；
 * 服务端这份只负责"玩家放方块时判断有没有放对"和"放齐后成型"。
 * 所以不需要网络同步投影数据，一次客户端 → 服务端的"开始投影"请求就够了。
 */
public final class ProjectionManager {
    /** 玩家离开投影超过这个距离（格）就自动取消，防止投影被当免费远程预览用。 */
    public static final int MAX_DISTANCE = 15;

    private static final Map<UUID, ProjectionData> PROJECTIONS = new HashMap<>();

    private ProjectionManager() {
    }

    /** 每 tick 检查玩家是否走远；走远就静默取消并提示。 */
    public static void checkDistance(ServerPlayer player) {
        ProjectionData data = PROJECTIONS.get(player.getUUID());
        if (data == null) {
            return;
        }
        if (player.blockPosition().distSqr(data.origin()) > (double) MAX_DISTANCE * MAX_DISTANCE) {
            PROJECTIONS.remove(player.getUUID());
            player.displayClientMessage(
                    Component.translatable("multiblock.ieaddon.projection_too_far", MAX_DISTANCE), true);
        }
    }

    public static void create(ServerPlayer player, ProjectionData data) {
        Level level = player.level();
        // 结构已经完整（比如玩家自己搭好了），直接成型
        if (data.isComplete(level)) {
            data.form(level, player);
            return;
        }
        PROJECTIONS.put(player.getUUID(), data);
        player.displayClientMessage(
                Component.translatable("multiblock.ieaddon.projection_started",
                        data.multiblock().getDisplayName(),
                        data.remaining(level)),
                false);
        if (IEAddonTemplate.LOGGER.isDebugEnabled()) {
            IEAddonTemplate.LOGGER.debug("Projection started for {}: {} at {}",
                    player.getName().getString(), data.multiblock().getUniqueName(), data.origin());
        }
    }

    @Nullable
    public static ProjectionData get(ServerPlayer player) {
        return PROJECTIONS.get(player.getUUID());
    }

    public static void onBlockPlaced(ServerPlayer player, BlockPos pos) {
        ProjectionData data = PROJECTIONS.get(player.getUUID());
        if (data == null || !data.contains(pos)) {
            return;
        }
        Level level = player.level();
        if (data.isComplete(level)) {
            data.form(level, player);
            PROJECTIONS.remove(player.getUUID());
            player.displayClientMessage(
                    Component.translatable("multiblock.ieaddon.projection_complete",
                            data.multiblock().getDisplayName()),
                    false);
        }
    }

    public static void cancel(ServerPlayer player) {
        if (PROJECTIONS.remove(player.getUUID()) != null) {
            player.displayClientMessage(
                    Component.translatable("multiblock.ieaddon.projection_cancelled"), true);
        }
    }

    /** 退出登录时静默清理，不发消息（连接可能已经断开）。 */
    public static void remove(ServerPlayer player) {
        PROJECTIONS.remove(player.getUUID());
    }

    public static void clear() {
        PROJECTIONS.clear();
    }
}
