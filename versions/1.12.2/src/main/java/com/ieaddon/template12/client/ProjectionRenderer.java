package com.ieaddon.template12.client;

import com.ieaddon.template12.IEAddonTemplate;
import com.ieaddon.template12.network.BuildRequestMessage;
import com.ieaddon.template12.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * 1.12.2 的投影渲染：半透明方块预览。
 *
 * <p>用 {@link BlockRendererDispatcher#renderBlockBrightness} 直接画方块模型，
 * 关闭深度写入 + alpha 混合，得到"幽灵方块"效果。
 */
@Mod.EventBusSubscriber(modid = IEAddonTemplate.MODID, value = Side.CLIENT)
public final class ProjectionRenderer {
    private ProjectionRenderer() {
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderWorldLastEvent event) {
        if (!ClientProjection.isActive()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null || mc.world == null) {
            return;
        }
        List<ClientProjection.Entry> entries = ClientProjection.entries();
        if (entries.isEmpty()) {
            return;
        }

        double px = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.getPartialTicks();
        double py = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.getPartialTicks();
        double pz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.getPartialTicks();

        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.pushMatrix();
        GlStateManager.translate(-px, -py, -pz);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 0.30f);

        BlockRendererDispatcher dispatcher = mc.getBlockRendererDispatcher();
        for (ClientProjection.Entry entry : entries) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(entry.pos.getX(), entry.pos.getY(), entry.pos.getZ());
            // 注意：第二个参数是 0~1 的亮度，不是 int 光照值
            dispatcher.renderBlockBrightness(entry.state, mc.world.getLightBrightness(entry.pos));
            GlStateManager.popMatrix();
        }

        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.popMatrix();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && ClientProjection.isActive()) {
            ClientProjection.update();
        }
    }

    @SubscribeEvent
    public static void onMouse(MouseEvent event) {
        if (ClientProjection.isActive() && !ClientProjection.isFixed()
                && event.getButton() == 0 && event.isButtonstate()
                && Minecraft.getMinecraft().currentScreen == null) {
            ClientProjection.cancel();
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getWorld().isRemote || !ClientProjection.isActive()) {
            return;
        }
        if (ClientProjection.isFixed() || ClientProjection.center() == null) {
            return;
        }
        // 右键 = 请求服务端自动摆放（成型仍由玩家用锤子敲）
        ModNetwork.CHANNEL.sendToServer(new BuildRequestMessage(
                ClientProjection.multiblockIndex(),
                ClientProjection.multiblockName(),
                ClientProjection.center(),
                ClientProjection.facing(),
                ClientProjection.lift()));
        ClientProjection.cancel();
        event.setCanceled(true);
    }
}
