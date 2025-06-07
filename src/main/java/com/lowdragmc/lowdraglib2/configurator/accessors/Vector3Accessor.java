package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaGutter;
import org.appliedenergistics.yoga.YogaWrap;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2023/5/27
 * @implNote Vector3Accessor
 */
@LDLRegisterClient(name = "vector3", registry = "ldlib2:configurator_accessor")
public class Vector3Accessor extends TypesAccessor<Vector3f> {

    public Vector3Accessor() {
        super(Vector3f.class);
    }

    @Override
    public Vector3f defaultValue(Field field, Class<?> type) {
        if (field.isAnnotationPresent(DefaultValue.class)) {
            return new Vector3f((float) field.getAnnotation(DefaultValue.class).numberValue()[0], (float) field.getAnnotation(DefaultValue.class).numberValue()[1], (float) field.getAnnotation(DefaultValue.class).numberValue()[2]);
        }
        return new Vector3f(0, 0, 0);
    }

    @Override
    public Configurator create(String name, Supplier<Vector3f> supplier, Consumer<Vector3f> consumer, boolean forceUpdate, Field field) {
        var configurator = new Configurator(name);
        NumberConfigurator x, y, z;

        configurator.inlineContainer.addChildren(
                x = new NumberConfigurator("x", () -> supplier.get().x,
                        v -> consumer.accept(new Vector3f(v.floatValue(), supplier.get().y, supplier.get().z)),
                        defaultValue(field, field.getType()).x, forceUpdate),
                y = new NumberConfigurator("y", () -> supplier.get().y,
                        v -> consumer.accept(new Vector3f(supplier.get().x, v.floatValue(), supplier.get().z)),
                        defaultValue(field, field.getType()).y, forceUpdate),
                z = new NumberConfigurator("z", () -> supplier.get().z,
                        v -> consumer.accept(new Vector3f(supplier.get().x, supplier.get().y, v.floatValue())),
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
            layout.setHeight(14);
        });
        y.layout(layout -> {
            layout.setFlex(1);
            layout.setMinWidth(40);
            layout.setHeight(14);
        });
        z.layout(layout -> {
            layout.setFlex(1);
            layout.setMinWidth(40);
            layout.setHeight(14);
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
