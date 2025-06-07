package com.lowdragmc.lowdraglib.gui.ui.elements;

import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.ui.BindableUIElement;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib.gui.ui.style.Style;
import com.lowdragmc.lowdraglib.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib.gui.ui.styletemplate.Sprites;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaPositionType;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
public class ProgressBar extends BindableUIElement<Float> {
    @Accessors(chain = true, fluent = true)
    public static class ProgressBarStyle extends Style {
        @Getter @Setter
        private ProgressTexture.FillDirection fillDirection = ProgressTexture.FillDirection.LEFT_TO_RIGHT;

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
        updateProgressBarStyle();
    }

    public ProgressBar progressBarStyle(Consumer<ProgressBarStyle> style) {
        style.accept(this.progressBarStyle);
        onStyleChanged();
        updateProgressBarStyle();
        return this;
    }

    public float getNormalizedValue() {
        return maxValue == minValue ? Float.NaN : (value - minValue) / (maxValue - minValue);
    }

    protected void updateProgressBarStyle() {
        switch (progressBarStyle.fillDirection) {
            case LEFT_TO_RIGHT -> {
                this.barContainer.layout(layout -> {
                    layout.setFlexDirection(YogaFlexDirection.COLUMN);
                    layout.setAlignItems(YogaAlign.FLEX_START);
                });
                this.bar.layout(layout -> {
                    layout.setHeightPercent(100);
                    layout.setWidthPercent(getNormalizedValue() * 100);
                });
            }
            case RIGHT_TO_LEFT -> {
                this.barContainer.layout(layout -> {
                    layout.setFlexDirection(YogaFlexDirection.COLUMN);
                    layout.setAlignItems(YogaAlign.FLEX_END);
                });
                this.bar.layout(layout -> {
                    layout.setHeightPercent(100);
                    layout.setWidthPercent(getNormalizedValue() * 100);
                });
            }
            case UP_TO_DOWN -> {
                this.barContainer.layout(layout -> {
                    layout.setFlexDirection(YogaFlexDirection.ROW);
                    layout.setAlignItems(YogaAlign.FLEX_START);
                });
                this.bar.layout(layout -> {
                    layout.setHeightPercent(getNormalizedValue() * 100);
                    layout.setWidthPercent(100);
                });
            }
            case DOWN_TO_UP -> {
                this.barContainer.layout(layout -> {
                    layout.setFlexDirection(YogaFlexDirection.ROW);
                    layout.setAlignItems(YogaAlign.FLEX_END);
                });
                this.bar.layout(layout -> {
                    layout.setHeightPercent(getNormalizedValue() * 100);
                    layout.setWidthPercent(100);
                });
            }
        }
    }

    public ProgressBar setRange(float minValue, float maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        setProgress(this.value);
        updateProgressBarStyle();
        return this;
    }

    public ProgressBar setProgress(float value, boolean notify) {
        return setValue(value, notify);
    }

    public ProgressBar setProgress(float value) {
        return setProgress(value, true);
    }

    public ProgressBar setOnProgressChange(FloatConsumer onProgressChange) {
        registerValueListener(v -> onProgressChange.accept(v.floatValue()));
        return this;
    }

    @Override
    public ProgressBar setValue(Float value, boolean notify) {
        var newValue = Math.max(minValue, Math.min(maxValue, value));
        if (newValue != this.value) {
            this.value = newValue;
            if (notify) {
                notifyListeners();
            }
            updateProgressBarStyle();
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
}
