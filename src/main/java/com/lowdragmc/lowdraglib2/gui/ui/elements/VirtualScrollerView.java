package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
@KJSBindings
@LDLRegister(name = "virtual-scroller-view", group = "container", registry = "ldlib2:ui_element")
public class VirtualScrollerView<T> extends ScrollerView {
    @Configurable(name = "VirtualScrollerViewStyle")
    public class VirtualScrollerViewStyle extends Style {
        private static final Property<?>[] PROPERTIES = new Property[] {
                PropertyRegistry.VIRTUAL_ITEM_HEIGHT_MODE,
                PropertyRegistry.VIRTUAL_ESTIMATED_ITEM_HEIGHT,
                PropertyRegistry.VIRTUAL_OVERSCAN_PIXELS,
        };

        protected VirtualScrollerViewStyle() {
            super(VirtualScrollerView.this);
        }

        public static void init() {
            PropertyRegistry.VIRTUAL_ITEM_HEIGHT_MODE.addListener(VirtualScrollerView::onPropertyChanged);
            PropertyRegistry.VIRTUAL_ESTIMATED_ITEM_HEIGHT.addListener(VirtualScrollerView::onPropertyChanged);
            PropertyRegistry.VIRTUAL_OVERSCAN_PIXELS.addListener(VirtualScrollerView::onPropertyChanged);
        }

        @Override
        protected Property<?>[] getProperties() {
            return PROPERTIES;
        }

        public VirtualItemHeightMode itemHeightMode() {
            return getConfiguredValue(PropertyRegistry.VIRTUAL_ITEM_HEIGHT_MODE);
        }

        public VirtualScrollerViewStyle itemHeightMode(VirtualItemHeightMode mode) {
            set(PropertyRegistry.VIRTUAL_ITEM_HEIGHT_MODE, mode);
            return this;
        }

        public float estimatedItemHeight() {
            return getConfiguredValue(PropertyRegistry.VIRTUAL_ESTIMATED_ITEM_HEIGHT);
        }

        public VirtualScrollerViewStyle estimatedItemHeight(float height) {
            set(PropertyRegistry.VIRTUAL_ESTIMATED_ITEM_HEIGHT, height);
            return this;
        }

        public float overscanPixels() {
            return getConfiguredValue(PropertyRegistry.VIRTUAL_OVERSCAN_PIXELS);
        }

        public VirtualScrollerViewStyle overscanPixels(float pixels) {
            set(PropertyRegistry.VIRTUAL_OVERSCAN_PIXELS, pixels);
            return this;
        }

        private <V> V getConfiguredValue(Property<V> property) {
            return getValue(property)
                    .or(() -> java.util.Optional.ofNullable(getImportant(property)))
                    .or(() -> java.util.Optional.ofNullable(getInline(property)))
                    .or(() -> java.util.Optional.ofNullable(getDefault(property)))
                    .orElse(property.initialValue);
        }
    }

    private static <T> void onPropertyChanged(UIElement element, Property<T> property, @Nullable T oldValue, @Nullable T newValue) {
        if (element instanceof VirtualScrollerView<?> virtualScrollerView) {
            virtualScrollerView.onVirtualScrollerStyleChanged();
        }
    }

    @Getter
    private final VirtualScrollerViewStyle virtualScrollerViewStyle = new VirtualScrollerViewStyle();
    private final VirtualHeightIndex heightIndex = new VirtualHeightIndex();
    private List<T> items = List.of();
    private UIElementProvider<T> itemUIProvider = value -> new UIElement();
    private Runnable beforeMountItems = () -> {};
    private final Map<Integer, UIElement> mountedRows = new LinkedHashMap<>();
    @Getter
    private int firstMountedIndex = -1;
    @Getter
    private int lastMountedIndex = -1;
    private float lastScrollOffset;
    private float lastViewportHeight;

    public VirtualScrollerView() {
        super();
        resetHeightIndex();
    }

    public VirtualScrollerView<T> virtualScrollerViewStyle(Consumer<VirtualScrollerViewStyle> style) {
        style.accept(virtualScrollerViewStyle);
        return this;
    }

    public VirtualScrollerView<T> setItems(List<T> items) {
        this.items = List.copyOf(items);
        resetHeightIndex();
        refreshVisibleItems(lastScrollOffset, lastViewportHeight);
        return this;
    }

    public VirtualScrollerView<T> setItemUIProvider(UIElementProvider<T> itemUIProvider) {
        this.itemUIProvider = itemUIProvider;
        refreshVisibleItems(lastScrollOffset, lastViewportHeight);
        return this;
    }

    public VirtualScrollerView<T> setBeforeMountItems(Runnable beforeMountItems) {
        this.beforeMountItems = beforeMountItems;
        return this;
    }

    public int getItemCount() {
        return items.size();
    }

