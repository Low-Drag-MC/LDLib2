package com.lowdragmc.lowdraglib2.gui.editor.view;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.DelegatingUIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@LDLRegisterClient(name = "modular_ui_preview", registry = "ldlib2:ui_element_renderer")
public final class ModularUIPreviewRenderer extends DelegatingUIElementRenderer<ModularUIPreview, ModularUIPreviewRenderer> {
    @Override
    public Class<ModularUIPreview> type() {
        return ModularUIPreview.class;
    }

    @Override
    public void drawBackgroundAdditional(ModularUIPreview element, IGUIContext context) {
        if (!(context instanceof GUIContext guiContext)) {
            drawParentBackgroundAdditional(element, context);
            return;
        }
        element.drawBackgroundAdditional(guiContext);
    }
}
