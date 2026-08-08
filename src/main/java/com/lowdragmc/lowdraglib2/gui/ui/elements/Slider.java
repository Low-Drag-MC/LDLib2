package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.w3c.dom.Element;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A value picker along a line: a thin track, the part of it already covered, and a handle you drag.
 * <p>
 * It is the sibling of {@link Scroller} — same value model, same drag handling, same way of exposing
 * everything to XML, the editor and stylesheets. The two differ in what they mean and therefore how
 * they look by default: a scroller represents a viewport inside a larger content, so its bar is a box
 * whose length is proportional to that viewport; a slider represents one value in a range, so its
 * handle has a fixed size and rides a line.
 *
 * @see PropertyRegistry#SLIDER_STEP
 * @see PropertyRegistry#SLIDER_TRACK_SIZE
 * @see PropertyRegistry#SLIDER_HANDLE_SIZE
 */
@ParametersAreNonnullByDefault
public abstract class Slider extends BindableUIElement<Float> {
    @Configurable(name = "SliderStyle")
    public class SliderStyle extends Style {
        private static final Property<?>[] PROPERTIES = new Property[] {
                PropertyRegistry.SLIDER_STEP,
                PropertyRegistry.SLIDER_TRACK_SIZE,
                PropertyRegistry.SLIDER_HANDLE_SIZE,
        };

        public SliderStyle() {
            super(Slider.this);
        }

        public static void init() {
            PropertyRegistry.SLIDER_TRACK_SIZE.addListener(SliderStyle::onPropertyChanged);
            PropertyRegistry.SLIDER_HANDLE_SIZE.addListener(SliderStyle::onPropertyChanged);
        }

        private static <T> void onPropertyChanged(UIElement element, Property<T> property,
                                                  @Nullable T oldValue, @Nullable T newValue) {
            if (element instanceof Slider slider) {
                slider.updateHandlePosition();
            }
        }

        @Override
        protected Property<?>[] getProperties() {
            return PROPERTIES;
        }

        // these three resolve immediately rather than reading the last computed value: the handle is
        // laid out from the constructor and again right after a setter, both of which run before the
        // style engine has recomputed the bag.

        /** How far one arrow key or mouse wheel notch moves the value, as a fraction of the range. */
        public SliderStyle sliderStep(float step) {
            set(PropertyRegistry.SLIDER_STEP, step);
            return this;
        }

        public float sliderStep() {
            return getValueImmediate(PropertyRegistry.SLIDER_STEP);
        }

        /** Thickness of the line, in pixels. */
        public SliderStyle trackSize(float trackSize) {
            set(PropertyRegistry.SLIDER_TRACK_SIZE, Math.max(0, trackSize));
            return this;
        }

        public float trackSize() {
            return getValueImmediate(PropertyRegistry.SLIDER_TRACK_SIZE);
        }

        /** Length of the handle along the track, in pixels. */
        public SliderStyle handleSize(float handleSize) {
            set(PropertyRegistry.SLIDER_HANDLE_SIZE, Math.max(1, handleSize));
            return this;
        }

        public float handleSize() {
            return getValueImmediate(PropertyRegistry.SLIDER_HANDLE_SIZE);
        }
    }

    /** Full width/height of the slider, catches clicks and the wheel anywhere along it. */
    public final UIElement trackContainer;
    /** The line itself. */
    public final UIElement track;
    /** The part of the line between the minimum and the current value. */
    public final UIElement fill;
    public final Button handle;
    @Getter
    private final SliderStyle sliderStyle = new SliderStyle();
    @Getter
    @Configurable(name = "minValue")
    protected float minValue = 0;
    @Getter
    @Configurable(name = "maxValue")
    protected float maxValue = 1;
    @Configurable(name = "value")
    protected float value = 0;
    @Getter @Setter
    @Accessors(chain = true)
    protected Function<Float, Float> clampNormalizedValue = Function.identity();
    // runtime
    @Getter
    protected boolean isDragging = false;