    public int getMountedItemCount() {
        if (firstMountedIndex < 0 || lastMountedIndex < firstMountedIndex) {
            return 0;
        }
        return lastMountedIndex - firstMountedIndex + 1;
    }

    public float getTotalVirtualHeight() {
        return heightIndex.getTotalHeight();
    }

    public void refreshVisibleItems() {
        refreshVisibleItems(getCurrentScrollOffset(), getCurrentViewportHeight());
    }

    public void refreshVisibleItems(float scrollOffset, float viewportHeight) {
        lastScrollOffset = Math.max(0, scrollOffset);
        lastViewportHeight = Math.max(0, viewportHeight);

        scrollerStyle(style -> style.adaptiveHeight(false));
        scrollerStyle(style -> style.adaptiveWidth(false));
        viewContainer.layout(layout -> layout.top(-lastScrollOffset));
        beforeMountItems.run();
        mountedRows.clear();
        clearAllScrollViewChildren();
        addScrollViewChild(createSpacer());

        if (items.isEmpty() || lastViewportHeight <= 0) {
            firstMountedIndex = -1;
            lastMountedIndex = -1;
            return;
        }

        var overscan = virtualScrollerViewStyle.overscanPixels();
        var startOffset = Math.max(0, lastScrollOffset - overscan);
        var endOffset = Math.min(heightIndex.getTotalHeight(), lastScrollOffset + lastViewportHeight + overscan);
        firstMountedIndex = heightIndex.findIndexAtOffset(startOffset);
        lastMountedIndex = heightIndex.findIndexAtOffset(endOffset);

        for (var index = firstMountedIndex; index <= lastMountedIndex; index++) {
            var row = createMountedRow(index);
            mountedRows.put(index, row);
            addScrollViewChild(row);
        }
    }

    public void scrollToTop() {
        verticalScroller.setNormalizedValue(0, false);
        refreshVisibleItems(0, getCurrentViewportHeight());
    }

    public boolean updateMeasuredItemHeight(int index, float measuredHeight) {
        return updateMeasuredItemHeight(index, measuredHeight, true);
    }

    private boolean updateMeasuredItemHeight(int index, float measuredHeight, boolean refresh) {
        var changed = heightIndex.updateMeasuredHeight(index, measuredHeight);
        if (changed && refresh) {
            refreshVisibleItems(lastScrollOffset, lastViewportHeight);
        }
        return changed;
    }

    @Override
    public void screenTick() {
        super.screenTick();
        if (virtualScrollerViewStyle.itemHeightMode() != VirtualItemHeightMode.VARIABLE || mountedRows.isEmpty()) {
            return;
        }

        var changed = false;
        for (var entry : List.copyOf(mountedRows.entrySet())) {
            var measuredHeight = getMeasuredRowHeight(entry.getValue());
            if (measuredHeight > 0) {
                changed |= updateMeasuredItemHeight(entry.getKey(), measuredHeight, false);
            }
        }
        if (changed) {
            refreshVisibleItems(lastScrollOffset, lastViewportHeight);
        }
    }

    private float getMeasuredRowHeight(UIElement row) {
        var rowHeight = row.getSizeHeight();
        if (rowHeight > 0 || row.getChildren().isEmpty()) {
            return rowHeight;
        }
        var child = row.getChildren().getFirst();
        var childHeight = child.getSizeHeight();
        return childHeight + child.getMarginTop() + child.getMarginBottom();
    }

    protected UIElement createMountedRow(int index) {
        var offset = heightIndex.getOffset(index);
        var row = new UIElement().layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.top(offset);
            layout.widthPercent(100);
        });
        row.addChild(itemUIProvider.apply(items.get(index)));
        return row;
    }

    protected UIElement createSpacer() {
        return new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.height(heightIndex.getTotalHeight());
        }).setAllowHitTest(false);
    }

    protected void onVirtualScrollerStyleChanged() {
        resetHeightIndex();
        refreshVisibleItems(lastScrollOffset, lastViewportHeight);
    }

    private void resetHeightIndex() {
        heightIndex.setMode(virtualScrollerViewStyle.itemHeightMode());
        heightIndex.reset(items.size(), virtualScrollerViewStyle.estimatedItemHeight());
    }

    private float getCurrentViewportHeight() {
        var viewportHeight = viewPort.getContentHeight();
        return viewportHeight > 0 ? viewportHeight : lastViewportHeight;
    }

    private float getCurrentScrollOffset() {
        var viewportHeight = getCurrentViewportHeight();
        return verticalScroller.getNormalizedValue() * Math.max(0, heightIndex.getTotalHeight() - viewportHeight);
    }

    @Override
    protected void onVerticalScroll(float value) {
        super.onVerticalScroll(value);
        refreshVisibleItems(value * Math.max(0, heightIndex.getTotalHeight() - getCurrentViewportHeight()), getCurrentViewportHeight());
    }
}
