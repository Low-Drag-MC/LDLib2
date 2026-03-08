package com.lowdragmc.lowdraglib2.gui.ui.debugger;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.DelegatingUIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@LDLRegisterClient(name = "ui_debugger", registry = "ldlib2:ui_element_renderer")
public final class UIDebuggerRenderer extends DelegatingUIElementRenderer<UIDebugger, UIDebuggerRenderer> {
    @Override
    public Class<UIDebugger> type() {
        return UIDebugger.class;
    }

    @Override
    public void drawBackgroundAdditional(UIDebugger element, IGUIContext context) {
        if (!(context instanceof GUIContext guiContext)) {
            drawParentBackgroundAdditional(element, context);
            return;
        }
        element.drawBackgroundAdditional(guiContext);
    }
}
