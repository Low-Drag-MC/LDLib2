package com.lowdragmc.lowdraglib2.gui.ui;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;

public interface UIElementRenderer<T extends UIElement> {
    default void drawContents(T element, IGUIContext context) {
        var scissor = element.getStyle().clip().isScissor();
        if (scissor) {
            if (element.isCulled()) {
                return;
            }
            context.enableScissor(element.getContentX(), element.getContentY(), element.getContentWidth(), element.getContentHeight());
        }
        if (!element.isCulled()) {
            drawBackgroundAdditional(element, context);
        }
        if (!element.getChildren().isEmpty()) {
            var currentColor = context.getElementColor();
            var hasColor = currentColor != -1;
            if (hasColor) {
                context.resetElementColor();
            }

            var sortedChildren = element.getSafeSortedChildren();
            for (int i = sortedChildren.length - 1; i >= 0; i--) {
                sortedChildren[i].drawInBackground(context);
            }

            if (hasColor) {
                context.setElementColor(currentColor);
            }
        }
        if (scissor) {
            context.disableScissor();
        }
    }

    default void drawBackgroundAdditional(T element, IGUIContext context) {
        element.drawBackgroundAdditional(context);
    }

    default void drawBackgroundOverlay(T element, IGUIContext context) {
        var overlay = element.getStyle().overlayTexture();
        if (overlay != null && overlay != IGuiTexture.EMPTY) {
            context.drawTexture(overlay, element.getPositionX(), element.getPositionY(), element.getSizeWidth(), element.getSizeHeight());
        }
    }
}
