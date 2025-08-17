package com.lowdragmc.lowdraglib2.gui.factory;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIContainerMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class LDMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU, LDLib2.MOD_ID);

    // For some DeferredRegister<MenuType<?>> REGISTER
    public static final Supplier<MenuType<ModularUIContainerMenu>> PLAYER_UI = MENUS.register("player_ui",
            () -> IMenuTypeExtension.create(PlayerUIMenuType::create));

    public static void init(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
