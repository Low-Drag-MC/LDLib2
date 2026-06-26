package com.lowdragmc.lowdraglib2.editor.ui;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SplitView;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import com.mojang.datafixers.util.Pair;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Consumer;

@Accessors(chain = true)
@ParametersAreNonnullByDefault
public class SplittableWindow extends UIElement {
    public enum Edge {
        TOP, BOTTOM, LEFT, RIGHT;
    }
    @Configurable(name = "SplitStyle")
    public class SplitStyle extends Style {
        private static final Property<?>[] PROPERTIES = new Property[] {
                PropertyRegistry.PERCENTAGE,
                PropertyRegistry.MIN_PERCENTAGE,
                PropertyRegistry.MAX_PERCENTAGE,
        };

        public SplitStyle() {
            super(SplittableWindow.this);
        }

        public static void init() {
            PropertyRegistry.PERCENTAGE.addListener(SplitStyle::onPropertyChanged);
            PropertyRegistry.MIN_PERCENTAGE.addListener(SplitStyle::onPropertyChanged);
            PropertyRegistry.MAX_PERCENTAGE.addListener(SplitStyle::onPropertyChanged);
        }

        private static <T> void onPropertyChanged(UIElement element, Property<T> property, @org.jetbrains.annotations.Nullable T oldValue, @org.jetbrains.annotations.Nullable T newValue) {
            if (element instanceof SplittableWindow splittableWindow) {
                splittableWindow.onSplitStyleChanged();
            }
        }

        @Override
        protected Property<?>[] getProperties() {
            return PROPERTIES;
        }

        public float percentage() {
            return getValueSave(PropertyRegistry.PERCENTAGE);
        }

        public SplitStyle percentage(float percentage) {
            set(PropertyRegistry.PERCENTAGE, percentage);
            return this;
        }

        public float minPercentage() {
            return getValueSave(PropertyRegistry.MIN_PERCENTAGE);
        }

        public SplitStyle minPercentage(float minPercentage) {
            set(PropertyRegistry.MIN_PERCENTAGE, minPercentage);
            return this;
        }

        public float maxPercentage() {
            return getValueSave(PropertyRegistry.MAX_PERCENTAGE);
        }

        public SplitStyle maxPercentage(float maxPercentage) {
            set(PropertyRegistry.MAX_PERCENTAGE, maxPercentage);
            return this;
        }
    }
    @Getter
    private final SplitStyle splitStyle = new SplitStyle();
    @Nullable
    @Getter @Setter
    protected SplittableWindow parentWindow;
    @Getter @Setter
    protected boolean immortal = false;
    /**
     * Optional stable identifier so this window can be located after a layout reload.
     * Used by {@link com.lowdragmc.lowdraglib2.editor.ui.Editor} to rebind named anchor windows.
     */
    @Nullable @Getter @Setter
    protected String anchorId;

    // runtime
    @Nullable
    @Getter
    private ViewContainer viewContainer;
    @Nullable
    @Getter
    private SplitView splitView;
    @Nullable
    @Getter
    private SplittableWindow first, second;

    public SplittableWindow() {
        this(null);
    }

    public SplittableWindow(@Nullable SplittableWindow parent) {
        this(parent, new ViewContainer());
    }

    public SplittableWindow(@Nullable SplittableWindow parent, @Nonnull ViewContainer viewContainer) {
        getLayout().widthPercent(100);
        getLayout().heightPercent(100);
        this.parentWindow = parent;
        setViewContainer(viewContainer);

        addEventListener(UIEvents.DRAG_ENTER, this::onDragEnter, true);
        addEventListener(UIEvents.DRAG_LEAVE, this::onDragLeave, true);
        addEventListener(UIEvents.DRAG_PERFORM, this::onDragPerform);
    }

    public SplittableWindow splitStyle(Consumer<SplitStyle> styleConsumer) {
        styleConsumer.accept(splitStyle);
        return this;
    }

    protected void onSplitStyleChanged() {
        if (splitView != null) {
            this.splitView.setMinPercentage(splitStyle.minPercentage())
                    .setMaxPercentage(splitStyle.maxPercentage())
                    .setPercentage(splitStyle.percentage());
        }
    }

