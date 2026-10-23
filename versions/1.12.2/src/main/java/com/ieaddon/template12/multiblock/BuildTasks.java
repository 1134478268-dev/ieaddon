package com.ieaddon.template12.multiblock;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 逐块搭建任务管理：每个玩家一个任务，每 tick 放 2 个方块。
 */
public final class BuildTasks {
    private static final Map<UUID, BuildTask> TASKS = new HashMap<>();
    private static final int BLOCKS_PER_TICK = 2;

    private BuildTasks() {
    }

    public static void add(BuildTask task) {
        TASKS.put(task.playerId, task);
    }

    public static void cancel(UUID playerId) {
        TASKS.remove(playerId);
    }

    public static void clear() {
        TASKS.clear();
    }

    public static void tick() {
        if (TASKS.isEmpty()) {
            return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) {
            return;
        }
        Iterator<Map.Entry<UUID, BuildTask>> it = TASKS.entrySet().iterator();
        while (it.hasNext()) {
            BuildTask task = it.next().getValue();
            for (int i = 0; i < BLOCKS_PER_TICK && !task.isEmpty(); i++) {
                task.step();
            }
            if (!task.isEmpty()) {
                continue;
            }
            EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(task.playerId);
            if (player != null) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "搭建完成：放置 " + task.placed + " 个方块"
                                + (task.skipped > 0 ? "，跳过 " + task.skipped + " 个受阻位置" : "")
                                + "，请用工程师锤敲击成型"));
            }
            it.remove();
        }
    }
}
