package com.ieaddon.template12.client;

import blusunrize.immersiveengineering.api.ManualPageMultiblock;
import blusunrize.immersiveengineering.api.MultiblockHandler.IMultiblock;
import blusunrize.lib.manual.IManualPage;
import blusunrize.lib.manual.ManualInstance.ManualEntry;
import blusunrize.lib.manual.gui.GuiManual;
import com.ieaddon.template12.IEAddonTemplate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Mouse;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

/**
 * 1.12.2 版：在工程师手册的多方块页画一个"投影"按钮。
 *
 * <p>位置：直接复用 IE 画问号的坐标公式。
 * {@code ManualPageMultiblock} 里问号画在
 * {@code (x+116, y+yOffTotal/2-4)}，其中 {@code x = guiLeft+32}、{@code y = guiTop+28}，
 * {@code yOffTotal} 由 {@code transY/scale/结构尺寸} 算出。
 * 按钮就放在问号正上方 22 像素处。
 */
@Mod.EventBusSubscriber(modid = IEAddonTemplate.MODID, value = Side.CLIENT)
public final class ManualProjectionButton {
    private static final int BTN_SIZE = 18;
    private static final int GUI_WIDTH = 186;
    private static final int GUI_HEIGHT = 198;
    private static final int QUESTION_ABOVE = 22;

    private ManualProjectionButton() {
    }

    @SubscribeEvent
    public static void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        GuiScreen screen = event.getGui();
        if (!(screen instanceof GuiManual)) {
            return;
        }
        GuiManual gui = (GuiManual) screen;
        ManualPageMultiblock page = currentPage(gui);
        if (page == null) {
            return;
        }
        int[] pos = buttonPos(gui, page);
        int x = pos[0];
        int y = pos[1];

        gui.drawGradientRect(x, y, x + BTN_SIZE, y + BTN_SIZE, 0xCC204060, 0xCC204060);
        gui.drawGradientRect(x, y, x + BTN_SIZE, y + 1, 0xFF60A0FF, 0xFF60A0FF);
        Minecraft mc = Minecraft.getMinecraft();
        gui.drawCenteredString(mc.fontRenderer, "投", x + BTN_SIZE / 2, y + 5, 0xFFFFFF);
    }

    @SubscribeEvent
    public static void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        GuiScreen screen = event.getGui();
        if (!(screen instanceof GuiManual)) {
            return;
        }
        GuiManual gui = (GuiManual) screen;
        if (!Mouse.getEventButtonState() || Mouse.getEventButton() != 0) {
            return;
        }
        ManualPageMultiblock page = currentPage(gui);
        if (page == null) {
            return;
        }
        int[] pos = buttonPos(gui, page);
        int x = pos[0];
        int y = pos[1];

        Minecraft mc = Minecraft.getMinecraft();
        int mouseX = Mouse.getEventX() * gui.width / mc.displayWidth;
        int mouseY = gui.height - Mouse.getEventY() * gui.height / mc.displayHeight - 1;
        if (mouseX < x || mouseX >= x + BTN_SIZE || mouseY < y || mouseY >= y + BTN_SIZE) {
            return;
        }

        event.setCanceled(true);
        startProjection(mc, page);
        mc.displayGuiScreen(null);
    }

    private static void startProjection(Minecraft mc, ManualPageMultiblock page) {
        IMultiblock multiblock = pageMultiblock(page);
        if (multiblock == null) {
            return;
        }
        ClientProjection.begin(multiblock);
        if (mc.player != null) {
            mc.player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "投影已生成：准星定位，右键自动搭建，左键取消"));
        }
    }

    /** 按钮左上角：问号正上方。 */
    private static int[] buttonPos(GuiManual gui, ManualPageMultiblock page) {
        int guiLeft = (gui.width - GUI_WIDTH) / 2;
        int guiTop = (gui.height - GUI_HEIGHT) / 2;
        int pageX = guiLeft + 32;
        int pageY = guiTop + 28;

        int questionX = pageX + 116;
        int questionY = pageY + 60; // 兜底：结构数据读不到时的默认值

        try {
            Field transYField = ManualPageMultiblock.class.getDeclaredField("transY");
            transYField.setAccessible(true);
            float transY = (float) transYField.get(page);

            Field scaleField = ManualPageMultiblock.class.getDeclaredField("scale");
            scaleField.setAccessible(true);
            float scale = (float) scaleField.get(page);

            Field renderInfoField = ManualPageMultiblock.class.getDeclaredField("renderInfo");
            renderInfoField.setAccessible(true);
            Object renderInfo = renderInfoField.get(page);
            int h = intField(renderInfo, "structureHeight");
            int w = intField(renderInfo, "structureWidth");
            int l = intField(renderInfo, "structureLength");

            int yOffTotal = (int) (transY - pageY + scale * Math.sqrt(h * h + w * w + l * l) / 2);
            questionY = pageY + yOffTotal / 2 - 4;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // 反射失败就用默认位置
        }

        int buttonX = questionX - BTN_SIZE / 2;
        int buttonY = questionY - QUESTION_ABOVE;
        buttonX = Math.max(guiLeft + 4, Math.min(buttonX, guiLeft + GUI_WIDTH - BTN_SIZE - 4));
        buttonY = Math.max(guiTop + 4, buttonY);
        return new int[]{buttonX, buttonY};
    }

    private static int intField(Object owner, String name) throws ReflectiveOperationException {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(owner);
    }

    @Nullable
    private static ManualPageMultiblock currentPage(GuiManual gui) {
        try {
            ManualEntry entry = gui.getManual().getEntry(gui.getSelectedEntry());
            if (entry == null) {
                return null;
            }
            IManualPage[] pages = entry.getPages();
            if (pages == null || gui.page < 0 || gui.page >= pages.length) {
                return null;
            }
            IManualPage page = pages[gui.page];
            return page instanceof ManualPageMultiblock ? (ManualPageMultiblock) page : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    @Nullable
    private static IMultiblock pageMultiblock(ManualPageMultiblock page) {
        try {
            Field field = ManualPageMultiblock.class.getDeclaredField("multiblock");
            field.setAccessible(true);
            return (IMultiblock) field.get(page);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }
}
