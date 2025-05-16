package com.lowdragmc.lowdraglib.gui.ui.elements;

import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib.gui.ui.style.Style;
import com.lowdragmc.lowdraglib.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
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
    private Mode mode = Mode.BOTH;
    @Getter
    private ScrollDisplay verticalScrollDisplay = ScrollDisplay.AUTO;
    @Getter
    private ScrollDisplay horizontalScrollDisplay = ScrollDisplay.AUTO;

    @Getter
    private final ScrollerViewStyle scrollerViewStyle = new ScrollerViewStyle(this);

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


        viewContainer.layout(layout -> layout.setPositionType(YogaPositionType.ABSOLUTE));
        viewContainer.addEventListener(UIEvents.LAYOUT_CHANGED, this::onContainerLayoutChanged);

        // scroller
        verticalScroller.setOnValueChanged(this::onVerticalScroll);
        horizontalScroller.setOnValueChanged(this::onHorizontalScroll);
        addChildren(verticalContainer, horizontalScroller);
    }

    /// events
    protected void onHorizontalScroll(float value) {
        viewContainer.layout(layout -> {
            layout.setPosition(YogaEdge.LEFT, -value * Math.max(0, viewContainer.getSizeWidth() - viewPort.getContentWidth()));
        });
    }

    protected void onVerticalScroll(float value) {
        viewContainer.layout(layout -> {
            layout.setPosition(YogaEdge.TOP, -value * Math.max(0, viewContainer.getSizeHeight() - viewPort.getContentHeight()));
        });
    }

    protected void onScrollWheel(UIEvent event) {
        if (event.deltaY != 0 && (mode == Mode.VERTICAL || mode == Mode.BOTH)) {
            verticalScroller.onScrollWheel(event);
        }
        if (event.deltaX != 0 && (mode == Mode.HORIZONTAL || mode == Mode.BOTH)) {
            horizontalScroller.onScrollWheel(event);
        } else if (event.deltaY != 0 && mode == Mode.HORIZONTAL) {
            horizontalScroller.onScrollWheel(event);
        }
    }

    protected void onContainerLayoutChanged(UIEvent event) {
        updateScrollers();
    }

    private void updateScrollers() {
        if (mode == Mode.HORIZONTAL || mode == Mode.BOTH) {
            var width = viewContainer.getSizeWidth();
            var vp = Math.min(1, viewPort.getContentWidth() / width);
            horizontalScroller.setScrollBarSize(vp * 100);
            if ((horizontalScrollDisplay == ScrollDisplay.AUTO && vp < 1) || horizontalScrollDisplay == ScrollDisplay.ALWAYS) {
                horizontalScroller.setDisplay(YogaDisplay.FLEX);

            } else {
                horizontalScroller.setDisplay(YogaDisplay.NONE);
            }
        } else {
            horizontalScroller.setDisplay(YogaDisplay.NONE);
        }

        if (mode == Mode.VERTICAL || mode == Mode.BOTH) {
            var height = viewContainer.getSizeHeight();
            var hp = Math.min(1, viewPort.getContentHeight() / height);
            verticalScroller.setScrollBarSize(hp * 100);
            if ((verticalScrollDisplay == ScrollDisplay.AUTO && hp < 1) || verticalScrollDisplay == ScrollDisplay.ALWAYS) {
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
    }

    /// data
    public ScrollerView scrollerStyle(Consumer<ScrollerViewStyle> style) {
        style.accept(scrollerViewStyle);
        onStyleChanged();
        return this;
    }

    public ScrollerView setMode(Mode mode) {
        this.mode = mode;
        updateScrollers();
        return this;
    }

    public ScrollerView setVerticalScrollDisplay(ScrollDisplay display) {
        this.verticalScrollDisplay = display;
        updateScrollers();
        return this;
    }

    public ScrollerView setHorizontalScrollDisplay(ScrollDisplay display) {
        this.horizontalScrollDisplay = display;
        updateScrollers();
        return this;
    }

    /// structure
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