    public LayoutConfig getLayoutConfig() {
        boolean vertical = splitView instanceof SplitView.Vertical;
        // Note: user-dragged percentages are not currently restored. SplitView mutates its own layout
        // on drag but does not update splitStyle, so persisting splitView.getPercentage() and replaying
        // it via the layout-restore path produces visually broken sizes. We save the construction-time
        // default so reload is a no-op for sizes; view placement still restores correctly.
        return new LayoutConfig(anchorId, vertical, splitStyle.percentage(),
                first != null ? first.getLayoutConfig() : null,
                second != null ? second.getLayoutConfig() : null);
    }

    public SplittableWindow applyLayoutConfig(LayoutConfig layoutConfig) {
        splitStyle(style -> style.percentage(layoutConfig.percentage));
        if (isSplit()) {
            if (first != null && layoutConfig.first != null) {
                first.applyLayoutConfig(layoutConfig.first);
            }
            if (second != null && layoutConfig.second != null) {
                second.applyLayoutConfig(layoutConfig.second);
            }
        }
        return this;
    }

    /**
     * Tear down this window's children/splits and rebuild its substructure from the given LayoutConfig.
     * Anchor windows referenced by the supplied {@code anchorRegistry} are reused when an {@code anchorId}
     * appears in {@code config}; otherwise plain new windows are created. Leaves end up with empty
     * {@link ViewContainer}s. After rebuild, the {@code anchorRegistry} reflects the surviving anchors
     * found in the new tree (entries whose ID is absent from {@code config} are dropped).
     *
     * @param config         the target layout shape; if {@code null} the window is reset to a single empty container
     * @param anchorRegistry mutable map (id → window) consulted before creating new SplittableWindows;
     *                       updated in place to reflect the rebuilt tree (only ids present in config survive)
     */
    public void rebuildFromLayoutConfig(@Nullable LayoutConfig config, Map<String, SplittableWindow> anchorRegistry) {
        Map<String, SplittableWindow> survivors = new HashMap<>();
        rebuildInternal(config, anchorRegistry, survivors);
        anchorRegistry.clear();
        anchorRegistry.putAll(survivors);
    }

    private void rebuildInternal(@Nullable LayoutConfig config, Map<String, SplittableWindow> anchorRegistry, Map<String, SplittableWindow> survivors) {
        // detach all existing children/splits
        if (splitView != null) {
            splitView.removeSelf();
            splitView = null;
        }
        if (viewContainer != null) {
            viewContainer.removeSelf();
            viewContainer = null;
        }
        first = null;
        second = null;

        if (config == null) {
            setViewContainer(new ViewContainer());
            return;
        }

        // rebind this node's anchor id from the config
        this.anchorId = config.anchorId();
        if (config.anchorId() != null) {
            survivors.put(config.anchorId(), this);
        }
        splitStyle.percentage(config.percentage());

        boolean isLeaf = config.first() == null && config.second() == null;
        if (isLeaf) {
            setViewContainer(new ViewContainer());
            return;
        }

        // reconstruct as a split window
        var firstChild = createChildForConfig(config.first(), anchorRegistry, survivors);
        var secondChild = createChildForConfig(config.second(), anchorRegistry, survivors);
        firstChild.parentWindow = this;
        secondChild.parentWindow = this;
        this.first = firstChild;
        this.second = secondChild;

        if (config.vertical()) {
            this.splitView = new SplitView.Vertical().top(firstChild).bottom(secondChild);
        } else {
            this.splitView = new SplitView.Horizontal().left(firstChild).right(secondChild);
        }
        this.splitView.setPercentage(splitStyle.percentage());
        this.splitView.setMinPercentage(splitStyle.minPercentage());
        this.splitView.setMaxPercentage(splitStyle.maxPercentage());
        addChild(splitView);
    }

    private SplittableWindow createChildForConfig(@Nullable LayoutConfig childConfig, Map<String, SplittableWindow> anchorRegistry, Map<String, SplittableWindow> survivors) {
        SplittableWindow window = null;
        if (childConfig != null && childConfig.anchorId() != null) {
            window = anchorRegistry.get(childConfig.anchorId());
        }
        if (window == null) {
            window = new SplittableWindow(this);
        } else {
            window.parentWindow = this;
        }
        window.rebuildInternal(childConfig, anchorRegistry, survivors);
        return window;
    }

    public List<View> getAllViews() {
        var views = new ArrayList<View>();
        if (viewContainer != null) {
            views.addAll(viewContainer.getAllViews());
        }
        if (splitView != null) {
            if (first != null) views.addAll(first.getAllViews());
            if (second != null) views.addAll(second.getAllViews());
        }
        return views;
    }

