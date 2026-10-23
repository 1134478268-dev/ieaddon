package com.ieaddon.template12.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.resources.IResource;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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

        private static Map<String, String> LANG;

        /** 读 IE 自己的 zh_cn.lang，按 registryName 反查中文名。 */
        private static Map<String, String> lang() {
            if (LANG != null) {
                return LANG;
            }
            LANG = new HashMap<>();
            try {
                IResource res = Minecraft.getMinecraft().getResourceManager()
                        .getResource(new ResourceLocation("immersiveengineering", "lang/zh_cn.lang"));
                if (res != null) {
                    try (InputStream in = res.getInputStream()) {
                        java.io.BufferedReader reader = new java.io.BufferedReader(
                                new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            line = line.trim();
                            int eq = line.indexOf('=');
                            if (eq > 0 && !line.startsWith("#")) {
                                LANG.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                // 读不到就用原版显示名
            }
            return LANG;
        }

        private static String displayName(ItemStack stack) {
            String name = stack.getDisplayName();
            if (!name.startsWith("tile.") && !name.startsWith("item.")) {
                return name;
            }
            String id = stack.getItem().getRegistryName() == null
                    ? ""
                    : stack.getItem().getRegistryName().toString();
            if (!id.isEmpty()) {
                for (Map.Entry<String, String> e : lang().entrySet()) {
                    if (e.getKey().endsWith(".name") && e.getKey().contains(id)) {
                        return e.getValue();
                    }
                }
            }
            return id.isEmpty() ? name : id;
        }
    }
}
