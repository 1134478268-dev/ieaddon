package com.ieaddon.template.registry;

import com.ieaddon.template.IEAddonTemplate;
import com.ieaddon.template.item.MyUpgradeableToolItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import net.minecraft.core.Direction;

/**
 * 把工具内部的 IItemHandler 挂到 ItemStack 上，
 * IE 的工程师工作台就是通过这个 capability 访问升级槽的。
 */
@Mod.EventBusSubscriber(modid = IEAddonTemplate.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModCapabilities {
    private ModCapabilities() {
    }

    @SubscribeEvent
    public static void attachItemCapabilities(AttachCapabilitiesEvent<ItemStack> event) {
        ItemStack stack = event.getObject();
        if (!(stack.getItem() instanceof MyUpgradeableToolItem)) {
            return;
        }
        event.addCapability(new ResourceLocation(IEAddonTemplate.MODID, "tool_inventory"),
                new ICapabilityProvider() {
                    private final IItemHandler inventory = MyUpgradeableToolItem.getInventory(stack);

                    @Override
                    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
                        if (capability == ForgeCapabilities.ITEM_HANDLER) {
                            return LazyOptional.of(() -> inventory).cast();
                        }
                        return LazyOptional.empty();
                    }
                });
    }
}
