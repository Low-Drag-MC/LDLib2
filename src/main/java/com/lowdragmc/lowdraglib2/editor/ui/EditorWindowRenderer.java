package com.lowdragmc.lowdraglib2.editor.ui;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.DelegatingUIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@LDLRegisterClient(name = "editor_window", registry = "ldlib2:ui_element_renderer")
public final class EditorWindowRenderer extends DelegatingUIElementRenderer<EditorWindow, EditorWindowRenderer> {
    @Override
    public Class<EditorWindow> type() {
        return EditorWindow.class;
    }

    @Override
    public void drawBackgroundAdditional(EditorWindow element, IGUIContext context) {
        if (!(context instanceof GUIContext guiContext)) {
            drawParentBackgroundAdditional(element, context);
            return;
        }
        element.drawBackgroundAdditional(guiContext);
    }
}
