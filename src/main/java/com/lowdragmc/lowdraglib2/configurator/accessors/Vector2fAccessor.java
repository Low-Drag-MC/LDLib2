package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name = "vector2f", registry = "ldlib2:configurator_accessor")
public class Vector2fAccessor extends TypesAccessor<Vector2f> {

    public Vector2fAccessor() {
        super(Vector2f.class);
    }

    @Override
    public Vector2f defaultValue(@Nullable Field field, @Nullable Class<?> type) {
        if (field != null && field.isAnnotationPresent(DefaultValue.class)) {
            return new Vector2f((float) field.getAnnotation(DefaultValue.class).numberValue()[0], (float) field.getAnnotation(DefaultValue.class).numberValue()[1]);
        }
        return new Vector2f(0, 0);
    }

    @Override
    public Configurator create(String name, Supplier<Vector2f> supplier, Consumer<Vector2f> consumer, boolean forceUpdate, @Nullable Field field, @Nullable Object owner) {
        var configurator = new Configurator(name);
        NumberConfigurator x, y;

        configurator.inlineContainer.addChildren(
                x = new NumberConfigurator("x", () -> supplier.get().x,
                        v -> consumer.accept(new Vector2f(v.floatValue(), supplier.get().y)),
                        defaultValue(field).x, forceUpdate),
                y = new NumberConfigurator("y", () -> supplier.get().y,
                        v -> consumer.accept(new Vector2f(supplier.get().x, v.floatValue())),
                        defaultValue(field).y, forceUpdate)
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
        if (field != null && field.isAnnotationPresent(ConfigNumber.class)) {
            var config = field.getAnnotation(ConfigNumber.class);
            x.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
            y.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
        }

        configurator.setCopiable(() -> {
            var current = new Vector2f(supplier.get());
            return () -> new Vector2f(current);
        });
        configurator.setPastable(Vector2f.class, consumer);
        return configurator;
    }

}
