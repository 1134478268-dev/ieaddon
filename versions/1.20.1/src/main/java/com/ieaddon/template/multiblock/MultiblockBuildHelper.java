package com.ieaddon.template.multiblock;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import blusunrize.immersiveengineering.api.multiblocks.TemplateMultiblock;
import blusunrize.immersiveengineering.api.utils.DirectionUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多方块搭建的公共逻辑。
 *
 * <p>被两个入口共用：
 * <ul>
 *   <li>工程师手册 Shift + 右键（{@code ManualBuildHandler}）；</li>
 *   <li>投影模式（玩家按投影轮廓放方块，放齐后自动成型）。</li>
 * </ul>
 *
 * <p>核心原则：只负责把方块摆成"成型前"的样子，成型永远交给
 * {@link IMultiblock#createStructure}。
 */
public final class MultiblockBuildHelper {
    private MultiblockBuildHelper() {
    }

    /**
     * 以点击的方块为核心，尝试搭建。
     *
     * @param preview true 时只显示计划，不扣材料、不放置
     * @return 是否成功；预览时表示"找到了可搭建的结构"
     */
    public static boolean tryBuild(Level level, Player player, BlockPos clicked, Direction face, boolean preview) {
        if (level.isClientSide) {
            return true;
        }

        List<BuildPlan> plans = findPlans(level, clicked, face);
        if (plans.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("multiblock.ieaddon.no_match").withStyle(ChatFormatting.RED),
                    true);
            return false;
        }

        // 玩家预放的方块匹配得越多，越可能是他想建的结构。
        // List.sort 是稳定排序，匹配数相同时保持 IE 的注册顺序（与手册一致）。
        plans.sort(Comparator.comparingInt((BuildPlan plan) -> -plan.matched));
        BuildPlan plan = plans.get(0);

        if (plans.size() > 1) {
            player.displayClientMessage(
                    Component.translatable("multiblock.ieaddon.multiple",
                            plans.size(), plan.multiblock().getDisplayName()), false);
        }

        if (preview) {
            player.displayClientMessage(
                    Component.translatable("multiblock.ieaddon.plan",
                            plan.multiblock().getDisplayName(),
                            plan.toPlace().size(),
                            describeCost(plan.cost())), false);
            return true;
        }

        Map<Item, Integer> missing = missing(player, plan.cost());
        if (!missing.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("multiblock.ieaddon.missing",
                            describeCost(missing)).withStyle(ChatFormatting.RED),
                    false);
            return false;
        }

        consume(player, plan.cost());
        execute(plan, level, player);
        return true;
    }

    /** 收集所有以点击方块为核心、且能放下的结构方案。 */
    public static List<BuildPlan> findPlans(Level level, BlockPos clicked, Direction face) {
        List<BuildPlan> plans = new ArrayList<>();
        BlockState clickedState = level.getBlockState(clicked);
        Rotation rot = DirectionUtils.getRotationBetweenFacings(Direction.NORTH, face.getOpposite());
        if (rot == null) {
            return plans;
        }

        for (IMultiblock multiblock : MultiblockHandler.getMultiblocks()) {
            if (!(multiblock instanceof TemplateMultiblock template)) {
                continue;
            }
            if (!multiblock.isBlockTrigger(clickedState, face, level)) {
                continue;
            }
            List<StructureBlockInfo> structure;
            try {
                structure = multiblock.getStructure(level);
            } catch (RuntimeException ex) {
                continue;
            }

            List<Mirror> mirrors = template.canBeMirrored()
                    ? List.of(Mirror.NONE, Mirror.FRONT_BACK)
                    : List.of(Mirror.NONE);

            for (Mirror mirror : mirrors) {
                StructurePlaceSettings settings = new StructurePlaceSettings().setMirror(mirror).setRotation(rot);
                BlockPos origin = clicked.subtract(
                        StructureTemplate.calculateRelativePosition(settings, multiblock.getTriggerOffset()));

                List<BlockPos> toPlace = new ArrayList<>();
                Map<Item, Integer> cost = new LinkedHashMap<>();
                boolean blocked = false;
                int matched = 0;

                for (StructureBlockInfo info : structure) {
                    BlockPos here = origin.offset(StructureTemplate.calculateRelativePosition(settings, info.pos()));
                    BlockState expected = info.state().mirror(mirror).rotate(rot);
                    BlockState current = level.getBlockState(here);

                    if (current.equals(expected)) {
                        matched++;
                        continue;
                    }
                    if (!current.canBeReplaced()) {
                        blocked = true;
                        break;
                    }
                    toPlace.add(here);
                    Item item = expected.getBlock().asItem();
                    if (item != Items.AIR) {
                        cost.merge(item, 1, Integer::sum);
                    }
                }

                if (!blocked) {
                    plans.add(new BuildPlan(multiblock, clicked, face, origin, rot, mirror,
                            structure, toPlace, cost, matched));
                    break;
                }
            }
        }
        return plans;
    }

    /** 逐格放置并把成型交给 IE。 */
    public static void execute(BuildPlan plan, Level level, Player player) {
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setMirror(plan.mirror())
                .setRotation(plan.rotation());

        for (StructureBlockInfo info : plan.structure()) {
            BlockPos here = plan.origin().offset(StructureTemplate.calculateRelativePosition(settings, info.pos()));
            BlockState expected = info.state().mirror(plan.mirror()).rotate(plan.rotation());
            if (level.getBlockState(here).equals(expected)) {
                continue;
            }
            level.setBlock(here, expected, Block.UPDATE_ALL);
            if (info.nbt() != null) {
                BlockEntity blockEntity = level.getBlockEntity(here);
                if (blockEntity != null) {
                    blockEntity.load(info.nbt());
                    blockEntity.setChanged();
                }
            }
        }

        plan.multiblock().createStructure(level, plan.clicked(), plan.face(), player);
    }

    /** 返回"还差多少"：只包含数量不够的物品。 */
    public static Map<Item, Integer> missing(Player player, Map<Item, Integer> cost) {
        Map<Item, Integer> missing = new LinkedHashMap<>();
        if (player.isCreative() || cost.isEmpty()) {
            return missing;
        }
        Map<Item, Integer> have = new HashMap<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                have.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        for (Map.Entry<Item, Integer> entry : cost.entrySet()) {
            int shortfall = entry.getValue() - have.getOrDefault(entry.getKey(), 0);
            if (shortfall > 0) {
                missing.put(entry.getKey(), shortfall);
            }
        }
        return missing;
    }

    public static void consume(Player player, Map<Item, Integer> cost) {
        if (player.isCreative()) {
            return;
        }
        for (Map.Entry<Item, Integer> entry : cost.entrySet()) {
            int remaining = entry.getValue();
            for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.getItem() == entry.getKey()) {
                    int take = Math.min(remaining, stack.getCount());
                    stack.shrink(take);
                    remaining -= take;
                }
            }
        }
    }

    public static Component describeCost(Map<Item, Integer> cost) {
        if (cost.isEmpty()) {
            return Component.translatable("multiblock.ieaddon.free");
        }
        MutableComponent result = Component.empty();
        boolean first = true;
        for (Map.Entry<Item, Integer> entry : cost.entrySet()) {
            if (!first) {
                result.append(Component.literal("、"));
            }
            first = false;
            result.append(Component.literal(entry.getValue() + "x "))
                    .append(entry.getKey().getDescription());
        }
        return result;
    }

    /** 一次搭建计划。matched = 周围已经就位的方块数，用于多候选消歧。 */
    public record BuildPlan(
            IMultiblock multiblock,
            BlockPos clicked,
            Direction face,
            BlockPos origin,
            Rotation rotation,
            Mirror mirror,
            List<StructureBlockInfo> structure,
            List<BlockPos> toPlace,
            Map<Item, Integer> cost,
            int matched
    ) {
    }
}
