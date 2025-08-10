package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigHDR;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.ui.ColorConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.HDRColorConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaGutter;
import org.appliedenergistics.yoga.YogaWrap;
import org.joml.Vector4f;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name = "vector4f", registry = "ldlib2:configurator_accessor")
public class Vector4fAccessor extends TypesAccessor<Vector4f> {

    public Vector4fAccessor() {
        super(Vector4f.class);
    }

    @Override
    public Vector4f defaultValue(Field field, Class<?> type) {
        if (field.isAnnotationPresent(DefaultValue.class)) {
            return new Vector4f((float) field.getAnnotation(DefaultValue.class).numberValue()[0],
                    (float) field.getAnnotation(DefaultValue.class).numberValue()[1],
                    (float) field.getAnnotation(DefaultValue.class).numberValue()[2],
                    (float) field.getAnnotation(DefaultValue.class).numberValue()[3]);
        }
        return new Vector4f(0, 0, 0, 0);
    }

    @Override
    public Configurator create(String name, Supplier<Vector4f> supplier, Consumer<Vector4f> consumer, boolean forceUpdate, Field field, Object owner) {
        if (field.isAnnotationPresent(ConfigHDR.class)) {
            return new HDRColorConfigurator(name, supplier, consumer, defaultValue(field, field.getType()), forceUpdate);
        }
        if (field.isAnnotationPresent(ConfigColor.class)) {
            return new ColorConfigurator(name,
                    () -> ColorUtils.fromVector4f(supplier.get()), ColorUtils::toVector4f,
                    ColorUtils.fromVector4f(defaultValue(field, field.getType())), forceUpdate);
        }
        var configurator = new Configurator(name);
        NumberConfigurator x, y, z, w;

        configurator.inlineContainer.addChildren(
                x = new NumberConfigurator("x", () -> supplier.get().x,
                        v -> consumer.accept(new Vector4f(v.floatValue(), supplier.get().y, supplier.get().z, supplier.get().w)),
                        defaultValue(field, field.getType()).x, forceUpdate),
                y = new NumberConfigurator("y", () -> supplier.get().y,
                        v -> consumer.accept(new Vector4f(supplier.get().x, v.floatValue(), supplier.get().z, supplier.get().w)),
                        defaultValue(field, field.getType()).y, forceUpdate),
                z = new NumberConfigurator("z", () -> supplier.get().z,
                        v -> consumer.accept(new Vector4f(supplier.get().x, supplier.get().y, v.floatValue(), supplier.get().w)),
                        defaultValue(field, field.getType()).z, forceUpdate),
                w = new NumberConfigurator("w", () -> supplier.get().w,
                        v -> consumer.accept(new Vector4f(supplier.get().x, supplier.get().y, supplier.get().z, v.floatValue())),
                        defaultValue(field, field.getType()).w, forceUpdate)
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
        w.layout(layout -> {
            layout.setFlex(1);
            layout.setMinWidth(40);
            layout.setHeight(14);
        });
        if (field.isAnnotationPresent(ConfigNumber.class)) {
            var config = field.getAnnotation(ConfigNumber.class);
            x.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
            y.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
            z.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
            w.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
        }

        configurator.setCopiable(() -> {
            var current = new Vector4f(supplier.get());
            return () -> new Vector4f(current);
        });
        configurator.setPastable(Vector4f.class, consumer);
        return configurator;
    }

}
