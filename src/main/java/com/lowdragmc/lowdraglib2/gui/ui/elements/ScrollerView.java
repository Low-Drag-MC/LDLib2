package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleHandler;
import com.lowdragmc.lowdraglib2.gui.ui.style.UIStyleRegistries;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.BoolValue;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.EnumValue;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.FloatValue;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import org.appliedenergistics.yoga.*;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
@LDLRegister(name = "scroller_view", registry = "ldlib2:ui_element")
public class ScrollerView extends UIElement {
    @Accessors(chain = true, fluent = true)
    public static class ScrollerViewStyle extends Style {
        @Getter @Setter
        @Configurable(name = "ScrollerView.margin")
        @ConfigNumber(range = {-Float.MAX_VALUE, Float.MAX_VALUE})
        private float horizontalScrollerMargin = 5;
        @Getter @Setter
        @Configurable(name = "ScrollerView.mode")
        private ScrollerMode mode = ScrollerMode.BOTH;
        @Getter @Setter
        @Configurable(name = "ScrollerView.verticalScrollDisplay")
        private ScrollDisplay verticalScrollDisplay = ScrollDisplay.AUTO;
        @Getter @Setter
        @Configurable(name = "ScrollerView.horizontalScrollDisplay")
        private ScrollDisplay horizontalScrollDisplay = ScrollDisplay.AUTO;
        @Getter @Setter
        @Configurable(name = "ScrollerView.adaptiveWidth", tips = "ScrollerView.adaptiveWidth.tips")
        private boolean adaptiveWidth = false;
        @Getter @Setter
        @Configurable(name = "ScrollerView.adaptiveHeight", tips = "ScrollerView.adaptiveHeight.tips")
        private boolean adaptiveHeight = false;
        @Getter @Setter
        @Configurable(name = "ScrollerView.minScrollPixel")
        @ConfigNumber(range = {0, Float.MAX_VALUE})
        private float minScrollPixel = 5;
        @Getter @Setter
        @Configurable(name = "ScrollerView.maxScrollPixel")
        @ConfigNumber(range = {0, Float.MAX_VALUE})
        private float maxScrollPixel = 7;

        public ScrollerViewStyle(UIElement holder) {
            super(holder);
        }

        @Override
        public void applyStyles(Map<String, StyleValue<?>> values) {
            super.applyStyles(values);

            UIStyleRegistries.SCROLLER_VIEW_MARGIN.parse(values).ifPresent(this::horizontalScrollerMargin);
            UIStyleRegistries.SCROLLER_VIEW_MODE.parse(values).ifPresent(this::mode);
            UIStyleRegistries.VERTICAL_DISPLAY.parse(values).ifPresent(this::verticalScrollDisplay);
            UIStyleRegistries.HORIZONTAL_DISPLAY.parse(values).ifPresent(this::horizontalScrollDisplay);
            UIStyleRegistries.ADAPTIVE_WIDTH.parse(values).ifPresent(this::adaptiveWidth);
            UIStyleRegistries.ADAPTIVE_HEIGHT.parse(values).ifPresent(this::adaptiveHeight);
            UIStyleRegistries.MIN_SCROLL_PIXEL.parse(values).ifPresent(this::minScrollPixel);
            UIStyleRegistries.MAX_SCROLL_PIXEL.parse(values).ifPresent(this::maxScrollPixel);

            if (holder instanceof ScrollerView scrollerView) {
                scrollerView.updateScrollers();
            }
        }
    }
    public final UIElement verticalContainer;
    public final UIElement viewPort;
    public final UIElement viewContainer;
    public final Scroller horizontalScroller;
    public final Scroller verticalScroller;

    @Getter
    @Configurable(name = "scrollerViewStyle", subConfigurable = true)
    private final ScrollerViewStyle scrollerViewStyle = new ScrollerViewStyle(this);
    // runtime
    private float lastPortWidth = 0, lastContainerWidth = 0;
    private float lastPortHeight = 0, lastContainerHeight = 0;

    public ScrollerView() {
        this.verticalContainer = new UIElement();
        this.viewPort = new UIElement().setId("viewPort");
        this.viewContainer = new UIElement().setId("viewContainer");
        this.horizontalScroller = new Scroller.Horizontal().setRange(0, 1f).setClampNormalizedValue(this::horizontalClamp);
        this.verticalScroller = new Scroller.Vertical().setRange(0, 1f).setClampNormalizedValue(this::verticalClamp);
        this.addEventListener(UIEvents.MOUSE_WHEEL, UIEvent::stopPropagation);

        verticalContainer.layout(layout -> {
            layout.setFlex(1);
            layout.setFlexDirection(YogaFlexDirection.ROW);
        }).addChildren(viewPort, verticalScroller);

        viewPort.layout(layout -> {
            layout.setFlex(1);
            layout.setPadding(YogaEdge.ALL, 5);
        }).setOverflow(YogaOverflow.HIDDEN).style(style -> style.backgroundTexture(Sprites.BORDER));
        viewPort.addEventListener(UIEvents.MOUSE_WHEEL, this::onScrollWheel);
        viewPort.addChild(new UIElement() // we wrap the view container in a new element
                        .layout(layout -> layout.setFlex(1))
                        .addChild(viewContainer));

        viewContainer.addEventListener(UIEvents.LAYOUT_CHANGED, this::onContainerLayoutChanged);

        // scroller
        verticalScroller.setOnValueChanged(this::onVerticalScroll);
        horizontalScroller.setOnValueChanged(this::onHorizontalScroll);
        addChildren(verticalContainer, horizontalScroller);
        markAllChildrenAsInternal();
    }

