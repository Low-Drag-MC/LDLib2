package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import org.appliedenergistics.yoga.*;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
@KJSBindings
@LDLRegister(name = "scroller-view", group = "container", registry = "ldlib2:ui_element")
public class ScrollerView extends UIElement {
    @Configurable(name = "ScrollerViewStyle")
    public class ScrollerViewStyle extends Style {
        private static final Property<?>[] PROPERTIES = new Property[] {
                PropertyRegistry.SCROLLER_VIEW_MARGIN,
                PropertyRegistry.SCROLLER_VIEW_MODE,
                PropertyRegistry.SCROLLER_VERTICAL_DISPLAY,
                PropertyRegistry.SCROLLER_HORIZONTAL_DISPLAY,
                PropertyRegistry.ADAPTIVE_WIDTH,
                PropertyRegistry.ADAPTIVE_HEIGHT,
                PropertyRegistry.MIN_SCROLL_PIXEL,
                PropertyRegistry.MAX_SCROLL_PIXEL,
        };

        public ScrollerViewStyle() {
            super(ScrollerView.this);
        }

        public static void init() {
            PropertyRegistry.SCROLLER_VIEW_MARGIN.addListener(ScrollerViewStyle::onPropertyChanged);
            PropertyRegistry.SCROLLER_VIEW_MODE.addListener(ScrollerViewStyle::onPropertyChanged);
            PropertyRegistry.SCROLLER_VERTICAL_DISPLAY.addListener(ScrollerViewStyle::onPropertyChanged);
            PropertyRegistry.SCROLLER_HORIZONTAL_DISPLAY.addListener(ScrollerViewStyle::onPropertyChanged);
            PropertyRegistry.ADAPTIVE_WIDTH.addListener(ScrollerViewStyle::onPropertyChanged);
            PropertyRegistry.ADAPTIVE_HEIGHT.addListener(ScrollerViewStyle::onPropertyChanged);
        }

        private static <T> void onPropertyChanged(UIElement element, Property<T> property, @Nullable T oldValue, @Nullable T newValue) {
            if (element instanceof ScrollerView scrollerView) {
                scrollerView.updateScrollers();
            }
        }

        @Override
        protected Property<?>[] getProperties() {
            return PROPERTIES;
        }

        public float scrollerViewMargin() {
            return getValueSave(PropertyRegistry.SCROLLER_VIEW_MARGIN);
        }

        public ScrollerViewStyle scrollerViewStyle(float scrollerViewMargin) {
            set(PropertyRegistry.SCROLLER_VIEW_MARGIN, scrollerViewMargin);
            return this;
        }

        public ScrollerMode mode() {
            return getValueSave(PropertyRegistry.SCROLLER_VIEW_MODE);
        }

        public ScrollerViewStyle mode(ScrollerMode mode) {
            set(PropertyRegistry.SCROLLER_VIEW_MODE, mode);
            return this;
        }

        public ScrollDisplay verticalScrollDisplay() {
            return getValueSave(PropertyRegistry.SCROLLER_VERTICAL_DISPLAY);
        }

        public ScrollerViewStyle verticalScrollDisplay(ScrollDisplay display) {
            set(PropertyRegistry.SCROLLER_VERTICAL_DISPLAY, display);
            return this;
        }

        public ScrollDisplay horizontalScrollDisplay() {
            return getValueSave(PropertyRegistry.SCROLLER_HORIZONTAL_DISPLAY);
        }

        public ScrollerViewStyle horizontalScrollDisplay(ScrollDisplay display) {
            set(PropertyRegistry.SCROLLER_HORIZONTAL_DISPLAY, display);
            return this;
        }

        public boolean adaptiveWidth() {
            return getValueSave(PropertyRegistry.ADAPTIVE_WIDTH);
        }

        public ScrollerViewStyle adaptiveWidth(boolean adaptiveWidth) {
            set(PropertyRegistry.ADAPTIVE_WIDTH, adaptiveWidth);
            return this;
        }

        public boolean adaptiveHeight() {
            return getValueSave(PropertyRegistry.ADAPTIVE_HEIGHT);
        }

        public ScrollerViewStyle adaptiveHeight(boolean adaptiveHeight) {
            set(PropertyRegistry.ADAPTIVE_HEIGHT, adaptiveHeight);
            return this;
        }

        public float minScrollPixel() {
            return getValueSave(PropertyRegistry.MIN_SCROLL_PIXEL);
        }

        public ScrollerViewStyle minScrollPixel(float minScrollPixel) {
            set(PropertyRegistry.MIN_SCROLL_PIXEL, minScrollPixel);
            return this;
        }

        public float maxScrollPixel() {
            return getValueSave(PropertyRegistry.MAX_SCROLL_PIXEL);
        }

        public ScrollerViewStyle maxScrollPixel(float maxScrollPixel) {
            set(PropertyRegistry.MAX_SCROLL_PIXEL, maxScrollPixel);
            return this;
        }
    }

    public final UIElement verticalContainer;
    public final UIElement viewPort;
    public final UIElement viewContainer;
    public final Scroller horizontalScroller;
    public final Scroller verticalScroller;

