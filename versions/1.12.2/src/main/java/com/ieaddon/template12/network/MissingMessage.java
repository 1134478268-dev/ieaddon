package com.ieaddon.template12.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端 → 客户端：材料不足清单。
 *
 * <p>在客户端显示，这样能拿到客户端语言表，物品名才能正确翻译。
 */
public class MissingMessage implements IMessage {
    private List<ItemStack> stacks = new ArrayList<>();
    private List<Integer> counts = new ArrayList<>();

    public MissingMessage() {
    }

    public MissingMessage(List<ItemStack> stacks, List<Integer> counts) {
        this.stacks = stacks;
        this.counts = counts;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            stacks.add(ByteBufUtils.readItemStack(buf));
            counts.add(buf.readInt());
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(stacks.size());
        for (int i = 0; i < stacks.size(); i++) {
            ByteBufUtils.writeItemStack(buf, stacks.get(i));
            buf.writeInt(counts.get(i));
        }
    }

    public static class Handler implements IMessageHandler<MissingMessage, IMessage> {
        @Override
        public IMessage onMessage(MissingMessage message, MessageContext ctx) {
            List<ItemStack> stacks = message.stacks;
            List<Integer> counts = message.counts;
            Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayer player = Minecraft.getMinecraft().player;
                if (player == null) {
                    return;
                }
                player.sendMessage(new TextComponentString(TextFormatting.RED + "材料不足，还缺："));
                for (int i = 0; i < stacks.size(); i++) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GRAY + "  " + counts.get(i) + "x " + stacks.get(i).getDisplayName()));
                }
            });
            return null;
        }
    }
}
