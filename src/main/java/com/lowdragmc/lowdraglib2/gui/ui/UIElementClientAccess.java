package com.lowdragmc.lowdraglib2.gui.ui;

import net.minecraft.client.renderer.Rect2i;

import java.util.List;

public final class UIElementClientAccess {
    private UIElementClientAccess() {
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
