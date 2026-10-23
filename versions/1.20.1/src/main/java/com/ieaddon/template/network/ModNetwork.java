package com.ieaddon.template.network;

import com.ieaddon.template.IEAddonTemplate;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 极简网络通道：目前只有一个"开始投影"的 C→S 包。
 *
 * <p>投影的渲染数据由客户端自己算（结构模板客户端本来就有），
 * 所以不需要 S→C 同步。
 */
public final class ModNetwork {
    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(IEAddonTemplate.MODID, "main"))
            .networkProtocolVersion(() -> VERSION)
            .clientAcceptedVersions(VERSION::equals)
            .serverAcceptedVersions(VERSION::equals)
            .simpleChannel();

    private ModNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, ProjectionRequestPacket.class,
                ProjectionRequestPacket::encode,
                ProjectionRequestPacket::decode,
                ProjectionRequestPacket::handle);
    }
}
