package com.ieaddon.template.event;

import com.ieaddon.template.IEAddonTemplate;
import com.ieaddon.template.multiblock.MultiblockBuildHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 工程师手册的 Shift + 右键入口。
 *
 * <p>为什么可行：IE 1.20.1 的 {@code ManualItem} 没有针对 Shift 的任何特判——
 * <ul>
 *   <li>{@code use()} 直接打开手册 GUI，不看 Shift；</li>
 *   <li>{@code useOn()} 只在右键讲台时尝试放书，其余返回 PASS。</li>
 * </ul>
 * 所以 Shift + 右键方块在原版里和普通右键等价，我们拦截它不会破坏任何功能。
 *
 * <p>拦截时机：{@link PlayerInteractEvent.RightClickBlock} 发生在 {@code Item.useOn}
 * 和 {@code Item.use} 之前，cancel 掉就能同时挡住"放书"和"打开 GUI"，
 * 客户端不会再弹手册。
 *
 * <p>注意：判断"是不是 IE 手册"用注册名而不是 IE 的 {@code IEItems.MANUAL} 类，
 * 避免依赖 IE 的 internal 包（升级更稳，也不触发额外的开源义务）。
 */
@Mod.EventBusSubscriber(modid = IEAddonTemplate.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ManualBuildHandler {
    private static final ResourceLocation MANUAL_ID =
            new ResourceLocation("immersiveengineering", "manual");

    private ManualBuildHandler() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!player.isShiftKeyDown() || !isEngineersManual(event.getItemStack())) {
            return;
        }

        BlockPos pos = event.getPos();
        // 右键讲台时保持原版行为（手册可以放上书架）
        if (event.getLevel().getBlockState(pos).getBlock() instanceof LecternBlock) {
            return;
        }

        Direction face = event.getFace();
        if (face == null) {
            return;
        }

        // 取消原版：客户端不会再打开手册 GUI，服务端执行搭建
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (!event.getLevel().isClientSide) {
            MultiblockBuildHelper.tryBuild(event.getLevel(), player, pos, face, false);
        }
    }

    private static boolean isEngineersManual(ItemStack stack) {
        return !stack.isEmpty() && MANUAL_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }
}
