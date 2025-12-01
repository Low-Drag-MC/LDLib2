package com.lowdragmc.lowdraglib2.integration.emi;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.widget.Bounds;

@EmiEntrypoint
public class LDLibEMIPlugin implements EmiPlugin {

    public static Bounds getBounds(UIElement element) {
        return getBounds(element, false);
    }

    public static Bounds getBounds(UIElement element, boolean content) {
        if (content) {
            return new Bounds((int) element.getContentX(), (int) element.getContentY(), (int) element.getContentWidth(), (int) element.getContentHeight());
        } else {
            return new Bounds((int) element.getPositionX(), (int) element.getPositionY(), (int) element.getSizeWidth(), (int) element.getSizeHeight());
        }
    }

    @Override
    public void register(EmiRegistry registry) {
        registry.addGenericExclusionArea(ModularUIEMIHandlers.EXCLUSION_AREA);
        registry.addGenericStackProvider(ModularUIEMIHandlers.STACK_PROVIDER);
        registry.addGenericDragDropHandler(ModularUIEMIHandlers.DRAG_DROP_HANDLER);
    }
}
