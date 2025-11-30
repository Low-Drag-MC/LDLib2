package com.lowdragmc.lowdraglib2.integration.rei;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.client.gui.screens.Screen;

@REIPluginClient
public class LDLibREIPlugin implements REIClientPlugin {
    @Override
    public void registerExclusionZones(ExclusionZones zones) {
        zones.register(Screen.class, ModularUIREIHandlers.EXCLUSION_ZONES_PROVIDER);
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        registry.registerFocusedStack(ModularUIREIHandlers.FOCUSED_STACK_PROVIDER);
    }
}
