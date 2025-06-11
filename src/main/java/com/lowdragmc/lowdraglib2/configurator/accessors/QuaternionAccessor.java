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
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name = "quaternion", registry = "ldlib2:configurator_accessor")
public class QuaternionAccessor extends TypesAccessor<Quaternionf> {

    public QuaternionAccessor() {
        super(Quaternionf.class);
    }

    @Override
    public Quaternionf defaultValue(Field field, Class<?> type) {
        if (field.isAnnotationPresent(DefaultValue.class)) {
            return new Quaternionf().rotateXYZ((float) field.getAnnotation(DefaultValue.class).numberValue()[0], (float) field.getAnnotation(DefaultValue.class).numberValue()[1], (float) field.getAnnotation(DefaultValue.class).numberValue()[2]);
        }
        return new Quaternionf();
    }

    @Override
    public Configurator create(String name, Supplier<Quaternionf> supplier, Consumer<Quaternionf> consumer, boolean forceUpdate, Field field, Object owner) {
        Supplier<Vector3f> supplier2 = () -> supplier.get().getEulerAnglesXYZ(new Vector3f()).mul(57.29577951308232f);
        Consumer<Vector3f> consumer2 = v -> {
            var q = new Quaternionf();
            q.rotateXYZ(
                    (float) Math.toRadians(v.x),
                    (float) Math.toRadians(v.y),
                    (float) Math.toRadians(v.z));
            consumer.accept(q);
        };

        var configurator = new Configurator(name);
        NumberConfigurator x, y, z;

        configurator.inlineContainer.addChildren(
                x = new NumberConfigurator("x", () -> supplier2.get().x,
                        v -> consumer2.accept(new Vector3f(v.floatValue(), supplier2.get().y, supplier2.get().z)),
                        defaultValue(field, field.getType()).x, forceUpdate),
                y = new NumberConfigurator("y", () -> supplier2.get().y,
                        v -> consumer2.accept(new Vector3f(supplier2.get().x, v.floatValue(), supplier2.get().z)),
                        defaultValue(field, field.getType()).y, forceUpdate),
                z = new NumberConfigurator("z", () -> supplier2.get().z,
                        v -> consumer2.accept(new Vector3f(supplier2.get().x, supplier2.get().y, v.floatValue())),
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
