package com.ieaddon.template.registry;

import com.ieaddon.template.IEAddonTemplate;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, IEAddonTemplate.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + IEAddonTemplate.MODID))
                    .icon(() -> new ItemStack(ModItems.MY_TOOL.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.MY_TOOL.get());
                        output.accept(ModItems.MY_UPGRADE.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
