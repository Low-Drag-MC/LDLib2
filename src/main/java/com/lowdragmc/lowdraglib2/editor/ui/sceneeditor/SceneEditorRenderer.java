package com.lowdragmc.lowdraglib2.editor.ui.sceneeditor;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.DelegatingUIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@LDLRegisterClient(name = "scene_editor", registry = "ldlib2:ui_element_renderer")
public final class SceneEditorRenderer extends DelegatingUIElementRenderer<SceneEditor, SceneEditorRenderer> {
    @Override
    public Class<SceneEditor> type() {
        return SceneEditor.class;
    }

    @Override
    public void drawBackgroundAdditional(SceneEditor element, IGUIContext context) {
        if (!(context instanceof GUIContext guiContext)) {
            drawParentBackgroundAdditional(element, context);
            return;
        }
        element.drawBackgroundAdditional(guiContext);
    }
}
