package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4i;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name = "vector4i", registry = "ldlib2:configurator_accessor")
public class Vector4iAccessor extends TypesAccessor<Vector4i> {

    public Vector4iAccessor() {
        super(Vector4i.class);
    }

    @Override
    public Vector4i defaultValue(@Nullable Field field, @Nullable Class<?> type) {
        if (field != null && field.isAnnotationPresent(DefaultValue.class)) {
            return new Vector4i((int) field.getAnnotation(DefaultValue.class).numberValue()[0],
                    (int) field.getAnnotation(DefaultValue.class).numberValue()[1],
                    (int) field.getAnnotation(DefaultValue.class).numberValue()[2],
                    (int) field.getAnnotation(DefaultValue.class).numberValue()[3]);
        }
        return new Vector4i(0, 0, 0, 0);
    }

    @Override
    public Configurator create(String name, Supplier<Vector4i> supplier, Consumer<Vector4i> consumer, boolean forceUpdate, @Nullable Field field, @Nullable Object owner) {
        var configurator = new Configurator(name);
        NumberConfigurator x, y, z, w;

        configurator.inlineContainer.addChildren(
                x = new NumberConfigurator("x", () -> supplier.get().x,
                        v -> consumer.accept(new Vector4i(v.intValue(), supplier.get().y, supplier.get().z, supplier.get().w)),
                        defaultValue(field).x, forceUpdate),
                y = new NumberConfigurator("y", () -> supplier.get().y,
                        v -> consumer.accept(new Vector4i(supplier.get().x, v.intValue(), supplier.get().z, supplier.get().w)),
                        defaultValue(field).y, forceUpdate),
                z = new NumberConfigurator("z", () -> supplier.get().z,
                        v -> consumer.accept(new Vector4i(supplier.get().x, supplier.get().y, v.intValue(), supplier.get().w)),
                        defaultValue(field).z, forceUpdate),
                w = new NumberConfigurator("w", () -> supplier.get().w,
                        v -> consumer.accept(new Vector4i(supplier.get().x, supplier.get().y, supplier.get().z, v.intValue())),
                        defaultValue(field).w, forceUpdate)
        ).layout(layout -> {
            layout.gapAll(2);
            layout.marginLeft(2);
            layout.flexDirection(FlexDirection.ROW);
            layout.wrap(FlexWrap.WRAP);
        });
        x.layout(layout -> {
            layout.flex(1);
            layout.minWidth(40);
            layout.height(14);
        });
        y.layout(layout -> {
            layout.flex(1);
            layout.minWidth(40);
            layout.height(14);
        });
        z.layout(layout -> {
            layout.flex(1);
            layout.minWidth(40);
            layout.height(14);
        });
        w.layout(layout -> {
            layout.flex(1);
            layout.minWidth(40);
            layout.height(14);
        });
        if (field != null && field.isAnnotationPresent(ConfigNumber.class)) {
            var config = field.getAnnotation(ConfigNumber.class);
            x.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
            y.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
            z.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
            w.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
        }

        configurator.setCopiable(() -> {
            var current = new Vector4i(supplier.get());
            return () -> new Vector4i(current);
        });
        configurator.setPastable(Vector4i.class, consumer);
        return configurator;
    }

}
