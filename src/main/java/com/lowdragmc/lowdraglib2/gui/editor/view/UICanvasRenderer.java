package com.lowdragmc.lowdraglib2.gui.editor.view;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.DelegatingUIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@LDLRegisterClient(name = "ui_canvas", registry = "ldlib2:ui_element_renderer")
public final class UICanvasRenderer extends DelegatingUIElementRenderer<UICanvas, UICanvasRenderer> {
    @Override
    public Class<UICanvas> type() {
        return UICanvas.class;
    }

    @Override
    public void drawBackgroundAdditional(UICanvas element, IGUIContext context) {
        if (!(context instanceof GUIContext guiContext)) {
            drawParentBackgroundAdditional(element, context);
            return;
        }
        element.drawBackgroundAdditional(guiContext);
    }
}
