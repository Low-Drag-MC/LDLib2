package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ProgressBarRenderer {
    private ProgressBarRenderer() {
    }

    public static void drawBackgroundAdditional(ProgressBar progressBar, GUIContext context) {
        progressBar.applyInterpolatedProgress(context.partialTick);
    }
}
