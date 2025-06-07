package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaGutter;
import org.appliedenergistics.yoga.YogaWrap;
import org.joml.Vector3i;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2023/5/27
 * @implNote Vector3iAccessor
 */
@LDLRegisterClient(name = "vector3i", registry = "ldlib2:configurator_accessor")
public class Vector3iAccessor extends TypesAccessor<Vector3i> {

    public Vector3iAccessor() {
        super(Vector3i.class);
    }

    @Override
    public Vector3i defaultValue(Field field, Class<?> type) {
        if (field.isAnnotationPresent(DefaultValue.class)) {
            return new Vector3i((int) field.getAnnotation(DefaultValue.class).numberValue()[0], (int) field.getAnnotation(DefaultValue.class).numberValue()[1], (int) field.getAnnotation(DefaultValue.class).numberValue()[2]);
        }
        return new Vector3i(0, 0, 0);
    }

    @Override
    public Configurator create(String name, Supplier<Vector3i> supplier, Consumer<Vector3i> consumer, boolean forceUpdate, Field field) {
        var configurator = new Configurator(name);
        NumberConfigurator x, y, z;
        configurator.inlineContainer.addChildren(
                x = new NumberConfigurator("x", () -> supplier.get().x,
                        v -> consumer.accept(new Vector3i(v.intValue(), supplier.get().y, supplier.get().z)),
                        defaultValue(field, field.getType()).x, forceUpdate),
                y = new NumberConfigurator("y", () -> supplier.get().y,
                        v -> consumer.accept(new Vector3i(supplier.get().x, v.intValue(), supplier.get().z)),
                        defaultValue(field, field.getType()).y, forceUpdate),
                z = new NumberConfigurator("z", () -> supplier.get().z,
                        v -> consumer.accept(new Vector3i(supplier.get().x, supplier.get().y, v.intValue())),
                        defaultValue(field, field.getType()).z, forceUpdate)
        ).layout(layout -> {
            layout.setGap(YogaGutter.ALL, 2);
            layout.setMargin(YogaEdge.LEFT, 2);
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setWrap(YogaWrap.WRAP);
        });
        x.layout(layout -> {
            layout.setFlex(1);
            layout.setMinWidth(40);
        });
        y.layout(layout -> {
            layout.setFlex(1);
            layout.setMinWidth(40);
        });
        z.layout(layout -> {
            layout.setFlex(1);
            layout.setMinWidth(40);
        });
        if (field.isAnnotationPresent(ConfigNumber.class)) {
            var config = field.getAnnotation(ConfigNumber.class);
            x.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
            y.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
            z.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
        }
        return configurator;
    }

}
