package com.lowdragmc.lowdraglib2.gui.ui;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.UIVisualLayer;
import net.minecraft.client.renderer.Rect2i;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class UIElementClientAccess {
    private UIElementClientAccess() {
    }

    static UIElementClientState getState(UIElement element) {
        if (!(element.clientState instanceof UIElementClientState)) {
            var state = new UIElementClientState();
            element.clientState = state;
            return state;
        }
        return (UIElementClientState) element.clientState;
    }

    private static UIVisualLayer getOrCreateVisualLayer(UIElement element) {
        var state = getState(element);
        if (state.visualLayer == null) {
            state.visualLayer = new UIVisualLayer(element);
        }
        return state.visualLayer;
    }

    public static void pushVisualLayer(UIElement element, GUIContext context) {
        context.pushVisualLayer(getOrCreateVisualLayer(element));
    }

    public static void release(UIElement element) {
        if (element.clientState instanceof UIElementClientState state && state.visualLayer != null) {
            state.visualLayer.release();
            state.visualLayer = null;
        }
    }

    public static void appendExtraAreas(UIElement element, List<Rect2i> extraAreas) {
        if (!element.isDisplayed() || !element.isVisible()) {
            return;
        }
        var rect = new Rect2i(Math.round(element.getPositionX()), Math.round(element.getPositionY()),
                Math.round(element.getSizeWidth()), Math.round(element.getSizeHeight()));
        var contains = false;
        for (var extraArea : extraAreas) {
            if (extraArea.getX() <= rect.getX() &&
                    extraArea.getY() <= rect.getY() &&
                    extraArea.getX() + extraArea.getWidth() >= rect.getX() + rect.getWidth() &&
                    extraArea.getY() + extraArea.getHeight() >= rect.getY() + rect.getHeight()) {
                contains = true;
                break;
            }
        }
        if (!contains) {
            extraAreas.add(rect);
        }
        for (UIElement child : element.getChildren()) {
            appendExtraAreas(child, extraAreas);
        }
    }
}
