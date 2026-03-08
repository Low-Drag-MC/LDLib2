package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.DelegatingUIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@LDLRegisterClient(name = "text_area_content_view", registry = "ldlib2:ui_element_renderer")
public final class TextAreaContentViewRenderer extends DelegatingUIElementRenderer<TextArea.ContentView, TextAreaContentViewRenderer> {
    @Override
    public Class<TextArea.ContentView> type() {
        return TextArea.ContentView.class;
    }

    @Override
    public void drawBackgroundAdditional(TextArea.ContentView element, IGUIContext context) {
        if (!(context instanceof GUIContext guiContext)) {
            drawParentBackgroundAdditional(element, context);
            return;
        }
        TextAreaRenderer.drawContentViewElement(element, guiContext);
    }
}
