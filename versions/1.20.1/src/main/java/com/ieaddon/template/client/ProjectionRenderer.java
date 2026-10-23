package com.ieaddon.template.client;

import com.ieaddon.template.IEAddonTemplate;
import com.ieaddon.template.multiblock.ProjectionData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 投影渲染：给还没放对的每一格画绿色线框。
 *
 * <p>用线框而不是半透明方块，原因：
 * <ul>
 *   <li>不需要处理 RenderType 的 alpha 排序；</li>
 *   <li>不会和方块本身的光照/遮挡打架；</li>
 *   <li>玩家能透过线框看清里面已经放了什么。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = IEAddonTemplate.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ProjectionRenderer {
    private ProjectionRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        ProjectionData data = ClientProjection.getForRender();
        Level level = Minecraft.getInstance().level;
        if (data == null || level == null) {
            return;
        }

        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);

        List<BlockPos> positions = data.positions();
        List<BlockState> expected = data.expected();
        for (int i = 0; i < positions.size(); i++) {
            BlockPos pos = positions.get(i);
            if (level.getBlockState(pos).equals(expected.get(i))) {
                continue;
            }
            LevelRenderer.renderLineBox(pose, lines, new AABB(pos).deflate(0.005),
                    0.25f, 1.0f, 0.5f, 0.9f);
        }

        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        // 只在"已固定"阶段检查完成；跟随阶段的投影不参与完成判定
        if (!ClientProjection.isActive()) {
            return;
        }
        ProjectionData data = ClientProjection.getForRender();
        Level level = Minecraft.getInstance().level;
        Player player = Minecraft.getInstance().player;
        if (data == null || level == null || player == null) {
            return;
        }
        // 走远 15 格：投影消失
        if (player.blockPosition().distSqr(data.origin())
                > (double) ClientProjection.MAX_DISTANCE * ClientProjection.MAX_DISTANCE) {
            ClientProjection.clear();
            return;
        }
        if (data.isComplete(level)) {
            ClientProjection.clear();
        }
    }
}
