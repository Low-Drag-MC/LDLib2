package com.lowdragmc.lowdraglib2.integration.rei;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.client.gui.screens.Screen;

@REIPluginClient
public class LDLibREIPlugin implements REIClientPlugin {

    public static Rectangle getRectangle(UIElement element) {
        return getRectangle(element, false);
    }

    public static Rectangle getRectangle(UIElement element, boolean content) {
        if (content) {
            return new Rectangle(element.getContentX(), element.getContentY(), element.getContentWidth(), element.getContentHeight());
        } else {
            return new Rectangle(element.getPositionX(), element.getPositionY(), element.getSizeWidth(), element.getSizeHeight());
        }
    }

    @Override
    public void registerExclusionZones(ExclusionZones zones) {
        zones.register(Screen.class, ModularUIREIHandlers.EXCLUSION_ZONES_PROVIDER);
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        registry.registerFocusedStack(ModularUIREIHandlers.FOCUSED_STACK_PROVIDER);
        registry.registerDraggableStackVisitor(ModularUIREIHandlers.DRAGGABLE_STACK_VISITOR);
    }
}
