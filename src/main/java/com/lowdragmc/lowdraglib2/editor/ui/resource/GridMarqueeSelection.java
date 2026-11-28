package com.lowdragmc.lowdraglib2.editor.ui.resource;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import dev.vfyjxf.taffy.style.TaffyPosition;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Reusable empty-space click-to-clear and rubber-band marquee drag selection for a {@link ScrollerView}
 * holding grid or list cells (used by both {@link ResourceProviderContainer} and
 * {@link com.lowdragmc.lowdraglib2.editor.ui.browser.AssetBrowser}).
 */
public final class GridMarqueeSelection<K> {
    public static final String CLASS_SELECTION_RECT = "__resource-drag-selection__";
    private static final float DRAG_THRESHOLD = 2f;

    private final class MarqueeDrag {
        final UIElement rect;
        final Set<K> baseSelection;
        boolean dragged;

        MarqueeDrag(UIElement rect, Set<K> baseSelection) {
            this.rect = rect;
            this.baseSelection = baseSelection;
        }
    }

    private final ScrollerView scrollerView;
    private final Supplier<Map<K, UIElement>> entriesSupplier;
    private final Supplier<Set<K>> currentSelectionSupplier;
    private final Consumer<Collection<K>> applySelection;
    @Nullable
    private UIElement activeRect;

    private GridMarqueeSelection(ScrollerView scrollerView,
                                 Supplier<Map<K, UIElement>> entriesSupplier,
                                 Supplier<Set<K>> currentSelectionSupplier,
                                 Consumer<Collection<K>> applySelection) {
        this.scrollerView = scrollerView;
        this.entriesSupplier = entriesSupplier;
        this.currentSelectionSupplier = currentSelectionSupplier;
        this.applySelection = applySelection;

        var viewPort = scrollerView.viewPort;
        viewPort.addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        viewPort.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onDragSourceUpdate);
        viewPort.addEventListener(UIEvents.DRAG_END, this::onDragEnd);
        viewPort.addEventListener(UIEvents.REMOVED, e -> clearActiveRect());
    }

    public static <K> GridMarqueeSelection<K> attach(ScrollerView scrollerView,
                                                     Supplier<Map<K, UIElement>> entriesSupplier,
                                                     Supplier<Set<K>> currentSelectionSupplier,
                                                     Consumer<Collection<K>> applySelection) {
        return new GridMarqueeSelection<>(scrollerView, entriesSupplier, currentSelectionSupplier, applySelection);
    }

    @Nullable
    public K keyAt(@Nullable UIElement target) {
        if (target == null) return null;
        var entries = entriesSupplier.get();
        if (entries.isEmpty()) return null;
        var path = target.getStructurePath();
        for (var entry : entries.entrySet()) {
            if (path.contains(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void clearActiveRect() {
        if (activeRect != null) {
            activeRect.removeSelf();
            activeRect = null;
        }
    }

    private void onMouseDown(UIEvent event) {
        if (event.button != 0) return;
        if (keyAt(event.target) != null) return;

        clearActiveRect();
        var additive = event.isCtrlDown() || event.isShiftDown();
        var baseSelection = additive
                ? new LinkedHashSet<>(currentSelectionSupplier.get())
                : new LinkedHashSet<K>();
        if (!additive && !currentSelectionSupplier.get().isEmpty()) {
            applySelection.accept(Set.of());
        }

        var rect = new UIElement();
        rect.addClass(CLASS_SELECTION_RECT);
        rect.setAllowHitTest(false);
        Style.importantPipeline(rect.getLayout(), l -> l
                .positionType(TaffyPosition.ABSOLUTE)
                .left(0)
                .top(0)
                .width(0)
                .height(0));
        Style.importantPipeline(rect.getStyle(), s -> s.background(
                new SDFRectTexture()
                        .setStroke(0.5f)
                        .setColor(ColorPattern.T_LIGHT_BLUE.color)
                        .setBorderColor(ColorPattern.LIGHT_BLUE.color)));

        var viewPort = scrollerView.viewPort;
        activeRect = rect;
        viewPort.addChild(rect);
        viewPort.startDrag(new MarqueeDrag(rect, baseSelection), null);
    }

    private void onDragSourceUpdate(UIEvent event) {
        if (event.dragHandler == null) return;
        if (!(event.dragHandler.getDraggingObject() instanceof GridMarqueeSelection<?>.MarqueeDrag drag)) return;
        if (drag.rect != activeRect) return;

        float minX = Math.min(event.dragStartX, event.x);
        float maxX = Math.max(event.dragStartX, event.x);
        float minY = Math.min(event.dragStartY, event.y);
        float maxY = Math.max(event.dragStartY, event.y);
        float width = maxX - minX;
        float height = maxY - minY;

        var viewPort = scrollerView.viewPort;
        var localPos = viewPort.getLocalMouse(minX, minY);
        var localSize = viewPort.getLocalMouseNormal(width, height);
        float rectLeft = localPos.x - viewPort.getContentX();
        float rectTop = localPos.y - viewPort.getContentY();
        Style.importantPipeline(drag.rect.getLayout(), l -> l
                .left(rectLeft)
                .top(rectTop)
                .width(localSize.x)
                .height(localSize.y));

        if (drag.dragged || width >= DRAG_THRESHOLD || height >= DRAG_THRESHOLD) {
            drag.dragged = true;
            // Clamp hit region to the visible viewport so scrolled-out cells are not selected.
            float vpMinX = viewPort.getContentX();
            float vpMinY = viewPort.getContentY();
            float vpMaxX = vpMinX + viewPort.getContentWidth();
            float vpMaxY = vpMinY + viewPort.getContentHeight();
            float hitMinX = Math.max(minX, vpMinX);
            float hitMaxX = Math.min(maxX, vpMaxX);
            float hitMinY = Math.max(minY, vpMinY);
            float hitMaxY = Math.min(maxY, vpMaxY);

            @SuppressWarnings("unchecked")
            var selection = new LinkedHashSet<K>((Set<K>) drag.baseSelection);
            if (hitMinX <= hitMaxX && hitMinY <= hitMaxY) {
                var entries = entriesSupplier.get();
                var cellToKey = new IdentityHashMap<UIElement, K>(entries.size());
                entries.forEach((k, ui) -> cellToKey.put(ui, k));
                for (var child : scrollerView.viewContainer.getChildren()) {
                    if (!child.isDisplayed()) continue;
                    var key = cellToKey.get(child);
                    if (key == null) continue;
                    float cx1 = child.getPositionX();
                    float cy1 = child.getPositionY();
                    float cx2 = cx1 + child.getSizeWidth();
                    float cy2 = cy1 + child.getSizeHeight();
                    if (cx1 <= hitMaxX && cx2 >= hitMinX && cy1 <= hitMaxY && cy2 >= hitMinY) {
                        selection.add(key);
                    }
                }
            }
            applySelection.accept(selection);
        }
    }

    private void onDragEnd(UIEvent event) {
        if (event.dragHandler == null) return;
        if (event.dragHandler.getDraggingObject() instanceof GridMarqueeSelection<?>.MarqueeDrag drag
                && drag.rect == activeRect) {
            clearActiveRect();
        }
    }
}
