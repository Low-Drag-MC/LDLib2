package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Manages docking of {@link GraphPanel} instances into {@link DockSlot}s.
 *
 * <p>Each corner slot holds at most one {@link GraphPanel}; multiple {@link IGraphTool}s
 * landing in the same corner are merged into a single panel using its internal {@code TabView}.
 * The {@link DockSlot#CENTER} "slot" is a free-floating area: any number of panels can live there.
 */
public class DockManager {
    /** Pixel size of the corner hit zones used to detect docking. */
    public static final float CORNER_HIT_SIZE = 30f;
    /** Highlight overlay color (semi-transparent blue). */
    private static final int HIGHLIGHT_COLOR = 0x55_4488FF;

    public final GraphView graphView;
    public final EnumMap<DockSlot, GraphPanel> cornerPanels = new EnumMap<>(DockSlot.class);
    public final List<GraphPanel> floatingPanels = new ArrayList<>();
    public final UIElement highlightOverlay = new UIElement();

    public DockManager(GraphView graphView) {
        this.graphView = graphView;
        highlightOverlay.addClass("__dock-manager_highlight__");
        Style.defaultPipeline(highlightOverlay.getLayout(), l -> l.positionType(TaffyPosition.ABSOLUTE));
        Style.defaultPipeline(highlightOverlay.getStyle(), s -> s.background(new ColorRectTexture(HIGHLIGHT_COLOR)));
        highlightOverlay.setAllowHitTest(false);
        // Highlight is hidden by default — visibility is state-driven during dock drag.
        Style.importantPipeline(highlightOverlay.getLayout(), l -> l.display(TaffyDisplay.NONE));
        graphView.getPanelLayer().addChild(highlightOverlay);
    }

    /**
     * Attach a freshly-created panel to its initial slot. If a panel already occupies the slot,
     * the new panel's tools are merged into the existing one and {@code panel} is removed.
     */
    public void register(GraphPanel panel, DockSlot initialSlot) {
        if (initialSlot == DockSlot.CENTER) {
            floatingPanels.add(panel);
            panel.setSlot(DockSlot.CENTER);
            return;
        }
        var existing = cornerPanels.get(initialSlot);
        if (existing != null && existing != panel) {
            mergeInto(panel, existing);
            return;
        }
        panel.setSlot(initialSlot);
        cornerPanels.put(initialSlot, panel);
    }

    /**
     * Move {@code source} to {@code target}. If {@code target} is a corner already occupied
     * by another panel, source's tools are merged into the existing panel.
     */
    public void dock(GraphPanel source, DockSlot target) {
        var oldSlot = source.getSlot();
        if (oldSlot == target && cornerPanels.get(target) == source) {
            // already docked here — re-snap layout
            source.setSlot(target);
            return;
        }

        // Detach from previous tracking.
        if (oldSlot != DockSlot.CENTER) {
            if (cornerPanels.get(oldSlot) == source) cornerPanels.remove(oldSlot);
        } else {
            floatingPanels.remove(source);
        }

        if (target == DockSlot.CENTER) {
            floatingPanels.add(source);
            source.setSlot(DockSlot.CENTER);
            return;
        }

        var existing = cornerPanels.get(target);
        if (existing != null && existing != source) {
            mergeInto(source, existing);
        } else {
            source.setSlot(target);
            cornerPanels.put(target, source);
        }
    }

    /**
     * Determine which dock slot the mouse is currently over. Returns one of the four corners
     * if the mouse is within {@link #CORNER_HIT_SIZE} of that canvas corner; otherwise {@link DockSlot#CENTER}.
     */
    public DockSlot detectSlot(float worldX, float worldY) {
        var canvas = graphView.canvas;
        float cx = canvas.getPositionX();
        float cy = canvas.getPositionY();
        float cw = canvas.getSizeWidth();
        float ch = canvas.getSizeHeight();

        boolean left   = worldX >= cx           && worldX <= cx + CORNER_HIT_SIZE;
        boolean right  = worldX >= cx + cw - CORNER_HIT_SIZE && worldX <= cx + cw;
        boolean top    = worldY >= cy           && worldY <= cy + CORNER_HIT_SIZE;
        boolean bottom = worldY >= cy + ch - CORNER_HIT_SIZE && worldY <= cy + ch;

        if (left  && top)    return DockSlot.TOP_LEFT;
        if (right && top)    return DockSlot.TOP_RIGHT;
        if (left  && bottom) return DockSlot.BOTTOM_LEFT;
        if (right && bottom) return DockSlot.BOTTOM_RIGHT;
        return DockSlot.CENTER;
    }

    /**
     * Show the dock-target highlight overlay for the given slot.
     * Corners highlight the panel landing area; CENTER highlights the inner canvas region
     * (canvas minus the 4 corner hit zones).
     */
    public void showDockHighlight(DockSlot slot, GraphPanel source) {
        var canvas = graphView.canvas;
        float cx = 0;
        float cy = 0;
        float cw = canvas.getSizeWidth();
        float ch = canvas.getSizeHeight();

        if (slot == DockSlot.CENTER) {
            float inset = CORNER_HIT_SIZE;
            float left = cx + inset;
            float top = cy + inset;
            float w = Math.max(0f, cw - inset * 2);
            float h = Math.max(0f, ch - inset * 2);
            // Position and visibility during dock-target highlight — state-driven.
            float fLeft = left, fTop = top, fW = w, fH = h;
            Style.importantPipeline(highlightOverlay.getLayout(),
                    l -> l.left(fLeft).top(fTop).width(fW).height(fH).display(TaffyDisplay.FLEX));
            return;
        }

        // Highlight area = where the source panel would land (use existing slot panel size if merging).
        var existing = cornerPanels.get(slot);
        float w, h;
        if (existing != null && existing != source) {
            w = existing.getSizeWidth();
            h = existing.getSizeHeight();
        } else {
            w = source.getSizeWidth();
            h = source.getSizeHeight();
        }
        if (w < 20f) w = GraphPanel.DEFAULT_PANEL_W;
        if (h < 20f) h = GraphPanel.DEFAULT_PANEL_H;

        float left, top;
        switch (slot) {
            case TOP_LEFT     -> { left = cx;             top = cy; }
            case TOP_RIGHT    -> { left = cx + cw - w;    top = cy; }
            case BOTTOM_LEFT  -> { left = cx;             top = cy + ch - h; }
            case BOTTOM_RIGHT -> { left = cx + cw - w;    top = cy + ch - h; }
            default -> { return; }
        }

        float fLeft = left, fTop = top, fW = w, fH = h;
        Style.importantPipeline(highlightOverlay.getLayout(),
                l -> l.left(fLeft).top(fTop).width(fW).height(fH).display(TaffyDisplay.FLEX));
    }

    public void hideDockHighlight() {
        Style.importantPipeline(highlightOverlay.getLayout(), l -> l.display(TaffyDisplay.NONE));
    }

    @Nullable
    public GraphPanel getCornerPanel(DockSlot slot) {
        if (slot == DockSlot.CENTER) return null;
        return cornerPanels.get(slot);
    }

    /**
     * Migrate all tools from {@code source} into {@code target} and remove the source panel.
     */
    private void mergeInto(GraphPanel source, GraphPanel target) {
        var copy = new ArrayList<>(source.getTools());
        for (var tool : copy) {
            source.removeTool(tool);
            target.addTool(tool);
        }
        graphView.getPanelLayer().removeChild(source);
    }
}
