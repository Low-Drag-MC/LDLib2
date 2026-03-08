package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.DelegatingUIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@LDLRegisterClient(name = "graph_panel", registry = "ldlib2:ui_element_renderer")
public final class GraphPanelRenderer extends DelegatingUIElementRenderer<GraphPanel, GraphPanelRenderer> {
    @Override
    public Class<GraphPanel> type() {
        return GraphPanel.class;
    }

    @Override
    public void drawBackgroundAdditional(GraphPanel element, IGUIContext context) {
        if (!(context instanceof GUIContext guiContext)) {
            drawParentBackgroundAdditional(element, context);
            return;
        }
        element.drawBackgroundAdditional(guiContext);
    }
}
