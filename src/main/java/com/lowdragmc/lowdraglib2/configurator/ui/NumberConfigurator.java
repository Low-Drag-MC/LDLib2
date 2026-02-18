package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import lombok.Getter;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/1
 * @implNote NumberConfigurator
 */
public class NumberConfigurator extends ValueConfigurator<Number> {
    public final TextField textField;
    @Getter
    protected ConfigNumber.Type numberType = ConfigNumber.Type.AUTO;
    @Getter
    @Nullable
    protected Number min, max, wheel;

    public NumberConfigurator(String name, Supplier<Number> supplier, Consumer<Number> onUpdate, @Nonnull Number defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        setCopiable(value -> value);

        if (value == null) {
            value = defaultValue;
        }
        inlineContainer.addChildren(textField = new TextField());
        textField.setTextResponder(this::onNumberUpdate);
        updateTextField();
    }

    @Override
    protected void onPaste(Number pasted) {
        if ((max == null || pasted.doubleValue() <= max.doubleValue()) &&
                (min == null || pasted.doubleValue() >= min.doubleValue())) {
            super.onPaste(pasted);
        }
    }

    @Override
    protected boolean canDropObject(@Nonnull Object object) {
        return object instanceof Number || super.canDropObject(object);
    }

    @Override
    protected void onDropObject(@Nonnull Object object) {
        if (object instanceof Number number) {
            if (numberType == ConfigNumber.Type.INTEGER || (numberType == ConfigNumber.Type.AUTO && value instanceof Integer)){
                number = number.intValue();
            } else if (numberType == ConfigNumber.Type.LONG || (numberType == ConfigNumber.Type.AUTO && value instanceof Long)){
                number = number.longValue();
            } else if (numberType == ConfigNumber.Type.FLOAT || (numberType == ConfigNumber.Type.AUTO && value instanceof Float)){
                number = number.floatValue();
            } else if (numberType == ConfigNumber.Type.DOUBLE || (numberType == ConfigNumber.Type.AUTO && value instanceof Double)){
                number = number.doubleValue();
            } else if (numberType == ConfigNumber.Type.SHORT || (numberType == ConfigNumber.Type.AUTO && value instanceof Short)){
                number = number.shortValue();
            } else if (numberType == ConfigNumber.Type.BYTE || (numberType == ConfigNumber.Type.AUTO && value instanceof Byte)){
                number = number.byteValue();
            }
            updateValueActively(number);
            updateTextFieldValue();
        }
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
        float wheelValue = 0;
        if (numberType == ConfigNumber.Type.INTEGER || (numberType == ConfigNumber.Type.AUTO && value instanceof Integer)){
            textField.setNumbersOnlyInt(min == null ? Integer.MIN_VALUE : min.intValue(), max == null ? Integer.MAX_VALUE : max.intValue());
            wheelValue = 1;
            if (wheel != null) wheelValue = Math.max(wheelValue, wheel.intValue());
        } else if (numberType == ConfigNumber.Type.LONG || (numberType == ConfigNumber.Type.AUTO && value instanceof Long)){
            textField.setNumbersOnlyLong(min == null ? Long.MIN_VALUE : min.longValue(), max == null ? Long.MAX_VALUE : max.longValue());
            wheelValue = 1;
            if (wheel != null) wheelValue = Math.max(wheelValue, wheel.longValue());
        } else if (numberType == ConfigNumber.Type.FLOAT || (numberType == ConfigNumber.Type.AUTO && value instanceof Float)){
            textField.setNumbersOnlyFloat(min == null ? -Float.MAX_VALUE : min.floatValue(), max == null ? Float.MAX_VALUE : max.floatValue());
            wheelValue = 0.1f;
            if (wheel != null) wheelValue = wheel.floatValue();
        } else if (numberType == ConfigNumber.Type.DOUBLE || (numberType == ConfigNumber.Type.AUTO && value instanceof Double)){
            textField.setNumbersOnlyDouble(min == null ? -Double.MAX_VALUE : min.doubleValue(), max == null ? Double.MAX_VALUE : max.doubleValue());
            wheelValue = 0.1f;
            if (wheel != null) wheelValue = wheel.floatValue();
        } else if (numberType == ConfigNumber.Type.SHORT || (numberType == ConfigNumber.Type.AUTO && value instanceof Short)){
            textField.setNumbersOnlyShort(min == null ? Short.MIN_VALUE : min.shortValue(), max == null ? Short.MAX_VALUE : max.shortValue());
            wheelValue = 1;
            if (wheel != null) wheelValue = Math.max(wheelValue, wheel.shortValue());
        } else if (numberType == ConfigNumber.Type.BYTE || (numberType == ConfigNumber.Type.AUTO && value instanceof Byte)){
            textField.setNumbersOnlyByte(min == null ? Byte.MIN_VALUE : min.byteValue(), max == null ? Byte.MAX_VALUE : max.byteValue());
            wheelValue = 1;
            if (wheel != null) wheelValue = Math.max(wheelValue, wheel.byteValue());
        }
        if (wheel != null) {
            wheelValue = wheel.floatValue();
        }
        textField.setWheelDur(wheelValue);
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
    protected void onValueUpdatePassively(Number newValue) {
        if (newValue == null) newValue = defaultValue;
        if (newValue.equals(value)) return;
        super.onValueUpdatePassively(newValue);
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

        updateValueActively(number);
    }
}