    @Getter
    private final ScrollerViewStyle scrollerViewStyle = new ScrollerViewStyle();
    // runtime
    private float lastPortWidth = 0, lastContainerWidth = 0;
    private float lastPortHeight = 0, lastContainerHeight = 0;

    public ScrollerView() {
        this.verticalContainer = new UIElement().addClass("__scroller_view_vertical-container__");
        this.viewPort = new UIElement().addClass("__scroller_view_view-port__");
        this.viewContainer = new UIElement().addClass("__scroller_view_view-container__");
        this.horizontalScroller = new Scroller.Horizontal().setRange(0, 1f).setClampNormalizedValue(this::horizontalClamp);
        this.verticalScroller = new Scroller.Vertical().setRange(0, 1f).setClampNormalizedValue(this::verticalClamp);
        this.horizontalScroller.addClass("__scroller_view_horizontal-scroller__");
        this.verticalScroller.addClass("__scroller_view_vertical-scroller__");
        this.addEventListener(UIEvents.MOUSE_WHEEL, UIEvent::stopPropagation);

        verticalContainer.layout(layout -> {
            layout.flex(1);
            layout.flexDirection(FlexDirection.ROW);
        }).addChildren(viewPort, verticalScroller);

        viewPort.layout(layout -> {
            layout.flex(1);
            layout.paddingAll(5);
        }).setOverflowVisible(false).style(style -> style.backgroundTexture(Sprites.BORDER));
        viewPort.addEventListener(UIEvents.MOUSE_WHEEL, this::onScrollWheel);
        viewPort.addChild(new UIElement() // we wrap the view container in a new element
                        .layout(layout -> layout.flex(1))
                        .addChild(viewContainer));
        viewPort.addEventListener(UIEvents.LAYOUT_CHANGED, this::onViewPortLayoutChanged);

        viewContainer.addEventListener(UIEvents.LAYOUT_CHANGED, this::onContainerLayoutChanged);

        // scroller
        verticalScroller.setOnValueChanged(this::onVerticalScroll);
        horizontalScroller.setOnValueChanged(this::onHorizontalScroll);
        addChildren(verticalContainer, horizontalScroller);
        internalSetup();
    }

    /// events
    protected void onHorizontalScroll(float value) {
        viewContainer.layout(layout -> {
            layout.left(-value * Math.max(0, getContainerWidth() - viewPort.getContentWidth()));
        });
    }

    protected void onVerticalScroll(float value) {
        viewContainer.layout(layout -> {
            layout.top(-value * Math.max(0, getContainerHeight() - viewPort.getContentHeight()));
        });
    }

    protected void onScrollWheel(UIEvent event) {
        var mode = scrollerViewStyle.mode();
        if (event.deltaY != 0 && (mode == ScrollerMode.VERTICAL || mode == ScrollerMode.BOTH)) {
            verticalScroller.onScrollWheel(event);
        }
        if (event.deltaX != 0 && (mode == ScrollerMode.HORIZONTAL || mode == ScrollerMode.BOTH)) {
            horizontalScroller.onScrollWheel(event);
        } else if (event.deltaY != 0 && mode == ScrollerMode.HORIZONTAL) {
            horizontalScroller.onScrollWheel(event);
        }
    }

    protected float horizontalClamp(float normalizedValue) {
        var containerWidth = getContainerWidth() - viewPort.getContentWidth();
        return Mth.clamp(Mth.abs(normalizedValue),
                scrollerViewStyle.minScrollPixel() / containerWidth,
                scrollerViewStyle.maxScrollPixel() / containerWidth)
                * (normalizedValue > 0 ? 1 : -1);
    }

    protected float verticalClamp(float normalizedValue) {
        var containerHeight = getContainerHeight() - viewPort.getContentHeight();
        return Mth.clamp(Mth.abs(normalizedValue),
                scrollerViewStyle.minScrollPixel() / containerHeight,
                scrollerViewStyle.maxScrollPixel() / containerHeight)
                * (normalizedValue > 0 ? 1 : -1);
    }

    protected void onViewPortLayoutChanged(UIEvent event) {
        updateScrollers();
    }

    protected void onContainerLayoutChanged(UIEvent event) {
        updateScrollers();
    }

    public float getContainerWidth() {
        // cause we are using a flexbox, the width of the view container is not the same as the width of the view port
        // so we need to calculate the width ourselves
        var width = viewContainer.getSizeWidth();
        for (UIElement child : viewContainer.getChildren()) {
            if (child.isDisplayed()) {
                width = Math.max(width, child.getSizeWidth() + child.getTaffyLayout().location().x);
            }
        }
        return width;
    }

    public float getContainerHeight() {
        var height = viewContainer.getSizeHeight();
        for (UIElement child : viewContainer.getChildren()) {
            if (child.isDisplayed()) {
                height = Math.max(height, child.getSizeHeight() + child.getTaffyLayout().location().y);
            }
        }
        return height;
    }

