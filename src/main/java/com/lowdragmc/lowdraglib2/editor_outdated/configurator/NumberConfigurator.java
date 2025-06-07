package com.lowdragmc.lowdraglib2.editor_outdated.configurator;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib2.gui.widget.TextFieldWidget;
import lombok.Setter;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/1
 * @implNote NumberConfigurator
 */
public class NumberConfigurator extends ValueConfigurator<Number> {
    protected boolean isDecimal;
    protected TextFieldWidget textFieldWidget;
    protected ImageWidget image;
    protected Number min, max, wheel;
    @Setter
    protected boolean colorBackground;

    public NumberConfigurator(String name, Supplier<Number> supplier, Consumer<Number> onUpdate, @Nonnull Number defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        if (value == null) {
            value = defaultValue;
        }
        this.isDecimal = value instanceof Double || value instanceof Float;
        setRange(value, value);
        if (isDecimal) {
            setWheel(0.1);
        } else {
            setWheel(1);
        }
    }

    public NumberConfigurator setRange(Number min, Number max) {
        this.min = min;
        this.max = max;
        return this;
    }

    public NumberConfigurator setWheel(Number wheel) {
        if (wheel.doubleValue() == 0) return this;
        this.wheel = wheel;
        return this;
    }

    @Override
    protected void onValueUpdate(Number newValue) {
        if (newValue == null) newValue = defaultValue;
        if (newValue.equals(value)) return;
        super.onValueUpdate(newValue);
        if (isDecimal) {
            textFieldWidget.setCurrentString(value.floatValue());
        } else {
            textFieldWidget.setCurrentString(value.longValue());
        }
    }

    private IGuiTexture getCommonColor() {
        return colorBackground ? new ColorRectTexture(value.intValue()).setRadius(5).setRadius(5) : ColorPattern.T_GRAY.rectTexture().setRadius(5);
    }

    @Override
    public void init(int width) {
        super.init(width);
        addWidget(image = new ImageWidget(leftWidth, 2, width - leftWidth - 3 - rightWidth, 10, getCommonColor()));
        image.setDraggingConsumer(
                o -> o instanceof Number,
                o -> image.setImage(ColorPattern.GREEN.rectTexture().setRadius(5)),
                o -> image.setImage(getCommonColor()),
                o -> {
                    if (o instanceof Number number) {
                        onValueUpdate(number);
                        updateValue();
                    }
                    image.setImage(getCommonColor());
                });
        addWidget(textFieldWidget = new TextFieldWidget(leftWidth + 3, 2, width - leftWidth - 6 - rightWidth, 10, null, this::onNumberUpdate));
        textFieldWidget.setClientSideWidget();
        if (isDecimal) {
            textFieldWidget.setCurrentString(value.floatValue());
        } else {
            textFieldWidget.setCurrentString(value.longValue());
        }
        textFieldWidget.setBordered(false);
        if (isDecimal) {
            textFieldWidget.setNumbersOnly(min.floatValue(), max.floatValue());
        } else {
            textFieldWidget.setNumbersOnly(min.longValue(), max.longValue());
        }
        textFieldWidget.setWheelDur(wheel.floatValue());
    }

    private void onNumberUpdate(String s) {
        Number newValue = isDecimal ? Float.parseFloat(s) : Long.parseLong(s);
        if (value instanceof Integer && !value.equals(newValue.intValue())) {
            value = newValue.intValue();
            updateValue();
        } else if (value instanceof Long && !value.equals(newValue.longValue())) {
            value = newValue.longValue();
            updateValue();
        } else if (value instanceof Float && !value.equals(newValue.floatValue())) {
            value = newValue.floatValue();
            updateValue();
        } else if (value instanceof Double && !value.equals(newValue.doubleValue())) {
            value = newValue.doubleValue();
            updateValue();
        } else if (value instanceof Byte && !value.equals(newValue.byteValue())) {
            value = newValue.byteValue();
            updateValue();
        } else if (value == null) {
            if (isDecimal) {
                value = newValue.floatValue();
            } else {
                value = newValue.intValue();
            }
            updateValue();
        }
        if (colorBackground) {
            image.setImage(getCommonColor());
        }
    }

}
