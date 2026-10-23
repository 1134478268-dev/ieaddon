package com.ieaddon.template12.client;

import com.ieaddon.template12.IEAddonTemplate;
import com.ieaddon.template12.network.BuildRequestMessage;
import com.ieaddon.template12.network.ModNetwork;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.Minecraft;
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

        GlStateManager.pushMatrix();
        GlStateManager.translate(-px, -py, -pz);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.disableLighting();
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);

        // IE 的机器方块用 TESR 渲染，BlockRendererDispatcher 只会画出 missing model（紫黑格），
        // 所以投影改用纯色半透明立方体
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (ClientProjection.Entry entry : entries) {
            drawCube(buffer, entry.pos.getX(), entry.pos.getY(), entry.pos.getZ());
        }
        tessellator.draw();

        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.popMatrix();
    }

    private static void drawCube(BufferBuilder buf, double x, double y, double z) {
        double x2 = x + 1.0D;
        double y2 = y + 1.0D;
        double z2 = z + 1.0D;
        // 下 / 上 / 北 / 南 / 西 / 东
        quad(buf, x, y, z, x2, y, z, x2, y, z2, x, y, z2);
        quad(buf, x, y2, z, x, y2, z2, x2, y2, z2, x2, y2, z);
        quad(buf, x, y, z, x, y2, z, x2, y2, z, x2, y, z);
        quad(buf, x2, y, z2, x2, y2, z2, x, y2, z2, x, y, z2);
        quad(buf, x, y, z2, x, y2, z2, x, y2, z, x, y, z);
        quad(buf, x2, y, z, x2, y2, z, x2, y2, z2, x2, y, z2);
    }

    private static void quad(BufferBuilder buf,
                             double x1, double y1, double z1,
                             double x2, double y2, double z2,
                             double x3, double y3, double z3,
                             double x4, double y4, double z4) {
        buf.pos(x1, y1, z1).color(0.45f, 0.72f, 1.0f, 0.30f).endVertex();
        buf.pos(x2, y2, z2).color(0.45f, 0.72f, 1.0f, 0.30f).endVertex();
        buf.pos(x3, y3, z3).color(0.45f, 0.72f, 1.0f, 0.30f).endVertex();
        buf.pos(x4, y4, z4).color(0.45f, 0.72f, 1.0f, 0.30f).endVertex();
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
