package com.lowdragmc.lowdraglib2.gui.ui.style.properties;

import com.lowdragmc.lowdraglib2.configurator.accessors.PivotAccessor;
import com.lowdragmc.lowdraglib2.configurator.accessors.Translate2DAccessor;
import com.lowdragmc.lowdraglib2.configurator.accessors.Vector2fAccessor;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.Transform2DValue;
import lombok.experimental.Accessors;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Accessors(chain = true)
public class Transform2DProperty extends Property<Transform2D> {
    public Transform2DProperty(String name, Transform2D initialValue) {
        super(name, Transform2D.class, Transform2D.CODEC, initialValue, Transform2DValue::new);
        setAllowTransition(true);
        setInterpolator(this::interpolate);
    }

    @Override
    public Configurator createConfiguratorInternal(String name, Supplier<Transform2D> getter, Consumer<Transform2D> setter) {
        var group = new ConfiguratorGroup(name);
        group.addConfigurators(
                new Translate2DAccessor().create("Transform2D.translate",
                        () -> getter.get().translate(),
                        tran -> setter.accept(getter.get().copy().translate(tran)), true, getVALUE_FIELD(), this),
                new Vector2fAccessor().create("Transform2D.scale",
                        () -> getter.get().scale(),
                        scale -> setter.accept(getter.get().copy().scale(scale.x, scale.y)), true, getVALUE_FIELD(), this),
                new NumberConfigurator("Transform2D.rotation", () -> getter.get().rotation(),
                        rotation -> setter.accept(getter.get().copy().rotation(rotation.floatValue())), 0f, true),
                new PivotAccessor().create("Transform2D.pivot", () -> getter.get().pivot(),
                        pivot -> setter.accept(getter.get().copy().pivot(pivot.x, pivot.y)), true, getVALUE_FIELD(), this)
        );
        return group;
    }

    private Transform2D interpolate(Transform2D from, Transform2D to, float interpolation) {
        return Transform2D.interpolate(from, to, interpolation);
    }
}
