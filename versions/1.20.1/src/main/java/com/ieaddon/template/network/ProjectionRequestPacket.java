package com.ieaddon.template.network;

import com.ieaddon.template.multiblock.ProjectionData;
import com.ieaddon.template.multiblock.ProjectionManager;
import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import blusunrize.immersiveengineering.api.multiblocks.TemplateMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：在手册里点了"投影"按钮。
 *
 * <p>只带参数，不成型、不改方块；服务端据此建立放置检测状态。
 */
public record ProjectionRequestPacket(ResourceLocation structure, BlockPos origin,
                                      byte rotation, byte mirror, byte face) {

    public static void encode(ProjectionRequestPacket msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.structure);
        buf.writeBlockPos(msg.origin);
        buf.writeByte(msg.rotation);
        buf.writeByte(msg.mirror);
        buf.writeByte(msg.face);
    }

    public static ProjectionRequestPacket decode(FriendlyByteBuf buf) {
        return new ProjectionRequestPacket(buf.readResourceLocation(), buf.readBlockPos(),
                buf.readByte(), buf.readByte(), buf.readByte());
    }

    public static void handle(ProjectionRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            IMultiblock multiblock = MultiblockHandler.getByUniqueName(msg.structure);
            if (!(multiblock instanceof TemplateMultiblock)) {
                return;
            }
            Rotation rotation = Rotation.values()[Math.floorMod(msg.rotation, Rotation.values().length)];
            Mirror mirror = Mirror.values()[Math.floorMod(msg.mirror, Mirror.values().length)];
            Direction face = Direction.values()[Math.floorMod(msg.face, Direction.values().length)];

            ProjectionData data = ProjectionData.create(multiblock, msg.origin, rotation, mirror, face, player.level());
            ProjectionManager.create(player, data);
        });
        ctx.get().setPacketHandled(true);
    }
}
