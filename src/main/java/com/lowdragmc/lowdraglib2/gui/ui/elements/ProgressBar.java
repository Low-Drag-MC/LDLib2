package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IBindable;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IDataConsumer;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IDataProvider;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import com.lowdragmc.lowdraglib2.gui.util.ITickable;
import net.minecraft.util.Mth;
import org.appliedenergistics.yoga.YogaEdge;
import org.w3c.dom.Element;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
@KJSBindings
@LDLRegister(name = "progress-bar", group = "basic", registry = "ldlib2:ui_element")
public class ProgressBar extends UIElement implements IBindable<Float>, IDataConsumer<Float> {
    @Configurable(name = "ProgressBarStyle")
    public class ProgressBarStyle extends Style {
        private static final Property<?>[] PROPERTIES = new Property[] {
                PropertyRegistry.FILL_DIRECTION,
                PropertyRegistry.INTERPOLATE,
                PropertyRegistry.INTERPOLATE_STEP,
        };

        public static void init() {
            PropertyRegistry.FILL_DIRECTION.addListener(ProgressBarStyle::onPropertyChanged);
        }

        public ProgressBarStyle() {
            super(ProgressBar.this);
            setDefault(PropertyRegistry.FILL_DIRECTION, FillDirection.LEFT_TO_RIGHT);
        }

        private static <T> void onPropertyChanged(UIElement element, Property<T> property, @Nullable T oldValue, @Nullable T newValue) {
            if (element instanceof ProgressBar progressBar) {
                progressBar.onProgressBarStyleChanged();
            }
        }

        @Override
        protected Property<?>[] getProperties() {
            return PROPERTIES;
        }

        public ProgressBarStyle fillDirection(FillDirection fillDirection) {
            set(PropertyRegistry.FILL_DIRECTION, fillDirection);
            return this;
        }

        public FillDirection fillDirection() {
            return getValueSave(PropertyRegistry.FILL_DIRECTION);
        }

        public ProgressBarStyle interpolate(boolean interpolate) {
            set(PropertyRegistry.INTERPOLATE, interpolate);
            return this;
        }

        public boolean interpolate() {
            return getValueSave(PropertyRegistry.INTERPOLATE);
        }

        public ProgressBarStyle interpolateStep(float interpolateStep) {
            set(PropertyRegistry.INTERPOLATE_STEP, interpolateStep);
            return this;
        }

        public float interpolateStep() {
            return getValueSave(PropertyRegistry.INTERPOLATE_STEP);
        }
    }

    public final UIElement barContainer;
    public final UIElement barBackground;
    public final Label label;
    public final UIElement bar;
    @Getter
    private final ProgressBarStyle progressBarStyle = new ProgressBarStyle();
    @Getter
    @Configurable(name = "minValue")
    private float minValue = 0;
    @Getter
    @Configurable(name = "maxValue")
    private float maxValue = 1;
    @Configurable(name = "value")
    private float value = 0;
    // runtime
    protected final Map<IDataProvider<Float>, ISubscription> dataSources = new LinkedHashMap<>();
    private float lastValue = 0;

    public ProgressBar() {
        getLayout().height(14);

        this.barContainer = new UIElement();
        this.label = new Label();
        this.barBackground = new UIElement();
        this.bar = new UIElement();
        this.barContainer.addClass("__progress-bar_bar-container__");
        this.barBackground.addClass("__progress-bar_bar-background__");
        this.label.addClass("__progress-bar_label__");
        this.bar.addClass("__progress-bar_bar__");

        this.barContainer.layout(layout -> {
            layout.heightPercent(100);
            layout.widthPercent(100);
            layout.paddingAll(4);
        }).style(style -> style.backgroundTexture(Sprites.PROGRESS_CONTAINER));
        this.bar.style(style -> style.backgroundTexture(Sprites.PROGRESS_BAR));
        this.label.textStyle(style -> style
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER))
                .layout(layout -> {
                    layout.heightPercent(100);
                    layout.widthPercent(100);
                    layout.positionType(TaffyPosition.ABSOLUTE);
                });

