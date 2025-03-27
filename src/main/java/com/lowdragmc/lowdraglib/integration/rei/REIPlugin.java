package com.lowdragmc.lowdraglib.integration.rei;

import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import com.lowdragmc.lowdraglib.test.TestREIPlugin;
import me.shedaniel.rei.api.client.REIRuntime;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.forge.REIPluginClient;

/**
 * @author KilaBash
 * @date 2022/11/27
 * @implNote REIPlugin
 */
@REIPluginClient
public class REIPlugin implements REIClientPlugin {

    public static boolean isReiEnabled() {
        return REIRuntime.getInstance().isOverlayVisible();
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        registry.registerFocusedStack(ModularUIReiHandlers.FOCUSED_STACK_PROVIDER);
        registry.registerDraggableStackVisitor(ModularUIReiHandlers.DRAGGABLE_STACK_VISITOR);
    }

    @Override
    public void registerExclusionZones(ExclusionZones zones) {
        zones.register(ModularUIGuiContainer.class, ModularUIReiHandlers.EXCLUSION_ZONES_PROVIDER);
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        if (Platform.isDevEnv()) {
            TestREIPlugin.registerCategories(registry);
        }
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        if (Platform.isDevEnv()) {
            TestREIPlugin.registerDisplays(registry);
        }
    }
}
