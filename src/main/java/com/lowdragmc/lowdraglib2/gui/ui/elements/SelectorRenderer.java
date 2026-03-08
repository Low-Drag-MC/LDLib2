package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.DelegatingUIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@LDLRegisterClient(name = "selector", registry = "ldlib2:ui_element_renderer")
public final class SelectorRenderer extends DelegatingUIElementRenderer<Selector<?>, SelectorRenderer> {
    @Override
    public Class<Selector<?>> type() {
        return (Class<Selector<?>>) (Class<?>) Selector.class;
    }

    @Override
    public void drawBackgroundOverlay(Selector<?> selector, IGUIContext context) {
        if (!(context instanceof GUIContext guiContext)) {
            drawParentBackgroundOverlay(selector, context);
            return;
        }
        if (selector.isSelfOrChildHover() || selector.isFocused()) {
            guiContext.drawTexture(selector.getSelectorStyle().focusOverlay(),
                    selector.getPositionX(), selector.getPositionY(),
                    selector.getSizeWidth(), selector.getSizeHeight());
        }
        drawParentBackgroundOverlay(selector, context);
    }
}
