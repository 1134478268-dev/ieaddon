package com.ieaddon.template12.client;

import com.ieaddon.template12.IEAddonTemplate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;

/**
 * 客户端按键：R 键循环切换投影方向（0°/90°/180°/270°）。
 */
@Mod.EventBusSubscriber(modid = IEAddonTemplate.MODID, value = Side.CLIENT)
public final class ClientKeybindings {
    public static final KeyBinding ROTATE = new KeyBinding(
            "key.ieaddon.rotate", Keyboard.KEY_R, "key.categories.ieaddon");

    private ClientKeybindings() {
    }

    public static void register() {
        ClientRegistry.registerKeyBinding(ROTATE);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!ROTATE.isPressed()) {
            return;
        }
        if (!ClientProjection.isActive()) {
            return;
        }
        ClientProjection.cycleRotation();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player != null) {
            int deg = ClientProjection.rotation() * 90;
            mc.player.sendMessage(new TextComponentString(
                    TextFormatting.AQUA + "投影方向: " + deg + "°（R 键继续切换）"));
        }
    }
}
