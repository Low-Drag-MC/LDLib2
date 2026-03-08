package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.DelegatingUIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@LDLRegisterClient(name = "split_view", registry = "ldlib2:ui_element_renderer")
public final class SplitViewRenderer extends DelegatingUIElementRenderer<SplitView, SplitViewRenderer> {
    @Override
    public Class<SplitView> type() {
        return SplitView.class;
    }

    @Override
    public void drawBackgroundAdditional(SplitView splitView, IGUIContext context) {
        if (!(context instanceof GUIContext guiContext)) {
            drawParentBackgroundAdditional(splitView, context);
            return;
        }
        drawBackgroundAdditional(splitView, guiContext);
    }

    static void drawBackgroundAdditional(SplitView splitView, GUIContext context) {
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
