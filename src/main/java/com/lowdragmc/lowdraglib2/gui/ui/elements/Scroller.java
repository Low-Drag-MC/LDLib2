package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.editor_outdated.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.BindableUIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaGutter;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class Scroller extends BindableUIElement<Float> {
    @Accessors(chain = true, fluent = true)
    public static class ScrollerStyle extends Style {
        @Getter
        @Setter
        private float scrollDelta = 0.1f;

        public ScrollerStyle(UIElement holder) {
            super(holder);
        }
    }
    public final Button headButton;
    public final Button tailButton;
    public final UIElement scrollContainer;
    public final Button scrollBar;
    @Getter
    private final ScrollerStyle scrollerStyle = new ScrollerStyle(this);
    @Getter
    protected float minValue = 0;
    @Getter
    protected float maxValue = 1;
    protected float value = 0;
    @Getter
    protected float scrollBarSize = 20; // in percent
    // runtime
    @Getter
    protected boolean isDragging = false;

    public Scroller() {
        getLayout().setAlignItems(YogaAlign.CENTER);
        this.headButton = new Button();
        this.tailButton = new Button();
        this.scrollContainer = new UIElement();
        this.scrollBar = new Button();

        this.headButton.noText().layout(layout -> {
            layout.setWidth(5);
            layout.setHeight(5);
        });
        this.headButton.setOnClick(e -> moveHead());

        this.tailButton.noText().layout(layout -> {
            layout.setWidth(5);
            layout.setHeight(5);
        });
        this.tailButton.setOnClick(e -> moveTail());

        this.scrollContainer.layout(layout -> {
            layout.setAlignSelf(YogaAlign.STRETCH);
            layout.setFlexGrow(1);
        }).addChild(new UIElement().layout(layout -> layout.setFlex(1)).addChild(scrollBar));
        scrollBar.noText().layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
        });
        scrollBar.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            scrollBar.startDrag(getValue(), null);
            isDragging = true;
            e.stopPropagation();
        });
        scrollBar.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onDraggingScrollBar);
        scrollBar.addEventListener(UIEvents.DRAG_END, e -> {
            isDragging = false;
            scrollBar.setButtonState(Button.State.DEFAULT);
        });
        // do not modify the scroll bar texture during dragging
        scrollContainer.addEventListener(UIEvents.MOUSE_LEAVE, e -> {
            if (e.target == scrollBar && isDragging) e.stopPropagation();
        }, true);
        scrollContainer.addEventListener(UIEvents.MOUSE_ENTER, e -> {
            if (e.target == scrollBar && isDragging) e.stopPropagation();
        }, true);
        scrollContainer.addEventListener(UIEvents.MOUSE_DOWN, this::clickScrollContainer);
        addChildren(headButton, scrollContainer, tailButton);
        scrollContainer.addEventListener(UIEvents.MOUSE_WHEEL, this::onScrollWheel);
    }

    public Scroller scrollerStyle(Consumer<ScrollerStyle> style) {
        style.accept(scrollerStyle);
        onStyleChanged();
        return this;
    }

    @Override
    public void applyStyle(Map<String, StyleValue<?>> values) {
        super.applyStyle(values);
        scrollerStyle.applyStyles(values);
    }

    public void scrollValue(float normalizedValue) {
        setNormalizedValue(getNormalizedValue() + normalizedValue);
    }

    /**
     * Set the range of the scroller.
     */
    public Scroller setRange(float minValue, float maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        return setValue(value);
    }

    /**
     * Set the value of the scroller.
     */
    public Scroller setValue(Float value, boolean notifyChange) {
        var newValue = Math.max(minValue, Math.min(maxValue, value));
        if (newValue != this.value) {
            this.value = newValue;
            updateScrollBarPosition();
            if (notifyChange) {
                notifyListeners();
            }
        }
        return this;
    }

    public Scroller setOnValueChanged(FloatConsumer onValueChanged) {
        registerValueListener(v -> onValueChanged.accept(v.floatValue()));
        return this;
    }

    public Scroller setValue(Float value) {
        return setValue(value, true);
    }

    public Float getValue() {
        return value;
    }

    public Scroller setNormalizedValue(float normalizedValue, boolean notifyChange) {
        return setValue(minValue + (maxValue - minValue) * normalizedValue, notifyChange);
    }

    public Scroller setNormalizedValue(float normalizedValue) {
        return setNormalizedValue(normalizedValue, true);
    }

    /**
     * Set the size of the scroll bar in percent.
     * @param size the size of the scroll bar in percent (0-100)
     */
    public Scroller setScrollBarSize(float size) {
        var newSize = Math.max(0, Math.min(100, size));
        if (newSize != this.scrollBarSize) {
            this.scrollBarSize = newSize;
            updateScrollBarPosition();
        }
        return this;
    }

    private void moveHead() {
        var newValue = value - (maxValue - minValue) * scrollerStyle.scrollDelta;
        setValue(newValue);
    }

    private void moveTail() {
        var newValue = value + (maxValue - minValue) * scrollerStyle.scrollDelta;
        setValue(newValue);
    }

    protected abstract void updateScrollBarPosition();

    protected abstract void onDraggingScrollBar(UIEvent event);

    protected abstract void clickScrollContainer(UIEvent event);

    protected abstract void onScrollWheel(UIEvent event);

    public float getNormalizedValue() {
        return maxValue == minValue ? Float.NaN : (value - minValue) / (maxValue - minValue);
    }

    public Scroller headButton(Consumer<Button> button) {
        button.accept(headButton);
        return this;
    }

    public Scroller tailButton(Consumer<Button> button) {
        button.accept(tailButton);
        return this;
    }

    public Scroller scrollContainer(Consumer<UIElement> container) {
        container.accept(scrollContainer);
        return this;
    }

    public Scroller scrollBar(Consumer<Button> button) {
        button.accept(scrollBar);
        return this;
    }

    public static class Vertical extends Scroller {
        public Vertical() {
            getLayout().setFlexDirection(YogaFlexDirection.COLUMN);
            getLayout().setGap(YogaGutter.ROW, 1);
            getLayout().setWidth(5);

            headButton.buttonStyle(style -> style
                    .defaultTexture(Icons.UP_ARROW_NO_BAR_S)
                    .hoverTexture(Icons.UP_ARROW_NO_BAR_S_LIGHT)
                    .pressedTexture(Icons.UP_ARROW_NO_BAR_S_WHITE)
            );
            tailButton.buttonStyle(style -> style
                    .defaultTexture(Icons.DOWN_ARROW_NO_BAR_S)
                    .hoverTexture(Icons.DOWN_ARROW_NO_BAR_S_LIGHT)
                    .pressedTexture(Icons.DOWN_ARROW_NO_BAR_S_WHITE)
            );
            scrollContainer.style(style -> style.backgroundTexture(Sprites.SCROLL_CONTAINER_V));
            scrollBar.buttonStyle(style -> style
                    .defaultTexture(Sprites.SCROLL_BAR_V)
                    .hoverTexture(Sprites.SCROLL_BAR_LIGHT_V)
                    .pressedTexture(Sprites.SCROLL_BAR_WHITE_V)
            );
            updateScrollBarPosition();
        }

        @Override
        protected void updateScrollBarPosition()  {
            float remainingSpace = 100 - scrollBarSize;
            float position = getNormalizedValue() * remainingSpace;
            scrollBar.layout(layout -> {
                layout.setHeightPercent(scrollBarSize);
                layout.setPositionPercent(YogaEdge.TOP, position);
            });
        }

        @Override
        protected void onDraggingScrollBar(UIEvent event) {
            if (event.dragHandler.draggingObject instanceof Float initialValue) {
                var minY = scrollContainer.getContentY();
                var maxY = scrollContainer.getContentY() + scrollContainer.getContentHeight();

                var remainingSpace = maxY - minY - scrollBar.getSizeHeight();
                var deltaY = event.y - event.dragStartY;
                var distValue = (deltaY / remainingSpace) * (maxValue - minValue);
                var newValue = distValue + initialValue;
                setValue(newValue);
            }
        }

        @Override
        protected void clickScrollContainer(UIEvent event) {
            if (event.button == 0) {
                setValue(minValue + (maxValue - minValue) * (event.y - scrollContainer.getContentY()) / scrollContainer.getContentHeight());
            }
        }

        @Override
        protected void onScrollWheel(UIEvent event) {
            if (event.deltaY != 0) scrollValue(event.deltaY > 0 ? -getScrollerStyle().scrollDelta : getScrollerStyle().scrollDelta);
        }
    }

    public static class Horizontal extends Scroller {
        public Horizontal() {
            getLayout().setFlexDirection(YogaFlexDirection.ROW);
            getLayout().setGap(YogaGutter.COLUMN, 1);
            getLayout().setHeight(5);

            headButton.buttonStyle(style -> style
                    .defaultTexture(Icons.LEFT_ARROW_NO_BAR_S)
                    .hoverTexture(Icons.LEFT_ARROW_NO_BAR_S_LIGHT)
                    .pressedTexture(Icons.LEFT_ARROW_NO_BAR_S_WHITE)
            );
            tailButton.buttonStyle(style -> style
                    .defaultTexture(Icons.RIGHT_ARROW_NO_BAR_S)
                    .hoverTexture(Icons.RIGHT_ARROW_NO_BAR_S_LIGHT)
                    .pressedTexture(Icons.RIGHT_ARROW_NO_BAR_S_WHITE)
            );
            scrollContainer.style(style -> style.backgroundTexture(Sprites.SCROLL_CONTAINER_H));
            scrollBar.buttonStyle(style -> style
                    .defaultTexture(Sprites.SCROLL_BAR_H)
                    .hoverTexture(Sprites.SCROLL_BAR_LIGHT_H)
                    .pressedTexture(Sprites.SCROLL_BAR_WHITE_H)
            );
            updateScrollBarPosition();
        }

        @Override
        protected void updateScrollBarPosition() {
            float remainingSpace = 100 - scrollBarSize;
            float position = getNormalizedValue() * remainingSpace;

            scrollBar.layout(layout -> {
                layout.setWidthPercent(scrollBarSize);
                layout.setPositionPercent(YogaEdge.LEFT, position);
            });
        }

        @Override
        protected void onDraggingScrollBar(UIEvent event) {
            if (event.dragHandler.draggingObject instanceof Float initialValue) {
                var minX = scrollContainer.getContentX();
                var maxX = scrollContainer.getContentX() + scrollContainer.getContentWidth();

                var remainingSpace = maxX - minX - scrollBar.getSizeWidth();
                var deltaX = event.x - event.dragStartX;
                var distValue = (deltaX / remainingSpace) * (maxValue - minValue);
                var newValue = distValue + initialValue;
                setValue(newValue);
            }
        }

        @Override
        protected void clickScrollContainer(UIEvent event) {
            if (event.button == 0) {
                setValue(minValue + (maxValue - minValue) * (event.x - scrollContainer.getContentX()) / scrollContainer.getContentWidth());
            }
        }

        @Override
        protected void onScrollWheel(UIEvent event) {
            var delta = getScrollerStyle().scrollDelta;
            if (event.deltaX != 0) scrollValue(event.deltaX > 0 ? -delta : delta);
            else if (event.deltaY != 0) scrollValue(event.deltaY > 0 ? -delta : delta);
        }
    }
}