    public Slider() {
        getLayout().alignItems(AlignItems.CENTER);
        this.trackContainer = new UIElement();
        this.track = new UIElement();
        this.fill = new UIElement();
        this.handle = new Button();
        this.trackContainer.addClass("__slider_track_container__");
        this.track.addClass("__slider_track__");
        this.fill.addClass("__slider_fill__");
        this.handle.addClass("__slider_handle__");

        this.trackContainer.layout(layout -> {
            layout.alignSelf(AlignItems.STRETCH);
            layout.setFlexGrow(1);
            // the line is thinner than the container, which is sized for a comfortable hit area.
            // Centering it across the container is the container's job, not ours.
            layout.alignItems(AlignItems.CENTER);
        });
        // the handle has to sit ON the line rather than next to it, which is the one thing flow layout
        // cannot express: it is taken out of the flow and offset along the track by its value.
        this.handle.getLayout().positionType(TaffyPosition.ABSOLUTE);
        this.handle.noText();

        this.handle.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            handle.startDrag(getValue(), null);
            isDragging = true;
            e.stopPropagation();
        });
        this.handle.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onDraggingHandle);
        this.handle.addEventListener(UIEvents.DRAG_END, e -> {
            isDragging = false;
            handle.setButtonState(Button.State.DEFAULT);
        });
        // do not flicker the handle texture while it is being dragged
        this.trackContainer.addEventListener(UIEvents.MOUSE_LEAVE, e -> {
            if (e.target == handle && isDragging) e.stopPropagation();
        }, true);
        this.trackContainer.addEventListener(UIEvents.MOUSE_ENTER, e -> {
            if (e.target == handle && isDragging) e.stopPropagation();
        }, true);
        this.trackContainer.addEventListener(UIEvents.MOUSE_DOWN, this::clickTrackContainer);
        this.trackContainer.addEventListener(UIEvents.MOUSE_WHEEL, this::onScrollWheel);

        this.track.addChild(fill);
        this.trackContainer.addChildren(track, handle);
        addChild(trackContainer);
    }

    public Slider sliderStyle(Consumer<SliderStyle> style) {
        style.accept(sliderStyle);
        return this;
    }

    /** Moves the value by the given fraction of the range. */
    public void slideValue(float normalizedDelta) {
        setNormalizedValue(getNormalizedValue() + clampNormalizedValue.apply(normalizedDelta));
    }

    @ConfigSetter(field = "minValue")
    public Slider setMinValue(float minValue) {
        return setRange(minValue, maxValue);
    }

    @ConfigSetter(field = "maxValue")
    public Slider setMaxValue(float maxValue) {
        return setRange(minValue, maxValue);
    }

    public Slider setRange(float minValue, float maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        return setValue(value);
    }

    @ConfigSetter(field = "value")
    private void setValueEditor(float value) {
        setValue(value);
    }

    public Slider setValue(@Nullable Float value, boolean notifyChange) {
        if (value == null) value = 0f;
        var newValue = Math.max(minValue, Math.min(maxValue, value));
        if (newValue != this.value) {
            this.value = newValue;
            updateHandlePosition();
            if (notifyChange) {
                notifyListeners();
            }
        }
        return this;
    }

    public Slider setValue(Float value) {
        return setValue(value, true);
    }

    public Float getValue() {
        return value;
    }

    public Slider setOnValueChanged(FloatConsumer onValueChanged) {
        registerValueListener(v -> onValueChanged.accept(v.floatValue()));
        return this;
    }

    public Slider setNormalizedValue(float normalizedValue, boolean notifyChange) {
        return setValue(minValue + (maxValue - minValue) * normalizedValue, notifyChange);
    }

    public Slider setNormalizedValue(float normalizedValue) {
        return setNormalizedValue(normalizedValue, true);
    }

    public float getNormalizedValue() {
        return maxValue == minValue ? 0 : (value - minValue) / (maxValue - minValue);
    }

    protected abstract void updateHandlePosition();

    protected abstract void onDraggingHandle(UIEvent event);

    protected abstract void clickTrackContainer(UIEvent event);

    protected abstract void onScrollWheel(UIEvent event);

    public Slider trackContainer(Consumer<UIElement> container) {
        container.accept(trackContainer);
        return this;
    }

    public Slider track(Consumer<UIElement> track) {
        track.accept(this.track);
        return this;
    }

    public Slider fill(Consumer<UIElement> fill) {
        fill.accept(this.fill);
        return this;
    }

    public Slider handle(Consumer<Button> handle) {
        handle.accept(this.handle);
        return this;
    }

    @KJSBindings("SliderHorizontal")
    @LDLRegister(name = "slider-horizontal", group = "utils", registry = "ldlib2:ui_element")
    public static class Horizontal extends Slider {
        public Horizontal() {
            getLayout().flexDirection(FlexDirection.ROW);
            getLayout().height(7);

            trackContainer.getLayout().flexDirection(FlexDirection.ROW);
            track.getLayout().widthPercent(100);
            track.style(style -> style.backgroundTexture(ColorPattern.GRAY.rectTexture()));
            fill.getLayout().heightPercent(100);
            fill.style(style -> style.backgroundTexture(ColorPattern.LIGHT_GRAY.rectTexture()));
            handle.getLayout().heightPercent(100);
            handle.buttonStyle(style -> style
                    .baseTexture(ColorPattern.LIGHT_GRAY.rectTexture())
                    .hoverTexture(ColorPattern.WHITE.rectTexture())
                    .pressedTexture(ColorPattern.WHITE.rectTexture()));
            updateHandlePosition();
            internalSetup();
        }

        @Override
        protected void updateHandlePosition() {
            var handleSize = getSliderStyle().handleSize();
            var progress = getNormalizedValue();
            Style.importantPipeline(track.getLayout(), layout -> layout.height(getSliderStyle().trackSize()));
            Style.importantPipeline(fill.getLayout(), layout -> layout.widthPercent(progress * 100));
            Style.importantPipeline(handle.getLayout(), layout -> {
                layout.width(handleSize);
                layout.leftPercent(progress * 100);
                // left is the handle's edge, the value belongs under its middle
                layout.marginLeft(-handleSize / 2);
            });
        }

        @Override
        protected void onDraggingHandle(UIEvent event) {
            if (event.dragHandler.draggingObject instanceof Float initialValue) {
                var remainingSpace = trackContainer.getContentWidth() - handle.getSizeWidth();
                if (remainingSpace <= 0) return;
                var localMouse = getLocalMouse(event.x, event.y);
                var localStart = getLocalMouse(event.dragStartX, event.dragStartY);
                var distValue = ((localMouse.x - localStart.x) / remainingSpace) * (maxValue - minValue);
                setValue(distValue + initialValue);
            }
        }

        @Override
        protected void clickTrackContainer(UIEvent event) {
            if (event.button != 0) return;
            var width = trackContainer.getContentWidth();
            if (width <= 0) return;
            setNormalizedValue((getLocalMouse(event.x, event.y).x - trackContainer.getContentX()) / width);
            // keep receiving updates so a click can be dragged straight into a slide
            handle.startDrag(getValue(), null);
            isDragging = true;
        }

        @Override
        protected void onScrollWheel(UIEvent event) {
            var step = getSliderStyle().sliderStep();
            if (event.deltaX != 0) slideValue(event.deltaX > 0 ? step : -step);
            else if (event.deltaY != 0) slideValue(event.deltaY > 0 ? -step : step);
        }
    }

    @KJSBindings("SliderVertical")
    @LDLRegister(name = "slider-vertical", group = "utils", registry = "ldlib2:ui_element")
    public static class Vertical extends Slider {
        public Vertical() {
            getLayout().flexDirection(FlexDirection.COLUMN);
            getLayout().width(7);

            trackContainer.getLayout().flexDirection(FlexDirection.COLUMN);
            track.getLayout().heightPercent(100);
            track.style(style -> style.backgroundTexture(ColorPattern.GRAY.rectTexture()));
            fill.getLayout().widthPercent(100);
            fill.style(style -> style.backgroundTexture(ColorPattern.LIGHT_GRAY.rectTexture()));
            handle.getLayout().widthPercent(100);
            handle.buttonStyle(style -> style
                    .baseTexture(ColorPattern.LIGHT_GRAY.rectTexture())
                    .hoverTexture(ColorPattern.WHITE.rectTexture())
                    .pressedTexture(ColorPattern.WHITE.rectTexture()));
            updateHandlePosition();
            internalSetup();
        }

        @Override
        protected void updateHandlePosition() {
            var handleSize = getSliderStyle().handleSize();
            var progress = getNormalizedValue();
            Style.importantPipeline(track.getLayout(), layout -> layout.width(getSliderStyle().trackSize()));
            Style.importantPipeline(fill.getLayout(), layout -> layout.heightPercent(progress * 100));
            Style.importantPipeline(handle.getLayout(), layout -> {
                layout.height(handleSize);
                layout.topPercent(progress * 100);
                // top is the handle's edge, the value belongs under its middle
                layout.marginTop(-handleSize / 2);
            });
        }

        @Override
        protected void onDraggingHandle(UIEvent event) {
            if (event.dragHandler.draggingObject instanceof Float initialValue) {
                var remainingSpace = trackContainer.getContentHeight() - handle.getSizeHeight();
                if (remainingSpace <= 0) return;
                var localMouse = getLocalMouse(event.x, event.y);
                var localStart = getLocalMouse(event.dragStartX, event.dragStartY);
                var distValue = ((localMouse.y - localStart.y) / remainingSpace) * (maxValue - minValue);
                setValue(distValue + initialValue);
            }
        }

        @Override
        protected void clickTrackContainer(UIEvent event) {
            if (event.button != 0) return;
            var height = trackContainer.getContentHeight();
            if (height <= 0) return;
            setNormalizedValue((getLocalMouse(event.x, event.y).y - trackContainer.getContentY()) / height);
            handle.startDrag(getValue(), null);
            isDragging = true;
        }

        @Override
        protected void onScrollWheel(UIEvent event) {
            var step = getSliderStyle().sliderStep();
            if (event.deltaY != 0) slideValue(event.deltaY > 0 ? -step : step);
        }
    }

    /// Editor + Xml
    @Override
    public void loadXml(Element element) {
        if (element.hasAttribute("min-value")) {
            setMinValue(XmlUtils.getAsFloat(element, "min-value", minValue));
        }
        if (element.hasAttribute("max-value")) {
            setMaxValue(XmlUtils.getAsFloat(element, "max-value", maxValue));
        }
        if (element.hasAttribute("value")) {
            setValue(XmlUtils.getAsFloat(element, "value", value));
        }
        super.loadXml(element);
    }
}
