package com.lowdragmc.lowdraglib.editor.configurator;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3i;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2023/5/27
 * @implNote Vector3iConfigurator
 */
public class Vector3iConfigurator extends ValueConfigurator<Vector3i>{

    protected int min, max, wheel;

    public Vector3iConfigurator(String name, Supplier<Vector3i> supplier, Consumer<Vector3i> onUpdate, @NotNull Vector3i defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        if (value == null) {
            value = defaultValue;
        }
        setWheel(1);
    }

    public Vector3iConfigurator setRange(int min, int max) {
        this.min = min;
        this.max = max;
        return this;
    }

    public Vector3iConfigurator setWheel(int wheel) {
        if (wheel == 0) return this;
        this.wheel = wheel;
        return this;
    }

    @Override
    protected void onValueUpdate(Vector3i newValue) {
        if (newValue == null) newValue = defaultValue;
        if (newValue.equals(value)) return;
        super.onValueUpdate(newValue);
    }

    @Override
    public void init(int width) {
        super.init(width);
        assert value != null;
        var w = (width - leftWidth - rightWidth) / 3;
        var x = new NumberConfigurator("x", () -> this.value.x, number -> {
            this.value.x = number.intValue();
            updateValue();
        }, defaultValue.x, forceUpdate);
        x.setRange(min, max);
        x.setWheel(wheel);
        x.setConfiguratorContainer(configuratorContainer);
        x.init(w);
        x.addSelfPosition(leftWidth, 0);
        addWidget(x);

        var y = new NumberConfigurator("y", () -> this.value.y, number -> {
            this.value.y = number.intValue();
            updateValue();
        }, defaultValue.y, forceUpdate);
        y.setRange(min, max);
        y.setWheel(wheel);
        y.setConfiguratorContainer(configuratorContainer);
        y.init(w);
        y.addSelfPosition(leftWidth + w, 0);
        addWidget(y);

        var z = new NumberConfigurator("z", () -> this.value.z, number -> {
            this.value.z = number.intValue();
            updateValue();
        }, defaultValue.z, forceUpdate);
        z.setRange(min, max);
        z.setWheel(wheel);
        z.setConfiguratorContainer(configuratorContainer);
        z.init(w);
        z.addSelfPosition(leftWidth + w * 2, 0);
        addWidget(z);
    }
}
