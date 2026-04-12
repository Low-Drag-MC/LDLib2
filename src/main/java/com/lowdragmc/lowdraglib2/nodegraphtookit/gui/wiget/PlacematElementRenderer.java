package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.wiget;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.DelegatingUIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@LDLRegisterClient(name = "placemat_element", registry = "ldlib2:ui_element_renderer")
public final class PlacematElementRenderer extends DelegatingUIElementRenderer<PlacematElement, PlacematElementRenderer> {
    @Override
    public Class<PlacematElement> type() {
        return PlacematElement.class;
    }

    @Override
    public void drawBackgroundOverlay(PlacematElement element, IGUIContext context) {
        if (!(context instanceof GUIContext guiContext)) {
            drawParentBackgroundOverlay(element, context);
            return;
        }
        element.drawBackgroundOverlay(guiContext);
    }
}
