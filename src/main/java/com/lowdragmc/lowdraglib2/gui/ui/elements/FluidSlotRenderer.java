package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;

@OnlyIn(Dist.CLIENT)
public final class FluidSlotRenderer {
    private FluidSlotRenderer() {
    }

    public static void drawBackgroundAdditional(FluidSlot fluidSlot, GUIContext context) {
        var renderedFluid = fluidSlot.getValue();
        var hovered = fluidSlot.isHover() || fluidSlot.isSelfOrChildHover();
        var drawSlotOverlay = fluidSlot.getSlotStyle().showSlotOverlayOnlyEmpty() || !renderedFluid.isEmpty();

        if (renderedFluid.isEmpty() && !hovered && !drawSlotOverlay) return;

        var contentX = fluidSlot.getContentX();
        var contentY = fluidSlot.getContentY();
        var contentWidth = fluidSlot.getContentWidth();
        var contentHeight = fluidSlot.getContentHeight();

        if (renderedFluid.isEmpty() || !fluidSlot.getSlotStyle().showSlotOverlayOnlyEmpty()) {
            drawSlotOverlay(fluidSlot, context, contentX, contentY, contentWidth, contentHeight);
        }
        if (!renderedFluid.isEmpty()) {
            drawFluid(fluidSlot, context, renderedFluid, contentX, contentY, contentWidth, contentHeight);
        }
        if (hovered) {
            drawHover(fluidSlot, context, contentX, contentY, contentWidth, contentHeight);
        }
    }

    private static void drawSlotOverlay(FluidSlot fluidSlot, GUIContext context, float contentX, float contentY, float contentWidth, float contentHeight) {
        context.drawTexture(fluidSlot.getSlotStyle().slotOverlay(), contentX, contentY, contentWidth, contentHeight);
    }

    private static void drawFluid(FluidSlot fluidSlot, GUIContext context, FluidStack renderedFluid, float contentX, float contentY, float contentWidth, float contentHeight) {
        var fillDirection = fluidSlot.getSlotStyle().fillDirection();
        double progress = renderedFluid.getAmount() * 1.0 / Math.max(Math.max(renderedFluid.getAmount(), fluidSlot.getCapacity()), 1);
        float drawnU = (float) fillDirection.getDrawnU(progress);
        float drawnV = (float) fillDirection.getDrawnV(progress);
        float drawnWidth = (float) fillDirection.getDrawnWidth(progress);
        float drawnHeight = (float) fillDirection.getDrawnHeight(progress);
        DrawerHelper.drawFluidForGui(context, renderedFluid,
                contentX + drawnU * contentWidth,
                contentY + drawnV * contentHeight,
                contentWidth * drawnWidth,
                contentHeight * drawnHeight, -1);
    }

    private static void drawHover(FluidSlot fluidSlot, GUIContext context, float contentX, float contentY, float contentWidth, float contentHeight) {
        context.drawTexture(fluidSlot.getSlotStyle().hoverOverlay(), contentX, contentY, contentWidth, contentHeight);
    }
}
