package com.lowdragmc.lowdraglib2.editor.ui.view;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.lowdragmc.lowdraglib2.editor.resource.Resource;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceInstance;
import com.lowdragmc.lowdraglib2.editor.resource.Resources;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.editor.ui.browser.AssetBrowser;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceContainer;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Tab;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TabView;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.layout.LayoutProperties;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourceView extends View {
    /** Marks the view itself, so a theme can reach the whole thing without naming its type. */
    public static final String CLASS_ROOT = "__resource-view__";
    public static final String CLASS_DROP_PLACEHOLDER = "__resource-view_tab-placeholder__";

    /**
     * How far into the strip a press still counts as a grab of the resize handle, and how far into the
     * content.
     *
     * <p>Lopsided on purpose. A strip only one tab thick is all tab, so every pixel the handle reaches
     * back into it is a pixel of that tab that no longer selects it — while the content side of the
     * seam is border, and costs nothing. One in, three out keeps the handle the four pixels wide that
     * {@code SplitView} gives its own divider without eating into the tabs.
     */
    public static final float RESIZE_GRAB_INSIDE = 1;
    public static final float RESIZE_GRAB_OUTSIDE = 3;

    /** Token identifying a strip resize, so the drag handler's payload says which drag this is. */
    private static final Object RESIZING = new Object();

    /**
     * The payload of a tab reorder drag. Carries the view so a strip only ever reorders its own tabs —
     * two resource views on screen at once would otherwise both paint a drop placeholder.
     */
    public record TabDrag(ResourceView view, Tab tab) {}

    public final TabView tabView = new TabView();
    public final Editor editor;
    @Getter
    private final Map<Resource<?>, ResourceInstance<?>> resources = new HashMap<>();
    @Getter
    private final BiMap<Resource<?>, Tab> resourceTabs= HashBiMap.create();
    @Getter @Nullable
    private ResourceInstance<?> selectedResourceInstance = null;
    /**
     * The file system view of the {@code ldlib2} folder. It is pinned in front of the per resource type
     * tabs and survives project loading, so it is not part of {@link #resourceTabs}.
     */
    @Getter
    private final AssetBrowser assetBrowser;
    @Getter
    private final Tab assetBrowserTab;
    /**
     * The line between the pinned {@link #assetBrowserTab} and the resource tabs scrolling below it.
     */
    public final UIElement pinnedTabSeparator = new UIElement();

    /**
     * Where the strip sits, how thick it is, and the order and visibility of the resource tabs. Read
     * from disk when the view is built and written back whenever the user changes any of it.
     */
    @Getter
    private final ResourceTabLayout tabLayout;
    private final String layoutKey;

    // runtime
    /** The tab the pointer went down on, while it is still a click rather than a drag. */
    @Nullable
    private Tab pressedTab;
    /** The tab being dragged, hidden from the strip for as long as the drag lasts. */
    @Nullable
    private Tab draggingTab;
    /** Where the dragged tab would land if it were dropped now. */
    @Nullable
    private UIElement tabDropPlaceholder;
    /** True while {@link #loadResources} is adding a project's resources one at a time. */
    private boolean loadingResources;

    public ResourceView(Editor editor) {
        super("editor.view.resources");
        this.editor = editor;
        this.layoutKey = layoutKeyOf(editor);
        this.tabLayout = ResourceTabLayoutStore.load(layoutKey);
        addClass(CLASS_ROOT);
        getLayout().flexDirection(FlexDirection.ROW);

        tabView.layout(layout -> {
            layout.heightPercent(100);
            layout.flex(1);
        }).moveInlineAsDefault();
        tabView.tabContentContainer.layout(layout -> {
            layout.flex(1);
            layout.paddingAll(1);
        }).style(style -> style.backgroundTexture(IGuiTexture.EMPTY)).moveInlineAsDefault();
        tabView.tabHeaderContainer.layout(layout -> {
            layout.paddingHorizontal(1);
            layout.paddingVertical(1);
            // The thickness a strip has before anyone resizes it, on both axes so it is right whichever
            // side the strip ends up on. A default rather than a constant of the layout code, because a
            // theme whose tabs are not 16 pixels has to be able to say so — and cannot be left to size
            // the strip to its content instead, since a wrapping row with no width to wrap inside
            // simply puts every tab on one line.
            layout.width(ResourceTabLayout.DEFAULT_STRIP_SIZE);
            layout.height(ResourceTabLayout.DEFAULT_STRIP_SIZE);
        }).style(style -> style.backgroundTexture(Sprites.RECT_SOLID)).moveInlineAsDefault();
        // The strip decides its own scrolling from the placement, so the adaptive sizing TabView turns
        // on for a horizontal tab row has to go: it writes an IMPORTANT size big enough to hold every
        // tab, which is the opposite of what a strip the user gave a fixed thickness wants.
        tabView.tabScroller
                .scrollerStyle(style -> style.adaptiveWidth(false).adaptiveHeight(false))
                .layout(layout -> layout.marginBottom(0))
                .moveInlineAsDefault();
        tabView.setOnTabSelected(this::onResourceSelected);

        this.addChildren(tabView);

        this.assetBrowser = new AssetBrowser(editor);
        this.assetBrowserTab = createTab(Icons.FOLDER, Component.translatable("editor.view.assets"));
        tabView.addTab(assetBrowserTab, assetBrowser, 0);
        pinAssetBrowserTab();

        // Capture, so a press that lands on the seam becomes a resize instead of reaching the tab or the
        // scroller underneath it.
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown, true);
        addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onResizeDragUpdate);
        addEventListener(UIEvents.DRAG_END, this::onResizeDragEnd);

        tabView.tabHeaderContainer.addEventListener(UIEvents.MOUSE_DOWN, this::onStripMouseDown);
        // Capture on the three that never bubble, and the same handler for enter and update — see
        // showDropPlaceholder for why the marker cannot be left to the enter alone.
        tabView.tabHeaderContainer.addEventListener(UIEvents.DRAG_ENTER, this::showDropPlaceholder, true);
        tabView.tabHeaderContainer.addEventListener(UIEvents.DRAG_UPDATE, this::showDropPlaceholder, true);
        tabView.tabHeaderContainer.addEventListener(UIEvents.DRAG_LEAVE, this::onStripDragLeave, true);
        tabView.tabHeaderContainer.addEventListener(UIEvents.DRAG_PERFORM, this::onStripDragPerform);

        applyPlacement();
    }

    /**
     * Which saved arrangement this view uses. Per editor class, so two different editors that open the
     * same project type keep their own strips; override to share one between editors, or to give an
     * editor that is created many times over a stable key of its own.
     */
    protected String layoutKeyOf(Editor editor) {
        return editor.getClass().getName();
    }

    /// placement

    public ResourceTabLayout.Placement getPlacement() {
        return tabLayout.getPlacement();
    }

    /** True while the tab strip is a column — down the left or down the right of the content. */
    public boolean isColumnStrip() {
        return getPlacement().isColumn;
    }

    public void setPlacement(ResourceTabLayout.Placement placement) {
        if (tabLayout.getPlacement() == placement) return;
        tabLayout.setPlacement(placement);
        applyPlacement();
        saveTabLayout();
    }

    /**
     * The strip's thickness — a width for a column of tabs, a height for a row. The size the user gave
     * it, or the one it currently has on screen while they have never resized it.
     */
    public float getStripSize() {
        if (tabLayout.hasStripSize()) return tabLayout.getStripSize();
        var header = tabView.tabHeaderContainer;
        return isColumnStrip() ? header.getSizeWidth() : header.getSizeHeight();
    }

    public void setStripSize(float size) {
        previewStripSize(size);
        saveTabLayout();
    }

    /**
     * Sets the thickness without writing it down, for the length of a drag: the gesture is one
     * decision, so it is saved once when the pointer is let go rather than once per pixel.
     */
    protected void previewStripSize(float size) {
        tabLayout.setStripSize(Math.max(size, getMinimumStripSize()));
        applyStripSize();
    }

    /**
     * Hands the strip's thickness back to the theme — one tab thick, in every theme that ships — which
     * undoes a resize without disturbing the order or what is hidden.
     */
    public void fitStripSize() {
        tabLayout.clearStripSize();
        applyStripSize();
        saveTabLayout();
    }

    /**
     * Rebuilds every part of the strip that the placement decides: which way the view splits, which way
     * the tabs flow and wrap, which way the separator runs, and which axis the scroller and its wheel
     * work on.
     *
     * <p>All of it at {@code IMPORTANT}, because this is the arrangement the user picked rather than a
     * look a theme is entitled to an opinion about — a stylesheet turning the strip back into a row
     * while it is docked down the left would simply break it. Everything cosmetic (colours, borders,
     * padding, gaps, the tabs themselves) is left alone.
     */
    protected void applyPlacement() {
        var placement = getPlacement();
        var column = placement.isColumn;
        for (var other : ResourceTabLayout.Placement.values()) {
            removeClass(placementClass(other));
        }
        addClass(placementClass(placement));

        // TabView adds the content before the header, so the plain direction puts the header last —
        // right, or bottom — and the reversed one puts it first.
        tabView.layout(layout -> Style.importantPipeline(layout, style -> style.flexDirection(column
                ? (placement.isLeading ? FlexDirection.ROW_REVERSE : FlexDirection.ROW)
                : (placement.isLeading ? FlexDirection.COLUMN_REVERSE : FlexDirection.COLUMN))));

        tabView.tabHeaderContainer.layout(layout -> Style.importantPipeline(layout, style -> {
            style.flexDirection(column ? FlexDirection.COLUMN : FlexDirection.ROW);
            if (column) {
                style.heightPercent(100);
            } else {
                style.widthPercent(100);
            }
        }));

        // Across the strip, so it separates the pinned browser from the tabs whichever way they run.
        pinnedTabSeparator.layout(layout -> Style.importantPipeline(layout, style -> {
            if (column) {
                style.widthPercent(100);
                style.height(1);
                style.marginVertical(1);
                style.marginHorizontal(0);
            } else {
                style.width(1);
                style.heightPercent(100);
                style.marginHorizontal(1);
                style.marginVertical(0);
            }
        }));

        tabView.tabScroller.layout(layout -> Style.importantPipeline(layout, style -> {
            style.flex(1);
            if (column) {
                style.widthPercent(100);
                style.heightAuto();
            } else {
                style.heightPercent(100);
                style.widthAuto();
            }
        }));
        // NEVER on both bars: the strip is barely wider than a tab, so a bar drawn in it would eat the
        // tabs. The wheel still scrolls it, and Scroller.Horizontal takes the ordinary vertical wheel.
        tabView.tabScroller.scrollerStyle(style -> Style.importantPipeline(style, s -> s
                .mode(column ? ScrollerMode.VERTICAL : ScrollerMode.HORIZONTAL)
                .verticalScrollDisplay(ScrollDisplay.NEVER)
                .horizontalScrollDisplay(ScrollDisplay.NEVER)));

        // The tabs flow across the strip's short axis and wrap onto a second line once there is room
        // for one: across and down in a column of tabs, down and across in a row of them.
        tabContainer().layout(layout -> Style.importantPipeline(layout, style -> {
            style.flexDirection(column ? FlexDirection.ROW : FlexDirection.COLUMN);
            style.wrap(FlexWrap.WRAP);
            if (column) {
                style.widthPercent(100);
                style.heightAuto();
            } else {
                style.heightPercent(100);
                style.widthAuto();
            }
        }));

        applyStripSize();
    }

    /** The class a stylesheet reaches this placement by. */
    public static String placementClass(ResourceTabLayout.Placement placement) {
        return "__resource-view_placement-" + placement.styleName() + "__";
    }

    /**
     * Writes the remembered thickness onto whichever axis the current placement measures it on — or
     * takes that override off again, handing the axis back to the theme's default, when the user has
     * no size of their own.
     *
     * <p>The cross axis is never touched here: {@link #applyPlacement} pins it to the full length, and
     * it does so on whichever axis is the cross one now, so no size written for the other placement is
     * left standing. Nor is the floor applied here — that is {@link #updateStripMinimum}'s job, so that
     * it holds for a strip left at the theme's own thickness as well.
     */
    protected void applyStripSize() {
        var header = tabView.tabHeaderContainer;
        var column = isColumnStrip();
        var mainAxis = column ? LayoutProperties.WIDTH : LayoutProperties.HEIGHT;
        if (!tabLayout.hasStripSize()) {
            header.getStyleBag().removeCandidates(mainAxis, slot -> slot.origin() == StyleOrigin.IMPORTANT);
            return;
        }
        var size = tabLayout.getStripSize();
        header.layout(layout -> Style.importantPipeline(layout, style -> {
            if (column) {
                style.width(size);
            } else {
                style.height(size);
            }
        }));
    }

    /// minimum thickness

    /**
     * The thinnest the strip may be: one tab, plus whatever the header puts around it.
     *
     * <p>Measured rather than assumed, because a theme decides how big a tab is — and how much bigger
     * the selected one gets. The vanilla-styled themes grow the selected tab by a few pixels so it
     * stands proud of its neighbours, and a strip sized for an unselected tab clips that growth off:
     * the tab under the pointer is exactly the one that ends up cut in half.
     *
     * <p>Zero before the first layout, which is the right answer for "no constraint yet" — the strip
     * takes the theme's own thickness until there is something to measure.
     */
    public float getMinimumStripSize() {
        var header = tabView.tabHeaderContainer;
        var column = isColumnStrip();
        var thickest = 0f;
        for (var tab : allTabs()) {
            if (!tab.isDisplayed()) continue;
            var margin = tab.getTaffyLayout().margin();
            thickest = Math.max(thickest, column
                    ? tab.getSizeWidth() + margin.left + margin.right
                    : tab.getSizeHeight() + margin.top + margin.bottom);
        }
        if (thickest <= 0) return 0;
        var padding = header.getTaffyLayout().padding();
        var border = header.getTaffyLayout().border();
        return thickest + (column
                ? padding.left + padding.right + border.left + border.right
                : padding.top + padding.bottom + border.top + border.bottom);
    }

    /**
     * The scrolling part of the strip, which is what the resource tabs are children of — the pinned
     * browser tab and the divider are not in it, and neither is anything else the header holds.
     */
    protected UIElement tabContainer() {
        return tabView.tabScroller.viewContainer;
    }

    /** The pinned browser tab and every resource tab, in strip order, whether showing or not. */
    public List<Tab> allTabs() {
        var tabs = new ArrayList<Tab>(resourceTabs.size() + 1);
        tabs.add(assetBrowserTab);
        for (var child : tabContainer().getChildren()) {
            if (child instanceof Tab tab) {
                tabs.add(tab);
            }
        }
        return tabs;
    }

    /**
     * Keeps the strip from being thinner than its tabs, as a floor under both the size the user chose
     * and the one the theme asked for.
     *
     * <p>Written as a {@code min-width}/{@code min-height} rather than folded into the size, so a strip
     * left at the theme's thickness still gets the floor — and so switching theme, which can change how
     * big a tab is, is picked up on the next frame without anything having to ask for it.
     */
    protected void updateStripMinimum() {
        var minimum = getMinimumStripSize();
        if (minimum <= 0) return;
        var column = isColumnStrip();
        tabView.tabHeaderContainer.layout(layout -> Style.importantPipeline(layout, style -> {
            if (column) {
                style.minWidth(minimum);
                style.minHeight(0);
            } else {
                style.minHeight(minimum);
                style.minWidth(0);
            }
        }));
    }

    /// resizing

    /**
     * Where the strip meets the content, along the axis the strip is measured on — its right edge on
     * the left, its left edge on the right, and so on.
     */
    public float getStripSeam() {
        var header = tabView.tabHeaderContainer;
        var placement = getPlacement();
        if (placement.isColumn) {
            return placement.isLeading
                    ? header.getPositionX() + header.getSizeWidth()
                    : header.getPositionX();
        }
        return placement.isLeading
                ? header.getPositionY() + header.getSizeHeight()
                : header.getPositionY();
    }

    /** Whether a press at this point would grab the strip's resize handle rather than what is under it. */
    public boolean isOverResizeHandle(float mouseX, float mouseY) {
        if (!tabView.tabHeaderContainer.isDisplayed()) return false;
        var placement = getPlacement();
        var seam = getStripSeam();
        // The strip is on the low side of the seam for a leading placement and on the high side for a
        // trailing one, so which way the narrow half of the band faces turns with it.
        var near = seam - (placement.isLeading ? RESIZE_GRAB_INSIDE : RESIZE_GRAB_OUTSIDE);
        var far = seam + (placement.isLeading ? RESIZE_GRAB_OUTSIDE : RESIZE_GRAB_INSIDE);
        if (placement.isColumn) {
            return mouseX >= near && mouseX <= far
                    && mouseY >= getPositionY() && mouseY <= getPositionY() + getSizeHeight();
        }
        return mouseY >= near && mouseY <= far
                && mouseX >= getPositionX() && mouseX <= getPositionX() + getSizeWidth();
    }

    protected SpriteTexture getResizeIcon() {
        return isColumnStrip() ? Icons.ARROW_LEFT_RIGHT : Icons.ARROW_UP_DOWN;
    }

    protected void onMouseDown(UIEvent event) {
        if (event.button != 0 || !isOverResizeHandle(event.x, event.y)) return;
        var icon = getResizeIcon();
        var width = icon.spriteSize.width;
        var height = icon.spriteSize.height;
        startDrag(RESIZING, icon).setDragTexture(-width / 2f, -height / 2f, width, height);
        // Capture phase: taking the press here is what stops it also landing on the tab or the scroller
        // the seam happens to sit against.
        event.stopPropagation();
    }

    protected void onResizeDragUpdate(UIEvent event) {
        if (event.target != this || event.dragHandler.getDraggingObject() != RESIZING) return;
        var placement = getPlacement();
        var local = getLocalMouse(event.x, event.y);
        // Measured from the far edge of the pair, which stands still: measuring from the strip's own
        // near edge works on the left and the top only because the strip happens to start there, and
        // on the right and the bottom that edge is the one the drag is moving.
        var start = placement.isColumn ? tabView.getPositionX() : tabView.getPositionY();
        var extent = placement.isColumn ? tabView.getSizeWidth() : tabView.getSizeHeight();
        var pointer = placement.isColumn ? local.x : local.y;
        previewStripSize(placement.isLeading ? pointer - start : start + extent - pointer);
    }

    protected void onResizeDragEnd(UIEvent event) {
        if (event.target != this || event.dragHandler.getDraggingObject() != RESIZING) return;
        saveTabLayout();
    }

    @Override
    protected void drawBackgroundAdditional(IGUIContext guiContext) {
        super.drawBackgroundAdditional(guiContext);
        if (!(guiContext instanceof GUIContext context)) return;
        if (!isOverResizeHandle(context.mouseX, context.mouseY)) return;
        context.postRendering(ctx -> {
            var icon = getResizeIcon();
            var width = icon.spriteSize.width;
            var height = icon.spriteSize.height;
            ctx.drawTexture(icon,
                    ctx.localMouseX - width / 2f,
                    ctx.localMouseY - height / 2f,
                    width,
                    height);
        });
    }

    /// tabs

    /**
     * Lifts the browser's tab out of the scrolling strip and parks it at the head of the header, with a
     * divider between it and the tabs that do scroll.
     *
     * <p>It is the way into the file system rather than one resource type among many, so it has no
     * business scrolling out of reach the moment a project brings enough resources to overflow the
     * strip. Only the header element moves: {@link TabView#addTab} has already wired the click and
     * registered the content, and none of that cares where the element ends up — so selection behaves
     * exactly as it did.
     */
    private void pinAssetBrowserTab() {
        pinnedTabSeparator.style(style -> style.backgroundTexture(ColorPattern.T_WHITE.rectTexture()));
        pinnedTabSeparator.addClass("__resource-view_pinned-tab-separator__").moveInlineAsDefault();

        tabView.tabHeaderContainer.addChildAt(assetBrowserTab, 0);
        tabView.tabHeaderContainer.addChildAt(pinnedTabSeparator, 1);
    }

    private void onResourceSelected(Tab tab) {
        // the asset browser is not tied to a single resource type
        var resource = tab == assetBrowserTab ? null : resourceTabs.inverse().get(tab);
        selectedResourceInstance = resource == null ? null : getResourceInstance(resource);
    }

    private Tab createTab(IGuiTexture icon, Component tooltip) {
        var tab = new Tab().tabStyle(style -> {
            style.baseTexture(IGuiTexture.EMPTY);
            style.hoverTexture(Sprites.RECT_RD_T);
            style.selectedTexture(Sprites.RECT_RD_T);
        });
        tab.textStyle(style -> style.adaptiveWidth(false)).layout(layout -> {
            layout.width(14);
            layout.height(14);
            layout.paddingAll(1);
            layout.marginAll(1);
        }).style(style -> style.tooltips(tooltip)).addChild(new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        }).style(style -> style.backgroundTexture(icon)));
        tab.moveInlineAsDefault();
        return tab;
    }

    /**
     * Makes a resource tab draggable within the strip.
     *
     * <p>The drag starts on leaving the tab with the button still down rather than on the press itself,
     * which is what leaves an ordinary click free to select it — the same rule a view's own tab uses to
     * tell "switch to this" from "move this".
     */
    private void makeTabDraggable(Tab tab) {
        tab.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 0) {
                pressedTab = tab;
            }
        });
        tab.addEventListener(UIEvents.MOUSE_UP, event -> pressedTab = null);
        tab.addEventListener(UIEvents.MOUSE_LEAVE, event -> {
            if (pressedTab == tab && isMouseDown(0)) {
                startTabDrag(tab);
            }
            pressedTab = null;
        }, true);
        tab.addEventListener(UIEvents.DRAG_END, event -> endTabDrag());
    }

    private void startTabDrag(Tab tab) {
        var resource = resourceTabs.inverse().get(tab);
        if (resource == null) return;
        draggingTab = tab;
        // Square, and deliberately not the tab's own box: a theme is free to make tabs taller than they
        // are wide — the vanilla-styled ones do, so the selected tab can stand proud — and a resource
        // icon stretched to that shape is the one thing the user is looking straight at for the whole
        // gesture. The shorter side keeps it inside the footprint it came from.
        var side = Math.min(tab.getSizeWidth(), tab.getSizeHeight());
        tab.startDrag(new TabDrag(this, tab),
                        new GuiTextureGroup(ColorPattern.T_WHITE.rectTexture(), resource.getIcon()))
                .setDragTexture(-side / 2f, -side / 2f, side, side);
        // Out of the flow for the length of the drag, so the strip lays out as it would once the tab has
        // moved and the placeholder shows the real result rather than the result plus a gap.
        tab.setDisplay(false);
    }

    private void endTabDrag() {
        if (tabDropPlaceholder != null) {
            tabDropPlaceholder.removeSelf();
            tabDropPlaceholder = null;
        }
        if (draggingTab != null) {
            draggingTab.setDisplay(!isTabHidden(draggingTab));
            draggingTab = null;
        }
    }

    /** The tab drag this strip owns, or null for anything else being dragged over it. */
    @Nullable
    private Tab ownTabDrag(UIEvent event) {
        if (event.dragHandler == null) return null;
        return event.dragHandler.getDraggingObject() instanceof TabDrag(ResourceView view, Tab tab) && view == this
                ? tab : null;
    }

    /**
     * Puts the drop marker where the dragged tab would land, creating it on the way in.
     *
     * <p>Handles the way in and every move after it, because {@code dragEnter} is only sent when the
     * element under the pointer <em>changes</em> — and the element a drag that started in this strip is
     * already over need not be a change at all, which would leave the gesture with no marker for as
     * long as the pointer stayed put.
     */
    protected void showDropPlaceholder(UIEvent event) {
        if (ownTabDrag(event) == null) return;
        var container = tabContainer();
        if (tabDropPlaceholder == null) {
            tabDropPlaceholder = new UIElement().layout(layout -> {
                layout.width(14);
                layout.height(14);
                layout.paddingAll(1);
                layout.marginAll(1);
            }).style(style -> style.backgroundTexture(ColorPattern.GRAY.rectTexture()));
            // As a default rather than inline, so a theme can give the marker its own accent.
            tabDropPlaceholder.addClass(CLASS_DROP_PLACEHOLDER).moveInlineAsDefault();
            container.addChildAt(tabDropPlaceholder,
                    Mth.clamp(computeDropIndex(event.x, event.y), 0, container.getChildren().size()));
            return;
        }
        // Already under the pointer, so where the drop would land has not changed. Worth short
        // circuiting rather than recomputing the same answer: the marker takes a slot of its own, so
        // moving it shifts the very tabs the next index is measured against, and a marker that keeps
        // moving keeps changing its own answer — which reads as a flicker between two slots with the
        // pointer standing still.
        if (tabDropPlaceholder.isMouseOver(event.x, event.y)) return;
        var from = container.getChildren().indexOf(tabDropPlaceholder);
        var index = computeDropIndex(event.x, event.y);
        // Taking the marker out shifts everything after it down one.
        if (index > from) index--;
        if (index == from) return;
        tabDropPlaceholder.removeSelf();
        container.addChildAt(tabDropPlaceholder, Mth.clamp(index, 0, container.getChildren().size()));
    }

    protected void onStripDragLeave(UIEvent event) {
        if (tabView.tabHeaderContainer.isMouseOverElement(event.x, event.y)) return;
        if (tabDropPlaceholder != null) {
            tabDropPlaceholder.removeSelf();
            tabDropPlaceholder = null;
        }
    }

    protected void onStripDragPerform(UIEvent event) {
        if (tabDropPlaceholder == null) return;
        var tab = ownTabDrag(event);
        if (tab == null) return;
        var container = tabContainer();
        var index = container.getChildren().indexOf(tabDropPlaceholder);
        tabDropPlaceholder.removeSelf();
        tabDropPlaceholder = null;
        if (index >= 0) {
            moveTabTo(tab, index);
        }
    }

    /**
     * Where the dragged tab would be inserted if it were dropped at this point, counted over the
     * strip's children as they are — the drop marker included, when one is already showing.
     *
     * <p>Nearest tab first, then which side of it: a wrapped strip is a grid rather than a list, so
     * "the tab the pointer is over" is not always one that exists — the pointer spends a good part of
     * the gesture in the gaps between rows. Which side is read off the wrap axis when the pointer is
     * clear of the tab's line and off the flow axis when it is on it, so a pointer below the only tab
     * in a one-per-row strip lands after it rather than before it.
     */
    protected int computeDropIndex(float mouseX, float mouseY) {
        var children = tabContainer().getChildren();
        var flowIsX = isColumnStrip();
        var best = -1;
        var bestDistance = Float.MAX_VALUE;
        var after = false;
        for (int i = 0; i < children.size(); i++) {
            var child = children.get(i);
            if (child == tabDropPlaceholder || !child.isDisplayed()) continue;
            var dx = mouseX - (child.getPositionX() + child.getSizeWidth() / 2f);
            var dy = mouseY - (child.getPositionY() + child.getSizeHeight() / 2f);
            var distance = dx * dx + dy * dy;
            if (distance >= bestDistance) continue;
            bestDistance = distance;
            best = i;
            var flowDelta = flowIsX ? dx : dy;
            var crossDelta = flowIsX ? dy : dx;
            var crossHalf = (flowIsX ? child.getSizeHeight() : child.getSizeWidth()) / 2f;
            after = Math.abs(crossDelta) > crossHalf ? crossDelta > 0 : flowDelta > 0;
        }
        if (best < 0) return children.size();
        return after ? best + 1 : best;
    }

    /**
     * Moves a tab to {@code index}, counted over the strip's children with the placeholder already gone.
     */
    protected void moveTabTo(Tab tab, int index) {
        var container = tabContainer();
        var current = container.getChildren().indexOf(tab);
        if (current < 0) return;
        // Taking the tab out shifts everything after it down one, so a target past it has to follow.
        var target = current < index ? index - 1 : index;
        container.removeChild(tab);
        container.addChildAt(tab, Mth.clamp(target, 0, container.getChildren().size()));
        tab.setDisplay(!isTabHidden(tab));
        captureTabOrder();
    }

    /// visibility

    public boolean isResourceHidden(Resource<?> resource) {
        return tabLayout.isHidden(resource.getName());
    }

    private boolean isTabHidden(Tab tab) {
        var resource = resourceTabs.inverse().get(tab);
        return resource != null && isResourceHidden(resource);
    }

    /**
     * Shows or hides one resource's tab. A hidden tab keeps its resources and its place in the order;
     * it is only out of the strip, and out of the way, until it is asked for again.
     */
    public void setResourceHidden(Resource<?> resource, boolean hidden) {
        tabLayout.setHidden(resource.getName(), hidden);
        var tab = resourceTabs.get(resource);
        if (tab != null) {
            tab.setDisplay(!hidden);
            if (hidden && tabView.getSelectedTab() == tab) {
                selectFallbackTab();
            }
        }
        saveTabLayout();
    }

    public void toggleResourceHidden(Resource<?> resource) {
        setResourceHidden(resource, !isResourceHidden(resource));
    }

    /** Moves the selection off a tab that has just been hidden, onto the first one still showing. */
    private void selectFallbackTab() {
        for (var child : tabContainer().getChildren()) {
            if (child instanceof Tab tab && tab.isDisplayed()) {
                tabView.selectTab(tab);
                return;
            }
        }
        tabView.selectTab(assetBrowserTab);
    }

    /// layout persistence

    /**
     * Puts the strip back in the order and visibility that was saved, and parks anything the saved
     * order has never heard of after the tabs it does know — a resource type added by a mod installed
     * since then keeps a place rather than jumping to the front.
     */
    protected void applyTabLayout() {
        var container = tabContainer();
        var order = tabLayout.getOrder();
        if (!order.isEmpty()) {
            var sorted = new ArrayList<UIElement>();
            for (var name : order) {
                var resource = findResource(name);
                var tab = resource == null ? null : resourceTabs.get(resource);
                if (tab != null && container.hasChild(tab) && !sorted.contains(tab)) {
                    sorted.add(tab);
                }
            }
            for (var child : container.getChildren()) {
                if (!sorted.contains(child)) {
                    sorted.add(child);
                }
            }
            // Detaching and re-attaching a tab costs it its style registration and its animations, so it
            // is only worth doing when the strip is not already in the order that was asked for.
            if (!sorted.equals(container.getChildren())) {
                for (var child : sorted) {
                    container.removeChild(child);
                    container.addChild(child);
                }
            }
        }
        var selectedHidden = false;
        for (var entry : resourceTabs.entrySet()) {
            var hidden = isResourceHidden(entry.getKey());
            entry.getValue().setDisplay(!hidden);
            selectedHidden |= hidden && tabView.getSelectedTab() == entry.getValue();
        }
        if (selectedHidden) {
            selectFallbackTab();
        }
    }

    @Nullable
    private Resource<?> findResource(String name) {
        for (var resource : resourceTabs.keySet()) {
            if (resource.getName().equals(name)) return resource;
        }
        return null;
    }

    /** Reads the order back off the strip and remembers it. */
    protected void captureTabOrder() {
        var order = new ArrayList<String>();
        for (var child : tabContainer().getChildren()) {
            var resource = resourceTabs.inverse().get(child);
            if (resource != null) {
                order.add(resource.getName());
            }
        }
        // Resources that are not in the strip right now — another project's types — would be dropped by
        // a straight overwrite, so their remembered places are kept behind the ones that are.
        for (var name : tabLayout.getOrder()) {
            if (!order.contains(name) && findResource(name) == null) {
                order.add(name);
            }
        }
        tabLayout.setOrder(order);
        saveTabLayout();
    }

    protected void saveTabLayout() {
        ResourceTabLayoutStore.save(layoutKey, tabLayout);
    }

    /** Back to a plain one-tab-wide strip down the left, with every resource showing. */
    public void resetTabLayout() {
        tabLayout.reset();
        applyPlacement();
        applyTabLayout();
        saveTabLayout();
    }

    /// menu

    protected void onStripMouseDown(UIEvent event) {
        if (event.button != 1) return;
        editor.openMenu(this, event.x, event.y, createStripMenu(), false);
    }

    /**
     * The strip's own right-click menu. It stays open while it is used, because the visible-tabs
     * submenu is a set of toggles rather than a choice and ticking one entry must not put the rest
     * away — so every icon in it is read each frame instead of captured when the menu was built.
     */
    protected TreeBuilder.Menu createStripMenu() {
        var menu = TreeBuilder.Menu.start();
        menu.branch(Icons.PAGE_FIT, Component.translatable("editor.resources.placement"), placements -> {
            for (var placement : ResourceTabLayout.Placement.values()) {
                placements.leaf(
                        DynamicTexture.of(() -> getPlacement() == placement
                                ? Icons.RADIOBOX_MARKED : Icons.RADIOBOX_BLANK),
                        Component.translatable("editor.resources.placement." + placement.styleName()),
                        () -> setPlacement(placement));
            }
        });
        var tabs = orderedResources();
        if (!tabs.isEmpty()) {
            // A submenu of its own: the list is as long as the project has resource types, and it is a
            // set of toggles rather than a choice, so it has no business pushing the handful of entries
            // that are a choice off the bottom of the menu.
            menu.branch(Icons.EYE, Component.translatable("editor.resources.visible_tabs"), visibility -> {
                for (var resource : tabs) {
                    visibility.leaf(DynamicTexture.of(() -> isResourceHidden(resource)
                                    ? Icons.CHECKBOX_BLANK : Icons.CHECKBOX_MARKED),
                            resource.getDisplayName(), () -> toggleResourceHidden(resource));
                }
            });
        }
        menu.crossLine();
        menu.leaf(Icons.COLLAPSE_HORIZONTAL, Component.translatable("editor.resources.fit_strip"),
                this::fitStripSize);
        menu.leaf(Icons.REPLAY, Component.translatable("editor.resources.reset_layout"), this::resetTabLayout);
        return menu;
    }

    /** The resources in the order their tabs are in, so the menu reads like the strip does. */
    public List<Resource<?>> orderedResources() {
        var ordered = new ArrayList<Resource<?>>();
        for (var child : tabContainer().getChildren()) {
            var resource = resourceTabs.inverse().get(child);
            if (resource != null) {
                ordered.add(resource);
            }
        }
        return ordered;
    }

    /// resources

    public void addResourceInstance(ResourceInstance<?> resourceInstance) {
        var resource = resourceInstance.resource;
        var previous = resourceTabs.remove(resource);
        if (previous != null) {
            tabView.removeTab(previous);
        }
        var tab = createTab(resource.getIcon(), resource.getDisplayName());
        // registered before addTab: adding the first tab selects it right away, which calls back into
        // onResourceSelected and needs the tab to already be resolvable.
        resources.put(resource, resourceInstance);
        resourceTabs.put(resource, tab);
        tabView.addTab(tab, new ResourceContainer<>(resourceInstance, editor));
        makeTabDraggable(tab);
        if (!loadingResources) {
            applyTabLayout();
        }
    }

    public void addResourceInstances(ResourceInstance<?>... resources) {
        for (var resource : resources) {
            addResourceInstance(resource);
        }
    }

    public void loadResources(Resources resources) {
        // the selected tab is deliberately left alone: loading a project keeps whatever was showing,
        // which for a fresh editor and after clear() is the pinned asset browser
        loadingResources = true;
        try {
            resources.resources.stream().map(Resource::getResourceInstance).forEach(this::addResourceInstance);
        } finally {
            loadingResources = false;
        }
        // Once, with the whole set in hand. Applying it per resource would shuffle the strip into the
        // saved order as many times as there are resources, and each shuffle detaches every tab.
        applyTabLayout();
    }

    public void removeResource(Resource<?> resource) {
        var tab = resourceTabs.remove(resource);
        if (tab != null) {
            tabView.removeTab(tab);
        }
        resources.remove(resource);
        // Removing the selected tab hands the selection to whichever tab comes first, which may be one
        // the user has hidden — leaving a resource on screen with no tab to switch away from it.
        var selected = tabView.getSelectedTab();
        if (selected != null && !selected.isDisplayed()) {
            selectFallbackTab();
        }
    }

    public void clear() {
        // the resource tabs are removed one by one instead of clearing the whole tab view, so the
        // pinned asset browser tab stays in place (and keeps its selection state consistent).
        for (var tab : List.copyOf(resourceTabs.values())) {
            tabView.removeTab(tab);
        }
        resourceTabs.clear();
        resources.clear();
        selectedResourceInstance = null;
        tabView.selectTab(assetBrowserTab);
        assetBrowser.reset();
    }

    @Override
    public void screenTick() {
        super.screenTick();
        // the browser's borrowed resource behaviors have to keep flushing dirty resources to disk even
        // while another tab is showing, and hidden children are not ticked by the framework.
        assetBrowser.tickBehaviors();
        // Cheap and idempotent: a tab size only changes when the theme does, and writing the same
        // minimum back is dropped by the style bag before it reaches the layout.
        updateStripMinimum();
    }

    public void selectResourceInstance(Resource<?> resource) {
        var tab = resourceTabs.get(resource);
        if (tab != null) {
            tabView.selectTab(tab);
        }
    }

    /**
     * Get a resource by its name.
     */
    @Nullable
    public <T> ResourceInstance<T> getResourceInstance(Resource<?> resource) {
        return (ResourceInstance<T>) resources.get(resource);
    }

}
