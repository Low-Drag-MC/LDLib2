package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class SelectorRenderer {
    private SelectorRenderer() {
    }

    public static void drawBackgroundOverlay(Selector<?> selector, GUIContext context) {
        if (selector.isSelfOrChildHover() || selector.isFocused()) {
            context.drawTexture(selector.getSelectorStyle().focusOverlay(),
                    selector.getPositionX(), selector.getPositionY(),
                    selector.getSizeWidth(), selector.getSizeHeight());
        }
        selector.drawSharedDefaultOverlay(context);
    }
}
