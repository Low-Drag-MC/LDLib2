package com.lowdragmc.lowdraglib.configurator.ui;

import com.lowdragmc.lowdraglib.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/1
 * @implNote NumberConfigurator
 */
public class NumberConfigurator extends ValueConfigurator<Number> {
    protected ConfigNumber.Type numberType = ConfigNumber.Type.AUTO;
    protected final TextField textField;
    protected ImageWidget image;
    protected Number min, max, wheel;

    public NumberConfigurator(String name, Supplier<Number> supplier, Consumer<Number> onUpdate, @Nonnull Number defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        if (value == null) {
            value = defaultValue;
        }
        inlineContainer.addChildren(textField = new TextField());
        textField.setTextResponder(this::onNumberUpdate);
        min = value;
        max = value;
        wheel = 0;
        updateTextField();
    }

    public NumberConfigurator setRange(Number min, Number max) {
        this.min = min;
        this.max = max;
        updateTextField();
        return this;
    }

    public NumberConfigurator setWheel(Number wheel) {
        if (wheel.doubleValue() == 0) return this;
        this.wheel = wheel;
        updateTextField();
        return this;
    }

    public NumberConfigurator setType(ConfigNumber.Type type) {
        this.numberType = type;
        updateTextField();
        return this;
    }

    protected void updateTextField() {
        if (numberType == ConfigNumber.Type.INTEGER || (numberType == ConfigNumber.Type.AUTO && value instanceof Integer)){
            textField.setNumbersOnlyInt(min.intValue(), max.intValue());
        } else if (numberType == ConfigNumber.Type.LONG || (numberType == ConfigNumber.Type.AUTO && value instanceof Long)){
            textField.setNumbersOnlyLong(min.longValue(), max.longValue());
        } else if (numberType == ConfigNumber.Type.FLOAT || (numberType == ConfigNumber.Type.AUTO && value instanceof Float)){
            textField.setNumbersOnlyFloat(min.floatValue(), max.floatValue());
        } else if (numberType == ConfigNumber.Type.DOUBLE || (numberType == ConfigNumber.Type.AUTO && value instanceof Double)){
            textField.setNumbersOnlyDouble(min.doubleValue(), max.doubleValue());
        } else if (numberType == ConfigNumber.Type.SHORT || (numberType == ConfigNumber.Type.AUTO && value instanceof Short)){
            textField.setNumbersOnlyShort(min.shortValue(), max.shortValue());
        } else if (numberType == ConfigNumber.Type.BYTE || (numberType == ConfigNumber.Type.AUTO && value instanceof Byte)){
            textField.setNumbersOnlyByte(min.byteValue(), max.byteValue());
        }
        textField.setWheelDur(wheel.floatValue());
        updateTextFieldValue();
    }

    protected void updateTextFieldValue() {
        assert value != null;
        if (numberType == ConfigNumber.Type.INTEGER || (numberType == ConfigNumber.Type.AUTO && value instanceof Integer)){
            textField.setText(String.valueOf(value.intValue()), false);
        } else if (numberType == ConfigNumber.Type.LONG || (numberType == ConfigNumber.Type.AUTO && value instanceof Long)){
            textField.setText(String.valueOf(value.longValue()), false);
        } else if (numberType == ConfigNumber.Type.FLOAT || (numberType == ConfigNumber.Type.AUTO && value instanceof Float)){
            textField.setText(String.valueOf(value.floatValue()), false);
        } else if (numberType == ConfigNumber.Type.DOUBLE || (numberType == ConfigNumber.Type.AUTO && value instanceof Double)){
            textField.setText(String.valueOf(value.doubleValue()), false);
        } else if (numberType == ConfigNumber.Type.SHORT || (numberType == ConfigNumber.Type.AUTO && value instanceof Short)){
            textField.setText(String.valueOf(value.shortValue()), false);
        } else if (numberType == ConfigNumber.Type.BYTE || (numberType == ConfigNumber.Type.AUTO && value instanceof Byte)){
            textField.setText(String.valueOf(value.byteValue()), false);
        }
    }

    @Override
    protected void onValueUpdate(Number newValue) {
        if (newValue == null) newValue = defaultValue;
        if (newValue.equals(value)) return;
        super.onValueUpdate(newValue);
        updateTextFieldValue();
    }

    private void onNumberUpdate(String s) {
        Number number = null;
        if (numberType == ConfigNumber.Type.INTEGER || (numberType == ConfigNumber.Type.AUTO && value instanceof Integer)){
            number = Integer.parseInt(s);
        } else if (numberType == ConfigNumber.Type.LONG || (numberType == ConfigNumber.Type.AUTO && value instanceof Long)){
            number = Long.parseLong(s);
        } else if (numberType == ConfigNumber.Type.FLOAT || (numberType == ConfigNumber.Type.AUTO && value instanceof Float)){
            number = Float.parseFloat(s);
        } else if (numberType == ConfigNumber.Type.DOUBLE || (numberType == ConfigNumber.Type.AUTO && value instanceof Double)){
            number = Double.parseDouble(s);
        } else if (numberType == ConfigNumber.Type.SHORT || (numberType == ConfigNumber.Type.AUTO && value instanceof Short)){
            number = Short.parseShort(s);
        } else if (numberType == ConfigNumber.Type.BYTE || (numberType == ConfigNumber.Type.AUTO && value instanceof Byte)){
            number = Byte.parseByte(s);
        }
        if (number == null) {
            number = defaultValue;
        }

        value = number;
        updateValue();
    }

}
