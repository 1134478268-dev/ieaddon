package com.ieaddon.template12.network;

import com.ieaddon.template12.multiblock.BuildService;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 客户端 → 服务端：请求自动摆放某个多方块结构。
 *
 * <p>同时带索引和名字：服务端先按名字（忽略大小写）找，找不到再按索引，
 * 双保险，避免客户端/服务端实例不一致。
 */
public class BuildRequestMessage implements IMessage {
    private int index;
    private String name;
    private BlockPos center;
    private EnumFacing facing;
    private int lift;

    public BuildRequestMessage() {
    }

    public BuildRequestMessage(int index, String name, BlockPos center, EnumFacing facing, int lift) {
        this.index = index;
        this.name = name == null ? "" : name;
        this.center = center;
        this.facing = facing;
        this.lift = lift;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        index = buf.readInt();
        name = ByteBufUtils.readUTF8String(buf);
        center = BlockPos.fromLong(buf.readLong());
        facing = EnumFacing.values()[buf.readByte()];
        lift = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(index);
        ByteBufUtils.writeUTF8String(buf, name);
        buf.writeLong(center.toLong());
        buf.writeByte(facing.ordinal());
        buf.writeInt(lift);
    }

    public static class Handler implements IMessageHandler<BuildRequestMessage, IMessage> {
        @Override
        public IMessage onMessage(BuildRequestMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            final String name = message.name;
            final int index = message.index;
            final BlockPos center = message.center;
            final EnumFacing facing = message.facing;
            final int lift = message.lift;
            player.getServerWorld().addScheduledTask(() ->
                    BuildService.build(player, index, name, center, facing, lift));
            return null;
        }
    }
}