    public SplittableWindow setViewContainer(@Nonnull ViewContainer viewContainer) {
        if (this.viewContainer != null) {
            this.viewContainer.removeSelf();
        }
        if (splitView != null) {
            splitView.removeSelf();
            splitView = null;
        }
        this.viewContainer = viewContainer;
        viewContainer._setWindowInternal(this);
        addChild(viewContainer);
        return this;
    }

    public ViewContainer getLeftTop() {
        if (this.viewContainer != null) return this.viewContainer;
        if (splitView instanceof SplitView.Vertical) {
            if (first != null) return first.getLeftTop();
            if (second != null) return second.getLeftTop();
        } else if (splitView instanceof SplitView.Horizontal) {
            if (first != null) return first.getLeftTop();
            if (second != null) return second.getLeftTop();
        }
        setViewContainer(new ViewContainer());
        return this.viewContainer;
    }

    public ViewContainer getLeftBottom() {
        if (this.viewContainer != null) return this.viewContainer;
        if (splitView instanceof SplitView.Vertical) {
            if (second != null) return second.getLeftBottom();
            if (first != null) return first.getLeftBottom();
        } else if (splitView instanceof SplitView.Horizontal) {
            if (first != null) return first.getLeftBottom();
            if (second != null) return second.getLeftBottom();
        }
        setViewContainer(new ViewContainer());
        return this.viewContainer;
    }

    public ViewContainer getRightTop() {
        if (this.viewContainer != null) return this.viewContainer;
        if (splitView instanceof SplitView.Vertical) {
            if (first != null) return first.getRightTop();
            if (second != null) return second.getRightTop();
        } else if (splitView instanceof SplitView.Horizontal) {
            if (second != null) return second.getRightTop();
            if (first != null) return first.getRightTop();
        }
        setViewContainer(new ViewContainer());
        return this.viewContainer;
    }

    public ViewContainer getRightBottom() {
        if (this.viewContainer != null) return this.viewContainer;
        if (splitView instanceof SplitView.Vertical) {
            if (second != null) return second.getRightBottom();
            if (first != null) return first.getRightBottom();
        } else if (splitView instanceof SplitView.Horizontal) {
            if (second != null) return second.getRightBottom();
            if (first != null) return first.getRightBottom();
        }
        setViewContainer(new ViewContainer());
        return this.viewContainer;
    }

    public boolean isSplit() {
        return splitView != null;
    }

    /**
     * Splits the current window horizontally or vertically based on the specified edge and
     * creates two new windows in the process.
     *
     * @param edge the edge of the window to split, which can be one of {@code Edge.TOP}, {@code Edge.BOTTOM},
     *             {@code Edge.LEFT}, or {@code Edge.RIGHT}
     * @param newWindow the new window to be placed on the specified edge after splitting
     * @return a pair of SplittableWindows: the split window on the specified edge and the remaining portion
     *         of the original window
     */
    public Pair<SplittableWindow, SplittableWindow> splitWith(Edge edge, SplittableWindow newWindow) {
        if (this.splitView != null) throw new IllegalStateException("Cannot split a split window");
        if (this.viewContainer == null) throw new IllegalStateException("Cannot split a window that is empty");
        if (edge == Edge.TOP) {
            this.splitView = new SplitView.Vertical()
                    .top(first = newWindow)
                    .bottom(second = new SplittableWindow(this, this.viewContainer));
        } else if (edge == Edge.BOTTOM) {
            this.splitView = new SplitView.Vertical()
                    .top(first = new SplittableWindow(this, this.viewContainer))
                    .bottom(second = newWindow);
        } else if (edge == Edge.LEFT) {
            this.splitView = new SplitView.Horizontal()
                    .left(first = newWindow)
                    .right(second = new SplittableWindow(this, this.viewContainer));
        } else if (edge == Edge.RIGHT) {
            this.splitView = new SplitView.Horizontal()
                    .left(first = new SplittableWindow(this, this.viewContainer))
                    .right(second = newWindow);
        } else {
            throw new IllegalArgumentException("Invalid edge: " + edge);
        }
        this.viewContainer = null;
        this.splitView.setPercentage(splitStyle.percentage());
        this.splitView.setMinPercentage(splitStyle.minPercentage());
        this.splitView.setMaxPercentage(splitStyle.maxPercentage());
        addChild(splitView);
        return Pair.of(first, second);
    }

