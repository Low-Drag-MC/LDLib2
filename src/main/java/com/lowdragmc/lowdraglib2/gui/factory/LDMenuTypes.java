package com.lowdragmc.lowdraglib2.gui.factory;

import com.lowdragmc.lowdraglib2.CommonProxy;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

import java.util.function.Supplier;

public final class LDMenuTypes {
    // For some DeferredRegister<MenuType<?>> REGISTER
    public static final Supplier<MenuType<ModularUIContainerMenu>> PLAYER_UI = CommonProxy.MENUS.register("player_ui",
            () -> IMenuTypeExtension.create(PlayerUIMenuType::create));

}