        this.barContainer.addChildren(barBackground
                        .layout(layout -> {
                            layout.heightPercent(100);
                            layout.widthPercent(100);
                        })
                .addChildren(this.bar, this.label));
        this.addChildren(this.barContainer);
        updateProgressBarStyle(getNormalizedValue());
        internalSetup();
    }

    public ProgressBar progressBarStyle(Consumer<ProgressBarStyle> style) {
        style.accept(this.progressBarStyle);
        return this;
    }

    protected void onProgressBarStyleChanged() {
        lastValue = value;
        updateProgressBarStyle(getNormalizedValue());
    }

    public float getNormalizedValue() {
        return getNormalizedValue(value);
    }

    public float getNormalizedValue(float value) {
        return maxValue == minValue ? Float.NaN : (value - minValue) / (maxValue - minValue);
    }

    protected void updateProgressBarStyle(float normalizedValue) {
        switch (progressBarStyle.fillDirection()) {
            case LEFT_TO_RIGHT -> {
                this.barBackground.layout(layout -> {
                    layout.flexDirection(FlexDirection.COLUMN);
                    layout.alignItems(AlignItems.FLEX_START);
                });
                this.bar.layout(layout -> {
                    layout.flexDirection(FlexDirection.ROW);
                    layout.justifyContent(AlignContent.FLEX_START);
                    layout.heightPercent(100);
                    layout.widthPercent(normalizedValue * 100);
                });
            }
            case RIGHT_TO_LEFT -> {
                this.barBackground.layout(layout -> {
                    layout.flexDirection(FlexDirection.COLUMN);
                    layout.alignItems(AlignItems.FLEX_END);
                });
                this.bar.layout(layout -> {
                    layout.flexDirection(FlexDirection.ROW);
                    layout.justifyContent(AlignContent.FLEX_END);
                    layout.heightPercent(100);
                    layout.widthPercent(normalizedValue * 100);
                });
            }
            case UP_TO_DOWN -> {
                this.barBackground.layout(layout -> {
                    layout.flexDirection(FlexDirection.ROW);
                    layout.alignItems(AlignItems.FLEX_START);
                });
                this.bar.layout(layout -> {
                    layout.flexDirection(FlexDirection.COLUMN);
                    layout.justifyContent(AlignContent.FLEX_START);
                    layout.heightPercent(normalizedValue * 100);
                    layout.widthPercent(100);
                });
            }
            case DOWN_TO_UP -> {
                this.barBackground.layout(layout -> {
                    layout.flexDirection(FlexDirection.ROW);
                    layout.alignItems(AlignItems.FLEX_END);
                });
                this.bar.layout(layout -> {
                    layout.flexDirection(FlexDirection.COLUMN);
                    layout.justifyContent(AlignContent.FLEX_END);
                    layout.heightPercent(normalizedValue * 100);
                    layout.widthPercent(100);
                });
            }
        }
    }

    @ConfigSetter(field = "minValue")
    public ProgressBar setMinValue(float minValue) {
        return setRange(minValue, maxValue);
    }

    @ConfigSetter(field = "maxValue")
    public ProgressBar setMaxValue(float maxValue) {
        return setRange(minValue, maxValue);
    }

    public ProgressBar setRange(float minValue, float maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        setProgress(this.value);
        lastValue = this.value;
        updateProgressBarStyle(getNormalizedValue());
        return this;
    }

    @ConfigSetter(field = "value")
    private void setProgressEditor(float value) {
        setProgress(value);
        onProgressBarStyleChanged();
    }

    public ProgressBar setProgress(float value) {
        return setValue(value);
    }

    @Override
    public ProgressBar bindDataSource(IDataProvider<Float> dataProvider) {
        UIEventListener tickableListener;
        if (dataProvider instanceof ITickable tickable) {
            tickableListener = e -> tickable.tick();
            addEventListener(UIEvents.TICK, tickableListener);
        } else {
            tickableListener = null;
        }
        var subscription = dataProvider.registerListener(this::setProgress, true);
        if (tickableListener != null) {
            subscription.andThen(() -> removeEventListener(UIEvents.TICK, tickableListener));
        }
        this.dataSources.put(dataProvider, subscription);
        return this;
    }

    @Override
    public ProgressBar unbindDataSource(IDataProvider<Float> dataProvider) {
        var removed = this.dataSources.remove(dataProvider);
        if (removed != null) {
            removed.unsubscribe();
        }
        return this;
    }

    @Override
    public Collection<IDataProvider<Float>> getBoundDataSources() {
        return dataSources.keySet();
    }

    @Override
    public ProgressBar setValue(@Nullable Float value) {
        if (value == null) value = 0f;
        var newValue = Math.max(minValue, Math.min(maxValue, value));
        if (newValue != this.value) {
            this.value = newValue;
            if (!progressBarStyle.interpolate()) {
                lastValue = this.value;
            }
            updateProgressBarStyle(getNormalizedValue(lastValue));
        }
        return this;
    }

    @Override
    public Float getValue() {
        return value;
    }

    public ProgressBar label(Consumer<Label> label) {
        label.accept(this.label);
        return this;
    }

    public ProgressBar barContainer(Consumer<UIElement> barContainer) {
        barContainer.accept(this.barContainer);
        return this;
    }

    public ProgressBar bar(Consumer<UIElement> bar) {
        bar.accept(this.bar);
        return this;
    }

    @Override
    public void screenTick() {
        super.screenTick();
        if (lastValue != value) {
            var stepValue = progressBarStyle.interpolateStep() * (maxValue - minValue);
            if (stepValue < 0) {
                // invalid step
                lastValue = value;
            } else {
                if (lastValue < value) {
                    if (lastValue + stepValue < value) {
                        lastValue += stepValue;
                    } else {
                        lastValue = value;
                    }
                } else if (lastValue > value) {
                    if  (lastValue - stepValue > value) {
                        lastValue -= stepValue;
                    } else {
                        lastValue = value;
                    }
                }
            }
            updateProgressBarStyle(getNormalizedValue(lastValue));
        }
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        super.drawBackgroundAdditional(guiContext);
        if (progressBarStyle.interpolate() && lastValue != value) {
            var stepValue = progressBarStyle.interpolateStep() * (maxValue - minValue);
            if (stepValue < 0) {
                updateProgressBarStyle(getNormalizedValue(Mth.lerp(guiContext.partialTick, lastValue, value)));
            } else {
                if (lastValue < value) {
                    if (lastValue + stepValue < value) {
                        updateProgressBarStyle(getNormalizedValue(Mth.lerp(guiContext.partialTick, lastValue, lastValue + stepValue)));
                    } else {
                        updateProgressBarStyle(getNormalizedValue(Mth.lerp(guiContext.partialTick, lastValue, value)));
                    }
                } else if (lastValue > value) {
                    if  (lastValue - stepValue > value) {
                        updateProgressBarStyle(getNormalizedValue(Mth.lerp(guiContext.partialTick, lastValue, lastValue - stepValue)));
                    } else {
                        updateProgressBarStyle(getNormalizedValue(Mth.lerp(guiContext.partialTick, lastValue, value)));
                    }
                }
            }
        }
    }

    /// Editor + Xml
    @Override
    public void loadXml(Element element) {
        // min value
        if (element.hasAttribute("min-value")) {
            setMinValue(XmlUtils.getAsFloat(element, "min-value", minValue));
        }
        // max value
        if (element.hasAttribute("max-value")) {
            setMaxValue(XmlUtils.getAsFloat(element, "max-value", maxValue));
        }
        // value
        if (element.hasAttribute("value")) {
            setValue(XmlUtils.getAsFloat(element, "value", value));
        }
        // text
        if (element.hasAttribute("text")) {
            label.setText(element.getAttribute("text"));
        }
        super.loadXml(element);
    }
}
