package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.DelegatingUIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@LDLRegisterClient(name = "progress_bar", registry = "ldlib2:ui_element_renderer")
public final class ProgressBarRenderer extends DelegatingUIElementRenderer<ProgressBar, ProgressBarRenderer> {
    @Override
    public Class<ProgressBar> type() {
        return ProgressBar.class;
    }

    @Override
    public void drawBackgroundAdditional(ProgressBar progressBar, IGUIContext context) {
        if (!(context instanceof GUIContext guiContext)) {
            drawParentBackgroundAdditional(progressBar, context);
            return;
        }
        drawBackgroundAdditional(progressBar, guiContext);
    }

    static void drawBackgroundAdditional(ProgressBar progressBar, GUIContext context) {
        progressBar.applyInterpolatedProgress(context.partialTick);
    }
}
