package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import org.jetbrains.annotations.Nullable;
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
    public Quaternionf defaultValue(@Nullable Field field, @Nullable Class<?> type) {
        if (field != null && field.isAnnotationPresent(DefaultValue.class)) {
            return new Quaternionf().rotateXYZ((float) field.getAnnotation(DefaultValue.class).numberValue()[0], (float) field.getAnnotation(DefaultValue.class).numberValue()[1], (float) field.getAnnotation(DefaultValue.class).numberValue()[2]);
        }
        return new Quaternionf();
    }

    @Override
    public Configurator create(String name, Supplier<Quaternionf> supplier, Consumer<Quaternionf> consumer, boolean forceUpdate, @Nullable Field field, @Nullable Object owner) {
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
                        defaultValue(field).x, forceUpdate),
                y = new NumberConfigurator("y", () -> supplier2.get().y,
                        v -> consumer2.accept(new Vector3f(supplier2.get().x, v.floatValue(), supplier2.get().z)),
                        defaultValue(field).y, forceUpdate),
                z = new NumberConfigurator("z", () -> supplier2.get().z,
                        v -> consumer2.accept(new Vector3f(supplier2.get().x, supplier2.get().y, v.floatValue())),
                        defaultValue(field).z, forceUpdate)
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
        if (field != null && field.isAnnotationPresent(ConfigNumber.class)) {
            var config = field.getAnnotation(ConfigNumber.class);
            x.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
            y.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
            z.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
        }

        configurator.setCopiable(() -> {
            var current = new Quaternionf(supplier.get());
            return () -> new Quaternionf(current);
        });
        configurator.setPastable(Quaternionf.class, consumer);
        return configurator;
    }

}
