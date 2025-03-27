package com.lowdragmc.lowdraglib.editor.configurator;

import com.lowdragmc.lowdraglib.math.Size;
import net.minecraft.world.phys.AABB;

import java.util.function.Consumer;
import java.util.function.Supplier;


public class AABBConfigurator extends ValueConfigurator<AABB> {

    public AABBConfigurator(String name, Supplier<AABB> supplier, Consumer<AABB> onUpdate, AABB defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        setSize(Size.of(200, 15 * 2));
        if (value == null) {
            value = defaultValue;
        }
    }

    @Override
    protected void onValueUpdate(AABB newValue) {
        if (newValue == null) newValue = defaultValue;
        if (newValue.equals(value)) return;
        super.onValueUpdate(newValue);
    }

    @Override
    public void init(int width) {
        super.init(width);
        assert value != null;
        var w = (width - leftWidth - rightWidth) / 3;
        var minX = new NumberConfigurator("minX", () -> this.value.minX, number -> {
            var newValue = new AABB(number.doubleValue(), this.value.minY, this.value.minZ, this.value.maxX, this.value.maxY, this.value.maxZ);
            onValueUpdate(newValue);
            updateValue();
        }, defaultValue.minX, forceUpdate);
        minX.setRange(-Double.MAX_VALUE, Double.MAX_VALUE);
        minX.setWheel(0.1);
        minX.setConfiguratorContainer(configuratorContainer);
        minX.init(w);
        minX.addSelfPosition(leftWidth, 0);
        addWidget(minX);

        var minY = new NumberConfigurator("minY", () -> this.value.minY, number -> {
            var newValue = new AABB(this.value.minX, number.doubleValue(), this.value.minZ, this.value.maxX, this.value.maxY, this.value.maxZ);
            onValueUpdate(newValue);
            updateValue();
        }, defaultValue.minY, forceUpdate);
        minY.setRange(-Double.MAX_VALUE, Double.MAX_VALUE);
        minY.setWheel(0.1);
        minY.setConfiguratorContainer(configuratorContainer);
        minY.init(w);
        minY.addSelfPosition(leftWidth + w, 0);
        addWidget(minY);

        var minZ = new NumberConfigurator("minZ", () -> this.value.minZ, number -> {
            var newValue = new AABB(this.value.minX, this.value.minY, number.doubleValue(), this.value.maxX, this.value.maxY, this.value.maxZ);
            onValueUpdate(newValue);
            updateValue();
        }, defaultValue.minZ, forceUpdate);
        minZ.setRange(-Double.MAX_VALUE, Double.MAX_VALUE);
        minZ.setWheel(0.1);
        minZ.setConfiguratorContainer(configuratorContainer);
        minZ.init(w);
        minZ.addSelfPosition(leftWidth + w * 2, 0);
        addWidget(minZ);

        var maxX = new NumberConfigurator("maxX", () -> this.value.maxX, number -> {
            var newValue = new AABB(this.value.minX, this.value.minY, this.value.minZ, number.doubleValue(), this.value.maxY, this.value.maxZ);
            onValueUpdate(newValue);
            updateValue();
        }, defaultValue.maxX, forceUpdate);
        maxX.setRange(-Double.MAX_VALUE, Double.MAX_VALUE);
        maxX.setWheel(0.1);
        maxX.setConfiguratorContainer(configuratorContainer);
        maxX.init(w);
        maxX.addSelfPosition(leftWidth, 15);
        addWidget(maxX);

        var maxY = new NumberConfigurator("maxY", () -> this.value.maxY, number -> {
            var newValue = new AABB(this.value.minX, this.value.minY, this.value.minZ, this.value.maxX, number.doubleValue(), this.value.maxZ);
            onValueUpdate(newValue);
            updateValue();
        }, defaultValue.maxY, forceUpdate);
        maxY.setRange(-Double.MAX_VALUE, Double.MAX_VALUE);
        maxY.setWheel(0.1);
        maxY.setConfiguratorContainer(configuratorContainer);
        maxY.init(w);
        maxY.addSelfPosition(leftWidth + w, 15);
        addWidget(maxY);

        var maxZ = new NumberConfigurator("maxZ", () -> this.value.maxZ, number -> {
            var newValue = new AABB(this.value.minX, this.value.minY, this.value.minZ, this.value.maxX, this.value.maxY, number.doubleValue());
            onValueUpdate(newValue);
            updateValue();
        }, defaultValue.maxZ, forceUpdate);
        maxZ.setRange(-Double.MAX_VALUE, Double.MAX_VALUE);
        maxZ.setWheel(0.1);
        maxZ.setConfiguratorContainer(configuratorContainer);
        maxZ.init(w);
        maxZ.addSelfPosition(leftWidth + w * 2, 15);
        addWidget(maxZ);
    }

}
