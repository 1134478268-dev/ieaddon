package com.ieaddon.template.client;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import blusunrize.immersiveengineering.client.manual.ManualElementMultiblock;
import blusunrize.lib.manual.ManualEntry;
import blusunrize.lib.manual.SpecialManualElement;
import blusunrize.lib.manual.gui.ManualScreen;
import com.ieaddon.template.IEAddonTemplate;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;

/**
 * 在工程师手册的多方块预览页右上角加一个"投影"按钮。
 *
 * <p>不修改 IE 的任何类，只监听 Forge 的 Screen 事件：
 * <ul>
 *   <li>{@link ScreenEvent.Render.Post} 画按钮；</li>
 *   <li>{@link ScreenEvent.MouseButtonPressed.Pre} 检测点击。</li>
 * </ul>
 *
 * <p>判断"当前页是不是多方块页"：{@code ManualScreen.page} 与
 * {@code ManualEntry.getSpecialsByPage()} 都是 public 的；
 * 但 {@code ManualElementMultiblock.multiblock} 是 private，只能用反射读。
 * 反射失败时按钮不显示（不影响原版手册）。
 */
@Mod.EventBusSubscriber(modid = IEAddonTemplate.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ManualProjectionButton {
    private static final int BTN_WIDTH = 44;
    private static final int BTN_HEIGHT = 14;
    private static final int GUI_WIDTH = 186;
    private static final int GUI_HEIGHT = 198;

    private ManualProjectionButton() {
    }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof ManualScreen screen)) {
            return;
        }
        if (currentMultiblock(screen) == null) {
            return;
        }
        int x = buttonX(screen);
        int y = buttonY(screen);
        event.getGuiGraphics().fill(x, y, x + BTN_WIDTH, y + BTN_HEIGHT, 0xC0204060);
        event.getGuiGraphics().fill(x, y, x + BTN_WIDTH, y + 1, 0xFF60A0FF);
        event.getGuiGraphics().drawCenteredString(Minecraft.getInstance().font,
                Component.translatable("multiblock.ieaddon.project_button"),
                x + BTN_WIDTH / 2, y + 3, 0xFFFFFF);
    }

    @SubscribeEvent
    public static void onClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 0 || !(event.getScreen() instanceof ManualScreen screen)) {
            return;
        }
        IMultiblock multiblock = currentMultiblock(screen);
        if (multiblock == null) {
            return;
        }
        int x = buttonX(screen);
        int y = buttonY(screen);
        if (event.getMouseX() < x || event.getMouseX() >= x + BTN_WIDTH
                || event.getMouseY() < y || event.getMouseY() >= y + BTN_HEIGHT) {
            return;
        }

        if (Minecraft.getInstance().player == null) {
            return;
        }

        // 进入"跟随准星"阶段：位置每 tick 重算，右键才固定
        ClientProjection.beginPlacing(multiblock);
        Minecraft.getInstance().setScreen(null);
        event.setCanceled(true);
    }

    private static int buttonX(ManualScreen screen) {
        return (screen.width - GUI_WIDTH) / 2 + GUI_WIDTH - BTN_WIDTH - 8;
    }

    private static int buttonY(ManualScreen screen) {
        return (screen.height - GUI_HEIGHT) / 2 + 16;
    }

    private static IMultiblock currentMultiblock(ManualScreen screen) {
        try {
            ManualEntry entry = screen.getCurrentPage();
            if (entry == null) {
                return null;
            }
            SpecialManualElement special = entry.getSpecialsByPage().get(screen.page);
            if (!(special instanceof ManualElementMultiblock)) {
                return null;
            }
            Field field = ManualElementMultiblock.class.getDeclaredField("multiblock");
            field.setAccessible(true);
            return (IMultiblock) field.get(special);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }
}
