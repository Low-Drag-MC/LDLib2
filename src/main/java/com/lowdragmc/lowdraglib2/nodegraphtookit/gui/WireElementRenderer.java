package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.DelegatingUIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@LDLRegisterClient(name = "wire_element", registry = "ldlib2:ui_element_renderer")
public final class WireElementRenderer extends DelegatingUIElementRenderer<WireElement, WireElementRenderer> {
    @Override
    public Class<WireElement> type() {
        return WireElement.class;
    }

    @Override
    public void drawBackgroundAdditional(WireElement element, IGUIContext context) {
        if (!(context instanceof GUIContext guiContext)) {
            drawParentBackgroundAdditional(element, context);
            return;
        }
        element.drawBackgroundAdditional(guiContext);
    }
}
