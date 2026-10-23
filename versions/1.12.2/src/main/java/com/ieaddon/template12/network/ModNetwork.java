package com.ieaddon.template12.network;

import com.ieaddon.template12.IEAddonTemplate;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/** 极简网络通道：只有一个"自动搭建"请求。 */
public final class ModNetwork {
    public static final SimpleNetworkWrapper CHANNEL =
            NetworkRegistry.INSTANCE.newSimpleChannel(IEAddonTemplate.MODID);

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(BuildRequestMessage.Handler.class, BuildRequestMessage.class, 0, Side.SERVER);
        CHANNEL.registerMessage(MissingMessage.Handler.class, MissingMessage.class, 1, Side.CLIENT);
    }
}
