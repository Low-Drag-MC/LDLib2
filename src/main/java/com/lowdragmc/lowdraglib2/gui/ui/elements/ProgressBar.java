package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.IBindable;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IDataConsumer;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IDataProvider;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaPositionType;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
public class ProgressBar extends UIElement implements IBindable<Float>, IDataConsumer<Float> {
    @Accessors(chain = true, fluent = true)
    public static class ProgressBarStyle extends Style {
        @Getter @Setter
        private FillDirection fillDirection = FillDirection.LEFT_TO_RIGHT;
        @Getter @Setter
        private boolean interpolate = true;
        @Getter @Setter
        private float interpolateStep = 0.1f;

        public ProgressBarStyle(UIElement holder) {
            super(holder);
        }
    }
    public final UIElement barContainer;
    public final Label label;
    public final UIElement bar;
    @Getter
    private final ProgressBarStyle progressBarStyle = new ProgressBarStyle(this);
    @Getter
    private float minValue = 0;
    @Getter
    private float maxValue = 1;
    private float value = 0;
    // runtime
    protected final Map<IDataProvider<Float>, ISubscription> dataSources = new LinkedHashMap<>();
    private float lastValue = 0;

    public ProgressBar() {
        getLayout().setHeight(14);

        this.barContainer = new UIElement();
        this.label = new Label();
        this.bar = new UIElement();

        this.barContainer.layout(layout -> {
            layout.setHeightPercent(100);
            layout.setWidthPercent(100);
            layout.setPadding(YogaEdge.ALL, 4);
        }).style(style -> style.backgroundTexture(Sprites.PROGRESS_CONTAINER));
        this.bar.style(style -> style.backgroundTexture(Sprites.PROGRESS_BAR));
        this.label.textStyle(style -> style
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER))
                .layout(layout -> {
                    layout.setHeightPercent(100);
                    layout.setWidthPercent(100);
                    layout.setPositionType(YogaPositionType.ABSOLUTE);
                });

        this.barContainer.addChildren(new UIElement()
                        .layout(layout -> {
                            layout.setHeightPercent(100);
                            layout.setWidthPercent(100);
                        })
                .addChildren(this.bar, this.label));
        this.addChildren(this.barContainer);
        updateProgressBarStyle(getNormalizedValue());
    }

    public ProgressBar progressBarStyle(Consumer<ProgressBarStyle> style) {
        style.accept(this.progressBarStyle);
        onStyleChanged();
        lastValue = value;
        updateProgressBarStyle(getNormalizedValue());
        return this;
    }

    public float getNormalizedValue() {
        return getNormalizedValue(value);
    }

    public float getNormalizedValue(float value) {
        return maxValue == minValue ? Float.NaN : (value - minValue) / (maxValue - minValue);
    }

    protected void updateProgressBarStyle(float normalizedValue) {
        switch (progressBarStyle.fillDirection) {
            case LEFT_TO_RIGHT -> {
                this.barContainer.layout(layout -> {
                    layout.setFlexDirection(YogaFlexDirection.COLUMN);
                    layout.setAlignItems(YogaAlign.FLEX_START);
                });
                this.bar.layout(layout -> {
                    layout.setHeightPercent(100);
                    layout.setWidthPercent(normalizedValue * 100);
                });
            }
            case RIGHT_TO_LEFT -> {
                this.barContainer.layout(layout -> {
                    layout.setFlexDirection(YogaFlexDirection.COLUMN);
                    layout.setAlignItems(YogaAlign.FLEX_END);
                });
                this.bar.layout(layout -> {
                    layout.setHeightPercent(100);
                    layout.setWidthPercent(normalizedValue * 100);
                });
            }
            case UP_TO_DOWN -> {
                this.barContainer.layout(layout -> {
                    layout.setFlexDirection(YogaFlexDirection.ROW);
                    layout.setAlignItems(YogaAlign.FLEX_START);
                });
                this.bar.layout(layout -> {
                    layout.setHeightPercent(normalizedValue * 100);
                    layout.setWidthPercent(100);
                });
            }
            case DOWN_TO_UP -> {
                this.barContainer.layout(layout -> {
                    layout.setFlexDirection(YogaFlexDirection.ROW);
                    layout.setAlignItems(YogaAlign.FLEX_END);
                });
                this.bar.layout(layout -> {
                    layout.setHeightPercent(normalizedValue * 100);
                    layout.setWidthPercent(100);
                });
            }
        }
    }

    public ProgressBar setRange(float minValue, float maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        setProgress(this.value);
        lastValue = this.value;
        updateProgressBarStyle(getNormalizedValue());
        return this;
    }

    public ProgressBar setProgress(float value) {
        return setValue(value);
    }

    @Override
    public ProgressBar bindDataSource(IDataProvider<Float> dataSource) {
        this.dataSources.put(dataSource, dataSource.registerListener(this::setProgress, true));
        return this;
    }

    @Override
    public ProgressBar unbindDataSource(IDataProvider<Float> dataSource) {
        var removed = this.dataSources.remove(dataSource);
        if (removed != null) {
            removed.unsubscribe();
        }
        return this;
    }

    @Override
    public ProgressBar setValue(@Nullable Float value) {
        if (value == null) value = 0f;
        var newValue = Math.max(minValue, Math.min(maxValue, value));
        if (newValue != this.value) {
            this.value = newValue;
            if (!progressBarStyle.interpolate) {
                lastValue = this.value;
            }
            updateProgressBarStyle(lastValue);
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
    public void applyStyle(Map<String, StyleValue<?>> values) {
        super.applyStyle(values);
        progressBarStyle.applyStyles(values);
    }

    @Override
    public void screenTick() {
        super.screenTick();
        if (lastValue != value) {
            var stepValue = progressBarStyle.interpolateStep * (maxValue - minValue);
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
            updateProgressBarStyle(lastValue);
        }
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        super.drawBackgroundAdditional(guiContext);
        if (progressBarStyle.interpolate && lastValue != value) {
            var stepValue = progressBarStyle.interpolateStep * (maxValue - minValue);
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
}