    /**
     * Splits the current window into two new windows based on the specified edge and returns the resulting pair of windows.
     *
     * @param edge the edge of the window to split, which can be one of {@code Edge.TOP}, {@code Edge.BOTTOM},
     *             {@code Edge.LEFT}, or {@code Edge.RIGHT}
     * @return a pair of SplittableWindows, where the first element is the window created on the specified edge
     *         and the second element is the remaining portion of the original window
     */
    public Pair<SplittableWindow, SplittableWindow> splitNew(Edge edge) {
        SplittableWindow newWindow = new SplittableWindow(this);
        return splitWith(edge, newWindow);
    }

    protected ViewContainer getEmptyOrSplitContainer(Edge edge) {
        if (splitView != null) {
            splitView.removeSelf();
            splitView = null;
        }
        if (this.viewContainer == null) {
            setViewContainer(new ViewContainer());
            return this.viewContainer;
        }
        if (this.viewContainer.isEmptyWindow()) {
            return this.viewContainer;
        }
        if (edge == Edge.TOP) {
            return Objects.requireNonNull(splitNew(edge).getFirst().getViewContainer());
        } else if (edge == Edge.BOTTOM) {
            return Objects.requireNonNull(splitNew(edge).getSecond().getViewContainer());
        } else if (edge == Edge.LEFT) {
            return Objects.requireNonNull(splitNew(edge).getFirst().getViewContainer());
        } else if (edge == Edge.RIGHT) {
            return Objects.requireNonNull(splitNew(edge).getSecond().getViewContainer());
        } else {
            throw new IllegalArgumentException("Invalid edge: " + edge);
        }
    }

    private boolean isWindowHovering(float mouseX, float mouseY) {
        var container = viewContainer == null ? this : viewContainer.tabView.tabContentContainer;
        return container.isMouseOverElement(mouseX, mouseY);
    }

    private boolean isBorderHovering(Edge edge, float mouseX, float mouseY) {
        var borderPercent = 0.2f;
        var container = viewContainer == null ? this : viewContainer.tabView.tabContentContainer;
        var x = container.getPositionX();
        var y = container.getPositionY();
        var w = container.getSizeWidth();
        var h = container.getSizeHeight();
        if (edge == Edge.TOP) {
            return isMouseOver(x, y, w, h * borderPercent, mouseX, mouseY);
        } else if (edge == Edge.BOTTOM) {
            return isMouseOver(x, y + h * (1 - borderPercent), w, h * borderPercent, mouseX, mouseY);
        } else if (edge == Edge.LEFT) {
            return isMouseOver(x, y, w * borderPercent, h, mouseX, mouseY);
        } else if (edge == Edge.RIGHT) {
            return isMouseOver(x + w * (1 - borderPercent), y, w * borderPercent, h, mouseX, mouseY);
        } else {
            throw new IllegalArgumentException("Invalid edge: " + edge);
        }
    }

    protected void onDragEnter(UIEvent event) {
        if (isSplit()) return;
        // check if a view is being dragged into the view
        if (event.dragHandler.draggingObject instanceof View) {
            style(style -> style.overlayTexture(GuiTexture.of(this::drawOverlay)));
        }
    }

    protected void onDragLeave(UIEvent event) {
        if (isSplit()) return;
        if (event.relatedTarget == null || !this.isAncestorOf(event.relatedTarget)) {
            style(style -> style.overlayTexture(IGuiTexture.EMPTY));
        }
    }

    protected void onDragPerform(UIEvent event) {
        if (isSplit()) return;
        style(style -> style.overlayTexture(IGuiTexture.EMPTY));
        if (event.dragHandler.draggingObject instanceof View view) {
            if (isBorderHovering(Edge.TOP, event.x, event.y)) {
                tryMoveToNewWindow(view, Edge.TOP);
            } else if (isBorderHovering(Edge.BOTTOM, event.x, event.y)) {
                tryMoveToNewWindow(view, Edge.BOTTOM);
            } else if (isBorderHovering(Edge.LEFT, event.x, event.y)) {
                tryMoveToNewWindow(view, Edge.LEFT);
            } else if (isBorderHovering(Edge.RIGHT, event.x, event.y)) {
                tryMoveToNewWindow(view, Edge.RIGHT);
            } else if (isWindowHovering(event.x, event.y)) {
                if (viewContainer == null) {
                    setViewContainer(new ViewContainer());
                }
                if (!viewContainer.hasView(view)) {
                    viewContainer.addView(view);
                    viewContainer.selectView(view);
                }
            }
        }
    }

