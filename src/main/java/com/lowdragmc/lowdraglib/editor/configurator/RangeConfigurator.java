package com.lowdragmc.lowdraglib.editor.configurator;

import com.lowdragmc.lowdraglib.math.Range;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/1
 * @implNote RangeConfigurator
 */
public class RangeConfigurator extends ValueConfigurator<Range> {
    protected Number min, max, wheel;

    public RangeConfigurator(String name, Supplier<Range> supplier, Consumer<Range> onUpdate, Range defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        if (value == null) {
            value = defaultValue;
        }
        var isDecimal = value.getA() instanceof Double || value.getB() instanceof Double || value.getA() instanceof Float || value.getB() instanceof Float;
        setRange(Math.min(value.getA().doubleValue(), value.getB().doubleValue()), Math.max(value.getA().doubleValue(), value.getB().doubleValue()));
        if (isDecimal) {
            setWheel(0.1);
        } else {
            setWheel(1);
        }
    }

    public RangeConfigurator setRange(Number min, Number max) {
        this.min = min;
        this.max = max;
        return this;
    }

    public RangeConfigurator setWheel(Number wheel) {
        if (wheel.doubleValue() == 0) return this;
        this.wheel = wheel;
        return this;
    }

    @Override
    protected void onValueUpdate(Range newValue) {
        if (newValue == null) newValue = defaultValue;
        if (newValue.equals(value)) return;
        super.onValueUpdate(newValue);
    }

    @Override
    public void init(int width) {
        super.init(width);
        assert value != null;
        var w = (width - leftWidth - rightWidth) / 2;
        var x = new NumberConfigurator("", () -> this.value.getA(), number -> {
            setValue(Range.of(number, value.getB()));
            updateValue();
        }, defaultValue.getA(), forceUpdate);
        x.setRange(min, max);
        x.setWheel(wheel);
        x.setConfiguratorContainer(configuratorContainer);
        x.init(w);
        x.addSelfPosition(leftWidth, 0);
        addWidget(x);

        var y = new NumberConfigurator("", () -> this.value.getB(), number -> {
            setValue(Range.of(value.getA(), number));
            updateValue();
        }, defaultValue.getB(), forceUpdate);
        y.setRange(min, max);
        y.setWheel(wheel);
        y.setConfiguratorContainer(configuratorContainer);
        y.init(w);
        y.addSelfPosition(leftWidth + w, 0);
        addWidget(y);
    }

}
