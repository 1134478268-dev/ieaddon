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
            System.out.println("[ieaddon] client received missing list, size=" + message.stacks.size());
            Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayer player = Minecraft.getMinecraft().player;
                if (player == null) {
                    return;
                }
                player.sendMessage(new TextComponentString(TextFormatting.RED + "材料不足，还缺："));
                for (int i = 0; i < stacks.size(); i++) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GRAY + "  " + counts.get(i) + "x " + displayName(stacks.get(i))));
                }
            });
            return null;
        }

        /** 优先原版显示名；未翻译时回退尝试 registry-name 形式的键，最后退回短 id。 */
        private static String displayName(ItemStack stack) {
            String name = stack.getDisplayName();
            if (!name.startsWith("tile.") && !name.startsWith("item.")) {
                return name;
            }
            String id = stack.getItem().getRegistryName() == null
                    ? "unknown"
                    : stack.getItem().getRegistryName().toString();
            String base = stack.getItem().getTranslationKey(stack);
            String[] keys = {
                    base + "." + id + ".name",
                    "tile." + id + ".name",
                    "item." + id + ".name",
                    "tile." + id.replace(':', '.') + ".name",
                    "item." + id.replace(':', '.') + ".name"
            };
            for (String key : keys) {
                String translated = net.minecraft.client.resources.I18n.format(key);
                if (!translated.equals(key)) {
                    return translated;
                }
            }
            return id;
        }
    }
}
