package com.lowdragmc.lowdraglib.gui.ui.elements;

import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib.gui.ui.style.Style;
import com.lowdragmc.lowdraglib.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib.gui.ui.styletemplate.Sprites;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import org.appliedenergistics.yoga.*;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
public class ScrollerView extends UIElement {
    public enum Mode {
        HORIZONTAL,
        VERTICAL,
        BOTH
    }
    public enum ScrollDisplay {
        AUTO,
        ALWAYS,
        NEVER
    }
    @Accessors(chain = true, fluent = true)
    public static class ScrollerViewStyle extends Style {
        @Getter @Setter
        private float horizontalScrollerMargin = 5;
        @Getter @Setter
        private Mode mode = Mode.BOTH;
        @Getter @Setter
        private ScrollDisplay verticalScrollDisplay = ScrollDisplay.AUTO;
        @Getter @Setter
        private ScrollDisplay horizontalScrollDisplay = ScrollDisplay.AUTO;
        @Getter @Setter
        private boolean adaptiveWidth = false; // enable it to make the scroller width adaptive to the view container
        @Getter @Setter
        private boolean adaptiveHeight = false; // enable it to make the scroller height adaptive to the view container

        public ScrollerViewStyle(UIElement holder) {
            super(holder);
        }
    }
    public final UIElement verticalContainer;
    public final UIElement viewPort;
    public final UIElement viewContainer;
    public final Scroller horizontalScroller;
    public final Scroller verticalScroller;

    @Getter
    private final ScrollerViewStyle scrollerViewStyle = new ScrollerViewStyle(this);
    // runtime
    private float lastPortWidth = 0, lastContainerWidth = 0;
    private float lastPortHeight = 0, lastContainerHeight = 0;

    public ScrollerView() {
        this.verticalContainer = new UIElement();
        this.viewPort = new UIElement().setId("viewPort");
        this.viewContainer = new UIElement().setId("viewContainer");
        this.horizontalScroller = new Scroller.Horizontal().setRange(0, 1f);
        this.verticalScroller = new Scroller.Vertical().setRange(0, 1f);
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
        if (event.deltaY != 0 && (scrollerViewStyle.mode == Mode.VERTICAL || scrollerViewStyle.mode == Mode.BOTH)) {
            verticalScroller.onScrollWheel(event);
        }
        if (event.deltaX != 0 && (scrollerViewStyle.mode == Mode.HORIZONTAL || scrollerViewStyle.mode == Mode.BOTH)) {
            horizontalScroller.onScrollWheel(event);
        } else if (event.deltaY != 0 && scrollerViewStyle.mode == Mode.HORIZONTAL) {
            horizontalScroller.onScrollWheel(event);
        }
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
        return viewContainer.getSizeHeight();
    }

    private void updateScrollers() {
        var lastContainerWidth = getContainerWidth();
        var lastContainerHeight = getContainerHeight();
        if (scrollerViewStyle.mode == Mode.HORIZONTAL || scrollerViewStyle.mode == Mode.BOTH) {
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

        if (scrollerViewStyle.mode == Mode.VERTICAL || scrollerViewStyle.mode == Mode.BOTH) {
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

    @Override
    public void applyStyle(Map<String, StyleValue<?>> values) {
        super.applyStyle(values);
        scrollerViewStyle.applyStyles(values);
    }
}
