package com.ieaddon.template.client;

import com.ieaddon.template.IEAddonTemplate;
import com.ieaddon.template.multiblock.ProjectionData;
import com.ieaddon.template.network.ModNetwork;
import com.ieaddon.template.network.ProjectionRequestPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * 投影放置阶段：跟随准星，右键固定，左键取消。
 *
 * <p>用 {@link InputEvent.MouseButton.Pre} 而不是 RightClickBlock：
 * <ul>
 *   <li>瞄准空气时也能取消；</li>
 *   <li>固定投影那一刻不应该触发原版的方块交互。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = IEAddonTemplate.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ProjectionPlacementHandler {
    private ProjectionPlacementHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (ClientProjection.isPlacing()) {
            ClientProjection.updatePending();
        }
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (!ClientProjection.isPlacing()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            ProjectionData pending = ClientProjection.getPending();
            if (pending == null) {
                return; // 准星没对准方块，不固定
            }
            ModNetwork.CHANNEL.sendToServer(new ProjectionRequestPacket(
                    pending.multiblock().getUniqueName(),
                    pending.origin(),
                    (byte) pending.rotation().ordinal(),
                    (byte) pending.mirror().ordinal(),
                    (byte) pending.face().ordinal()));
            ClientProjection.fix();
            event.setCanceled(true);
        } else if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            ClientProjection.cancel();
            event.setCanceled(true);
        }
    }
}
