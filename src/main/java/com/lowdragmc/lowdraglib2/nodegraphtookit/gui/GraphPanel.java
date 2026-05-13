package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Tab;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TabView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.layout.LayoutProperties;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.util.WindowDragHelper;
import com.lowdragmc.lowdraglib2.gui.util.WindowDragHelper.ResizeHandle;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class GraphPanel extends UIElement {
    public static final float DEFAULT_PANEL_W = 150f;
    public static final float DEFAULT_PANEL_H = 150f;

    public final GraphView graphView;
    public final UIElement titleBar = new UIElement();
    public final Toggle collapseToggle = new Toggle();
    public final Label title = new Label();
    public final UIElement content = new UIElement();
    public final TabView tabView = new TabView();

    @Getter
    private final List<IGraphTool> tools = new ArrayList<>();
    private final BiMap<IGraphTool, Tab> toolTabs = HashBiMap.create();
    @Getter
    private DockSlot slot = DockSlot.CENTER;

    /** Per-slot remembered size; reapplied when re-docking into the same slot. */
    private final EnumMap<DockSlot, Vector2f> lastSlotSize = new EnumMap<>(DockSlot.class);
    /** Mutable set used by WindowDragHelper to gate which resize handles are active for the current slot. */
    private final Set<ResizeHandle> activeHandles = EnumSet.allOf(ResizeHandle.class);

    // runtime
    @Getter
    private boolean isResizing;
    @Getter
    private boolean isCollapsed = false;
    /** True while the user is dragging this panel via the title bar — suppresses applySlotLayout's corner-snap. */
    private boolean isPanelDragging;

    public GraphPanel(GraphView graphView, IGraphTool initial) {
        this.graphView = graphView;

        getLayout().positionType(TaffyPosition.ABSOLUTE)
                .width(DEFAULT_PANEL_W).height(DEFAULT_PANEL_H).paddingAll(2);
        getStyle().background(new ColorRectTexture(0xAA000000));

        collapseToggle.getLayout().height(9);
        collapseToggle.noText().setOnToggleChanged(this::setCollapsed);
        collapseToggle.toggleStyle(toggleStyle -> toggleStyle
                .baseTexture(IGuiTexture.EMPTY)
                .hoverTexture(IGuiTexture.EMPTY)
                .markTexture(Icons.RIGHT_ARROW_NO_BAR_S_WHITE)
                .unmarkTexture(Icons.DOWN_ARROW_NO_BAR_S_WHITE));

        title.getLayout().flexGrow(1);
        title.setOverflowVisible(false);

        titleBar.getLayout().flexDirection(FlexDirection.ROW);
        titleBar.addChildren(collapseToggle, title);

        tabView.layout(layout -> layout.widthPercent(100).heightPercent(100));
        tabView.setOnTabSelected(t -> updateTitleFromActiveTab());
        tabView.tabContentContainer.getStyle().background(IGuiTexture.EMPTY);
        tabView.tabContentContainer.getLayout().flex(1);

        content.getLayout().flex(1);
        content.addChild(tabView);

        addChildren(titleBar, content);

        // Custom drag-to-dock on title bar (replaces WindowDragHelper.setDragMove).
        titleBar.addEventListener(UIEvents.MOUSE_DOWN, this::onTitleMouseDown);
        titleBar.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onTitleDragUpdate);
        titleBar.addEventListener(UIEvents.DRAG_END, this::onTitleDragEnd);

        WindowDragHelper.setBorderResize(this, this, 2, new Vector2f(20f), new Vector2f(Float.MAX_VALUE),
                activeHandles,
                e -> canResize(),
                (e, handle) -> {
                    isResizing = canResize();
                    return isResizing;
                },
                e -> {
                    isResizing = false;
                    rememberCurrentSize();
                });

        graphView.addEventListener(UIEvents.LAYOUT_CHANGED, e -> applySlotLayout());

        addTool(initial);
        applySlotLayout();

        setFocusable(true);
        internalSetup();
    }

    // region tools

    public GraphPanel addTool(IGraphTool tool) {
        if (toolTabs.containsKey(tool)) return this;
        tools.add(tool);
        var tab = new Tab();
        tab.getTabStyle()
                .baseTexture(IGuiTexture.EMPTY)
                .hoverTexture(new ColorRectTexture(0x22ffffff))
                .selectedTexture(new ColorRectTexture(0x44ffffff));
        tab.text.setText(tool.getTitle());
        tabView.addTab(tab, tool.getUIElement());
        toolTabs.put(tool, tab);

        // Tab drag-out: drag a tab to detach it into its own panel docked elsewhere.
        // We don't stopPropagation, so TabView's selectTab MOUSE_DOWN still runs.
        tab.addEventListener(UIEvents.MOUSE_DOWN, e -> onTabMouseDown(e, tool));
        tab.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onTabDragUpdate);
        tab.addEventListener(UIEvents.DRAG_END, this::onTabDragEnd);

        updateTabHeaderVisibility();
        updateTitleFromActiveTab();
        return this;
    }

    public GraphPanel removeTool(IGraphTool tool) {
        var tab = toolTabs.remove(tool);
        if (tab == null) return this;
        tools.remove(tool);
        tabView.removeTab(tab);
        // detach the tool's element from the tabView so it can be re-parented
        var ui = (UIElement) tool.getUIElement();
        if (ui.getParent() != null) ui.removeSelf();
        updateTabHeaderVisibility();
        updateTitleFromActiveTab();
        return this;
    }

    public boolean hasNoTools() {
        return tools.isEmpty();
    }

    private void updateTabHeaderVisibility() {
        // Hide the tab header strip when only a single tool is present — the GraphPanel's own title bar suffices.
        tabView.tabHeaderContainer.setDisplay(tools.size() > 1);
    }

    private void updateTitleFromActiveTab() {
        if (tools.isEmpty()) {
            title.setText("");
            return;
        }
        var selected = tabView.getSelectedTab();
        IGraphTool active = null;
        if (selected != null) {
            active = toolTabs.inverse().get(selected);
        }
        if (active == null) active = tools.getFirst();
        title.setText(active.getTitle());
    }

    // endregion

    // region slot layout

    public void setSlot(DockSlot slot) {
        if (this.slot == slot) {
            applySlotLayout();
            return;
        }
        rememberCurrentSize();
        this.slot = slot;
        activeHandles.clear();
        activeHandles.addAll(slot.allowedResizeHandles());
        // Restore previously-remembered size for this slot (one-shot on slot transition only).
        if (!isCollapsed) {
            var size = lastSlotSize.get(slot);
            if (size != null) {
                getStyleBag().removeCandidates(LayoutProperties.WIDTH, s -> s.origin() == StyleOrigin.IMPORTANT);
                getStyleBag().removeCandidates(LayoutProperties.HEIGHT, s -> s.origin() == StyleOrigin.IMPORTANT);
                getLayout().width(Math.max(20f, size.x)).height(Math.max(20f, size.y));
            }
        }
        applySlotLayout();
    }

    private void rememberCurrentSize() {
        // Skip if not yet laid out — getSizeWidth/Height return 0 before the first layout pass,
        // and we don't want to cache that as the "remembered" size for this slot.
        float w = getSizeWidth();
        float h = getSizeHeight();
        if (w < 20f || h < 20f) return;
        lastSlotSize.put(slot, new Vector2f(w, h));
    }

    /**
     * Pin position based on current slot. Size is preserved (whatever the user has resized to),
     * so resize during corner-dock isn't reset.
     */
    public void applySlotLayout() {
        // While the user is dragging the panel via the title bar, the drag handler owns left/top.
        // Skip corner-snap so the panel actually follows the mouse instead of being yanked back.
        if (isPanelDragging) return;
        var canvas = graphView.canvas;
        float cx = 0;
        float cy = 0;
        float cw = canvas.getSizeWidth();
        float ch = canvas.getSizeHeight();

        if (slot == DockSlot.CENTER) {
            // Free floating: keep current position, only clamp.
            adaptPositionToElement(canvas);
            return;
        }

        // Position only — use the panel's CURRENT size so user resize sticks across layout updates.
        float w = getSizeWidth();
        float h = getSizeHeight();

        float left, top;
        switch (slot) {
            case TOP_LEFT     -> { left = cx;             top = cy; }
            case TOP_RIGHT    -> { left = cx + cw - w;    top = cy; }
            case BOTTOM_LEFT  -> { left = cx;             top = cy + ch - h; }
            case BOTTOM_RIGHT -> { left = cx + cw - w;    top = cy + ch - h; }
            default -> { return; }
        }
        getLayout().left(left).top(top);
    }

    // endregion

    // region drag-to-dock

    public record DragPanel(GraphPanel source, float startLeft, float startTop) {}
    public record DragTab(GraphPanel source, IGraphTool tool) {}

    /** True if dropping onto {@code target} would land on this exact panel (so no dock/highlight needed). */
    private boolean isDropOnSelf(DockSlot target) {
        if (target == DockSlot.CENTER) return slot == DockSlot.CENTER;
        return graphView.dockManager.getCornerPanel(target) == this;
    }

    private void onTabMouseDown(UIEvent e, IGraphTool tool) {
        if (e.button != 0) return;
        // Splitting requires at least 2 tools; with 1 the panel-level drag covers it.
        if (tools.size() <= 1) return;
        var tab = toolTabs.get(tool);
        if (tab == null) return;
        var icon = Icons.MOVE;
        var width = 12;
        var height = 12;
        tab.startDrag(new DragTab(this, tool), icon)
                .setDragTexture(-width / 2f, -height / 2f, width, height);
        // Don't stopPropagation — TabView still selects this tab.
    }

    /**
     * True if the mouse is outside this panel's bounds — used to gate tab-drag-out so
     * a click-to-switch-tab inside the panel doesn't get treated as a split.
     */
    private boolean isMouseOutsidePanel(float worldX, float worldY) {
        float px = getPositionX();
        float py = getPositionY();
        float pw = getSizeWidth();
        float ph = getSizeHeight();
        return worldX < px || worldX > px + pw || worldY < py || worldY > py + ph;
    }

    private void onTabDragUpdate(UIEvent e) {
        if (!(e.dragHandler.draggingObject instanceof DragTab dt) || dt.source != this) return;
        if (!isMouseOutsidePanel(e.x, e.y)) {
            // Still inside the panel — looks like a tab click, not a drag-out.
            graphView.dockManager.hideDockHighlight();
            return;
        }
        var target = graphView.dockManager.detectSlot(e.x, e.y);
        if (isDropOnSelf(target)) {
            graphView.dockManager.hideDockHighlight();
            return;
        }
        graphView.dockManager.showDockHighlight(target, this);
    }

    private void onTabDragEnd(UIEvent e) {
        graphView.dockManager.hideDockHighlight();
        if (!(e.dragHandler.draggingObject instanceof DragTab dt) || dt.source != this) return;
        // Need at least 2 tools at end too — the count could've changed mid-drag in pathological cases.
        if (tools.size() <= 1) return;
        // Mouse never left the panel: treat as a tab-switch click (TabView already handled it).
        if (!isMouseOutsidePanel(e.x, e.y)) return;

        var target = graphView.dockManager.detectSlot(e.x, e.y);
        // Drop back onto this same panel → no-op (TabView already switched tab).
        if (isDropOnSelf(target)) return;

        var tool = dt.tool;
        removeTool(tool);
        var newPanel = new GraphPanel(graphView, tool);
        graphView.getPanelLayer().addChild(newPanel);

        if (target == DockSlot.CENTER) {
            // Land at the drop point (panelLayer-local coordinates).
            var layer = graphView.getPanelLayer();
            float lx = e.x - layer.getPositionX() - 30f;
            float ly = e.y - layer.getPositionY() - 8f;
            newPanel.getLayout().left(lx).top(ly);
        }
        graphView.dockManager.dock(newPanel, target);
    }

    private void onTitleMouseDown(UIEvent e) {
        if (e.button != 0) return;
        if (collapseToggle.isSelfOrChildHover()) return;
        if (!canDragMove()) return;
        var icon = Icons.MOVE;
        var width = 12;
        var height = 12;
        titleBar.startDrag(new DragPanel(this, getLayoutX(), getLayoutY()), icon)
                .setDragTexture(-width / 2f, -height / 2f, width, height);
        isPanelDragging = true;
        e.stopPropagation();
    }

    private void onTitleDragUpdate(UIEvent e) {
        if (!(e.dragHandler.draggingObject instanceof DragPanel dp) || dp.source != this) return;
        var target = graphView.dockManager.detectSlot(e.x, e.y);
        if (isDropOnSelf(target)) {
            graphView.dockManager.hideDockHighlight();
        } else {
            graphView.dockManager.showDockHighlight(target, this);
        }

        // Visually follow the mouse during drag while the target is CENTER (i.e. not over a corner hit zone).
        if (target == DockSlot.CENTER) {
            // Drop IMPORTANT width/height left over from collapse so left/top take effect.
            var off = titleBar.getLocalMouseNormal(e.x - e.dragStartX, e.y - e.dragStartY);
            getLayout().left(dp.startLeft + off.x).top(dp.startTop + off.y);
        }
    }

    private void onTitleDragEnd(UIEvent e) {
        graphView.dockManager.hideDockHighlight();
        isPanelDragging = false;
        if (!(e.dragHandler.draggingObject instanceof DragPanel dp) || dp.source != this) return;
        var target = graphView.dockManager.detectSlot(e.x, e.y);
        if (isDropOnSelf(target)) {
            // Snap back to current slot's anchor — panel may have wandered during drag.
            applySlotLayout();
            return;
        }
        graphView.dockManager.dock(this, target);
    }

    // endregion

    public void setCollapsed(boolean collapsed) {
        if (this.isCollapsed == collapsed) return;
        if (collapsed) {
            content.setDisplay(false);
            title.getTextStyle().adaptiveWidth(true);
            Style.importantPipeline(getLayout(), l -> l.widthAuto().heightAuto());
        } else {
            content.setDisplay(true);
            title.getLayout().widthAuto();
            title.getTextStyle().adaptiveWidth(false);
            getStyleBag().removeCandidates(LayoutProperties.WIDTH, s -> s.origin() == StyleOrigin.IMPORTANT);
            getStyleBag().removeCandidates(LayoutProperties.HEIGHT, s -> s.origin() == StyleOrigin.IMPORTANT);
        }

        this.isCollapsed = collapsed;
        applySlotLayout();
    }

    protected boolean canDragMove() {
        return true;
    }

    protected boolean canResize() {
        return !isCollapsed && !activeHandles.isEmpty();
    }

    @Override
    protected void onLayoutChanged() {
        super.onLayoutChanged();
        applySlotLayout();
    }

    @Override
    protected void drawBackgroundAdditional(@NotNull IGUIContext guiContext) {
        if (!(guiContext instanceof GUIContext context)) return;
        super.drawBackgroundAdditional(context);
        if (canResize() && this.isSelfOrChildHover() && !isResizing) {
            WindowDragHelper.drawResizeIcon(context, this, 2, activeHandles);
        }
    }
}
