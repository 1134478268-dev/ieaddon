package com.ieaddon.template.event;

import com.ieaddon.template.IEAddonTemplate;
import com.ieaddon.template.multiblock.ProjectionManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 投影的放置检测与清理。
 *
 * <p>玩家在投影范围内放方块时，检查投影是否已经全部就位；就位则自动成型。
 */
@Mod.EventBusSubscriber(modid = IEAddonTemplate.MODID)
public final class ProjectionEvents {
    private ProjectionEvents() {
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        ProjectionManager.onBlockPlaced(player, event.getPos());
    }

    /** 玩家离开投影 15 格自动取消。 */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (event.player instanceof ServerPlayer player) {
            ProjectionManager.checkDistance(player);
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ProjectionManager.remove(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ProjectionManager.clear();
    }
}