    /// events
    protected void onHorizontalScroll(float value) {
        viewContainer.layout(layout -> {
            layout.setPosition(YogaEdge.LEFT, -value * Math.max(0, getContainerWidth() - viewPort.getContentWidth()));
        });
    }

    protected void onVerticalScroll(float value) {
        viewContainer.layout(layout -> {
            layout.setPosition(YogaEdge.TOP, -value * Math.max(0, getContainerHeight() - viewPort.getContentHeight()));
        });
    }

    protected void onScrollWheel(UIEvent event) {
        if (event.deltaY != 0 && (scrollerViewStyle.mode == ScrollerMode.VERTICAL || scrollerViewStyle.mode == ScrollerMode.BOTH)) {
            verticalScroller.onScrollWheel(event);
        }
        if (event.deltaX != 0 && (scrollerViewStyle.mode == ScrollerMode.HORIZONTAL || scrollerViewStyle.mode == ScrollerMode.BOTH)) {
            horizontalScroller.onScrollWheel(event);
        } else if (event.deltaY != 0 && scrollerViewStyle.mode == ScrollerMode.HORIZONTAL) {
            horizontalScroller.onScrollWheel(event);
        }
    }

    protected float horizontalClamp(float normalizedValue) {
        var containerWidth = getContainerWidth() - viewPort.getContentWidth();
        return Mth.clamp(Mth.abs(normalizedValue),
                scrollerViewStyle.minScrollPixel / containerWidth,
                scrollerViewStyle.maxScrollPixel / containerWidth)
                * (normalizedValue > 0 ? 1 : -1);
    }

    protected float verticalClamp(float normalizedValue) {
        var containerHeight = getContainerHeight() - viewPort.getContentHeight();
        return Mth.clamp(Mth.abs(normalizedValue),
                scrollerViewStyle.minScrollPixel / containerHeight,
                scrollerViewStyle.maxScrollPixel / containerHeight)
                * (normalizedValue > 0 ? 1 : -1);
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
                width = Math.max(width, child.getSizeWidth() + child.getLayoutNode().getLayoutX());
            }
        }
        return width;
    }

    public float getContainerHeight() {
        var height = viewContainer.getSizeHeight();
        for (UIElement child : viewContainer.getChildren()) {
            if (child.isDisplayed()) {
                height = Math.max(height, child.getSizeHeight() + child.getLayoutNode().getLayoutY());
            }
        }
        return height;
    }

    private void updateScrollers() {
        var lastContainerWidth = getContainerWidth();
        var lastContainerHeight = getContainerHeight();
        if (scrollerViewStyle.mode == ScrollerMode.HORIZONTAL || scrollerViewStyle.mode == ScrollerMode.BOTH) {
            // cause we are using a flexbox, the width of the view container is not the same as the width of the view port
            // so we need to calculate the width ourselves
            var vp = Math.min(1, viewPort.getContentWidth() / lastContainerWidth);
            horizontalScroller.setScrollBarSize(vp * 100);
            if ((scrollerViewStyle.horizontalScrollDisplay == ScrollDisplay.AUTO && vp < 1) || scrollerViewStyle.horizontalScrollDisplay == ScrollDisplay.ALWAYS) {
                horizontalScroller.setDisplay(YogaDisplay.FLEX);

            } else {
                horizontalScroller.setDisplay(YogaDisplay.NONE);
            }
        } else {
            horizontalScroller.setDisplay(YogaDisplay.NONE);
        }

        if (scrollerViewStyle.mode == ScrollerMode.VERTICAL || scrollerViewStyle.mode == ScrollerMode.BOTH) {
            var hp = Math.min(1, viewPort.getContentHeight() / lastContainerHeight);
            verticalScroller.setScrollBarSize(hp * 100);
            if ((scrollerViewStyle.verticalScrollDisplay == ScrollDisplay.AUTO && hp < 1) || scrollerViewStyle.verticalScrollDisplay == ScrollDisplay.ALWAYS) {
                verticalScroller.setDisplay(YogaDisplay.FLEX);
            } else {
                verticalScroller.setDisplay(YogaDisplay.NONE);
            }
        } else {
            verticalScroller.setDisplay(YogaDisplay.NONE);
        }

        if (horizontalScroller.getLayoutNode().getDisplay() == YogaDisplay.FLEX) {
            horizontalScroller.layout(layout -> {
                layout.setMargin(YogaEdge.RIGHT, verticalScroller.getLayoutNode().getDisplay() == YogaDisplay.FLEX ? scrollerViewStyle.horizontalScrollerMargin : 0);
            });
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
            if (scrollerViewStyle.adaptiveWidth) {
                getLayout().setWidth(lastContainerWidth + getSizeWidth() - viewPort.getContentWidth());
            }
            if (scrollerViewStyle.adaptiveHeight) {
                getLayout().setHeight(lastContainerHeight + getSizeHeight() - viewPort.getContentHeight());
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
        onStyleChanged();
        updateScrollers();
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

}
