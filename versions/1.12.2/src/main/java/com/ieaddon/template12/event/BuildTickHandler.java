package com.ieaddon.template12.event;

import com.ieaddon.template12.IEAddonTemplate;
import com.ieaddon.template12.multiblock.BuildTasks;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** 服务端每 tick 推进逐块搭建任务。 */
@Mod.EventBusSubscriber(modid = IEAddonTemplate.MODID)
public final class BuildTickHandler {
    private BuildTickHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            BuildTasks.tick();
        }
    }
}
