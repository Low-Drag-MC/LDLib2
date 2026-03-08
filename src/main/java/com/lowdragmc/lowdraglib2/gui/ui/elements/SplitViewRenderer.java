package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class SplitViewRenderer {
    private SplitViewRenderer() {
    }

    public static void drawBackgroundAdditional(SplitView splitView, GUIContext context) {
        if (splitView.isHoverDragging(context.mouseX, context.mouseY)) {
            context.postRendering(ctx -> {
                var icon = splitView.getDraggingIcon();
                var width = icon.spriteSize.width;
                var height = icon.spriteSize.height;
                ctx.drawTexture(icon,
                        ctx.localMouseX - width / 2f,
                        ctx.localMouseY - height / 2f,
                        width,
                        height);
            });
        }
    }
}