    protected void onWindowsEmpty() {
        if (immortal) return;
        if (parentWindow != null) {
            parentWindow.removeSplitWindow(this);
        }
    }

    public void removeSplitWindow(SplittableWindow window) {
        if (this.parentWindow == null) return;
        var target = window == this.first ? this.second : this.first;
        if (target != null && this.parentWindow.splitView != null) {
            if (this == this.parentWindow.first) {
                this.parentWindow.first = target;
                this.parentWindow.splitView.first(target);
            } else {
                this.parentWindow.second = target;
                this.parentWindow.splitView.second(target);
            }
            target.parentWindow = this.parentWindow;
        }
    }

    protected void tryMoveToNewWindow(View view, Edge edge) {
        var oldContainer = view.getViewContainer();
        if (oldContainer == this.viewContainer && oldContainer != null && oldContainer.tabView.getTabContents().size() == 1) {
            return;
        }
        getEmptyOrSplitContainer(edge).addView(view);
    }

    private void drawOverlay(GUIContext context, float x, float y, float width, float height) {
        var isWindowEmpty = viewContainer == null || viewContainer.isEmptyWindow();
        var mui = getModularUI();
        if (mui != null) {
            if (mui.getDragHandler().getDraggingObject() instanceof View view) {
                var oldContainer = view.getViewContainer();
                if (oldContainer == this.viewContainer && oldContainer != null && oldContainer.tabView.getTabContents().size() == 1) {
                    isWindowEmpty = true;
                }
            }
        }
        var container = viewContainer == null ? this : viewContainer.tabView.tabContentContainer;
        x = container.getPositionX();
        y = container.getPositionY();
        width = container.getSizeWidth();
        height = container.getSizeHeight();
        var mouseX = context.mouseX;
        var mouseY = context.mouseY;
        if (isWindowHovering(mouseX, mouseY)) {
            if (!isWindowEmpty) {
                if (isBorderHovering(Edge.TOP, mouseX, mouseY)) {
                    DrawerHelperClient.drawSolidRect(context, x, y, width, height * 0.5f, ColorPattern.T_BLUE.color);
                } else if (isBorderHovering(Edge.BOTTOM, mouseX, mouseY)) {
                    DrawerHelperClient.drawSolidRect(context, x, y + height * 0.5f, width, height * 0.5f, ColorPattern.T_BLUE.color);
                } else if (isBorderHovering(Edge.LEFT, mouseX, mouseY)) {
                    DrawerHelperClient.drawSolidRect(context, x, y, width * 0.5f, height, ColorPattern.T_BLUE.color);
                } else if (isBorderHovering(Edge.RIGHT, mouseX, mouseY)) {
                    DrawerHelperClient.drawSolidRect(context, x + width * 0.5f, y, width * 0.5f, height, ColorPattern.T_BLUE.color);
                } else {
                    DrawerHelperClient.drawSolidRect(context, x, y, width, height, ColorPattern.T_BLUE.color);
                }
            } else {
                DrawerHelperClient.drawSolidRect(context, x, y, width, height, ColorPattern.T_BLUE.color);
            }
        }
    }


    public record LayoutConfig(@Nullable String anchorId, boolean vertical, float percentage,
                               @Nullable LayoutConfig first, @Nullable LayoutConfig second) {
        public CompoundTag serialize() {
            var tag = new CompoundTag();
            if (anchorId != null) tag.putString("anchorId", anchorId);
            tag.putBoolean("vertical", vertical);
            tag.putFloat("percentage", percentage);
            if (first != null) tag.put("first", first.serialize());
            if (second != null) tag.put("second", second.serialize());
            return tag;
        }

        public static LayoutConfig deserialize(CompoundTag tag) {
            var anchorId = tag.contains("anchorId") ? tag.getStringOr("anchorId", "") : null;
            var vertical = tag.getBooleanOr("vertical", false);
            var percentage = tag.getFloatOr("percentage", 0.5f);
            var first = tag.contains("first") ? deserialize(tag.getCompoundOrEmpty("first")) : null;
            var second = tag.contains("second") ? deserialize(tag.getCompoundOrEmpty("second")) : null;
            return new LayoutConfig(anchorId, vertical, percentage, first, second);
        }
    }

}