    private void updateScrollers() {
        if (!isDisplayed()) return;   // avoid adaptive broken
        // Ancestor display:none (or first frame before layout) leaves our own size at 0.
        // Reverse-deriving chrome from getSizeHeight() - viewPort.getContentHeight() would
        // collapse to 0 and bake a zero-chrome important override into our height/width.
        if (getSizeWidth() <= 0 || getSizeHeight() <= 0) return;
        var lastContainerWidth = getContainerWidth();
        var lastContainerHeight = getContainerHeight();
        var mode = scrollerViewStyle.mode();
        if (mode == ScrollerMode.HORIZONTAL || mode == ScrollerMode.BOTH) {
            // cause we are using a flexbox, the width of the view container is not the same as the width of the view port
            // so we need to calculate the width ourselves
            var vp = Math.min(1, viewPort.getContentWidth() / lastContainerWidth);
            horizontalScroller.setScrollBarSize(vp * 100);
            horizontalScroller.setDisplay((scrollerViewStyle.horizontalScrollDisplay() == ScrollDisplay.AUTO && vp < 1) || scrollerViewStyle.horizontalScrollDisplay() == ScrollDisplay.ALWAYS);
        } else {
            horizontalScroller.setDisplay(false);
        }

        if (mode == ScrollerMode.VERTICAL || mode == ScrollerMode.BOTH) {
            var hp = Math.min(1, viewPort.getContentHeight() / lastContainerHeight);
            verticalScroller.setScrollBarSize(hp * 100);
            verticalScroller.setDisplay((scrollerViewStyle.verticalScrollDisplay() == ScrollDisplay.AUTO && hp < 1) || scrollerViewStyle.verticalScrollDisplay() == ScrollDisplay.ALWAYS);
        } else {
            verticalScroller.setDisplay(false);
        }

        if (horizontalScroller.getTaffyStyle().style.display == TaffyDisplay.FLEX) {
            horizontalScroller.layout(layout -> Style.importantPipeline(layout, l ->
                    l.marginRight(verticalScroller.isDisplayed() ? scrollerViewStyle.scrollerViewMargin() : 0)));
        }

        var reloadValue = false;
        var lastPortWidth = viewPort.getSizeWidth();
        var lastPortHeight = viewPort.getSizeHeight();
        if (lastPortWidth != this.lastPortWidth || lastPortHeight != this.lastPortHeight) {
            this.lastPortWidth = lastPortWidth;
            this.lastPortHeight = lastPortHeight;
            reloadValue = true;
        }
        if (lastContainerWidth != this.lastContainerWidth || lastContainerHeight != this.lastContainerHeight) {
            this.lastContainerWidth = lastContainerWidth;
            this.lastContainerHeight = lastContainerHeight;
            reloadValue = true;
            if (scrollerViewStyle.adaptiveWidth()) {
                Style.importantPipeline(getLayout(), layout -> layout.width(lastContainerWidth + getSizeWidth() - viewPort.getContentWidth()));
            }
            if (scrollerViewStyle.adaptiveHeight()) {
                Style.importantPipeline(getLayout(), layout -> layout.height(lastContainerHeight + getSizeHeight() - viewPort.getContentHeight()));
            }
        }
        if (reloadValue) {
            onHorizontalScroll(horizontalScroller.value);
            onVerticalScroll(verticalScroller.value);
        }
    }

    /// data
    public ScrollerView scrollerStyle(Consumer<ScrollerViewStyle> style) {
        style.accept(scrollerViewStyle);
        return this;
    }

    /// structure
    public ScrollerView viewContainer(Consumer<UIElement> view) {
        view.accept(viewContainer);
        return this;
    }

    public ScrollerView viewPort(Consumer<UIElement> view) {
        view.accept(viewPort);
        return this;
    }

    public ScrollerView verticalContainer(Consumer<UIElement> view) {
        view.accept(verticalContainer);
        return this;
    }

    public ScrollerView horizontalScroller(Consumer<Scroller> view) {
        view.accept(horizontalScroller);
        return this;
    }

    public ScrollerView verticalScroller(Consumer<Scroller> view) {
        view.accept(verticalScroller);
        return this;
    }

    public boolean hasScrollViewChild(UIElement child) {
        return viewContainer.hasChild(child);
    }

    public ScrollerView addScrollViewChildAt(@Nullable UIElement child, int index) {
        viewContainer.addChildAt(child, index);
        return this;
    }

    public ScrollerView addScrollViewChild(@Nullable UIElement child) {
        viewContainer.addChild(child);
        return this;
    }

    public ScrollerView addScrollViewChildren(UIElement... children) {
        viewContainer.addChildren(children);
        return this;
    }

    public boolean removeScrollViewChild(@Nullable UIElement child) {
        return viewContainer.removeChild(child);
    }

    public void clearAllScrollViewChildren() {
        viewContainer.clearAllChildren();
    }

    @Override
    public void addEditorChild(UIElement child, int index) {
        if (index == -1) {
            addScrollViewChild(child);
        } else {
            addScrollViewChildAt(child, index);
        }
    }
}
