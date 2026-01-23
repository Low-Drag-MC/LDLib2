package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.data.Pivot;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaGutter;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name = "pivot", registry = "ldlib2:configurator_accessor")
public class PivotAccessor extends TypesAccessor<Pivot> {

    public PivotAccessor() {
        super(Pivot.class);
    }

    @Override
    public Pivot defaultValue(Field field, Class<?> type) {
        if (field.isAnnotationPresent(DefaultValue.class)) {
            return Pivot.of((float) field.getAnnotation(DefaultValue.class).numberValue()[0], (float) field.getAnnotation(DefaultValue.class).numberValue()[1]);
        }
        return Pivot.CENTER;
    }

    @Override
    public Configurator create(String name, Supplier<Pivot> supplier, Consumer<Pivot> consumer, boolean forceUpdate, Field field, Object owner) {
        var configurator = new Configurator(name);
        NumberConfigurator x, y;

        configurator.inlineContainer.addChildren(
                x = new NumberConfigurator("x", () -> supplier.get().x,
                        v -> consumer.accept(Pivot.of(v.floatValue(), supplier.get().y)),
                        defaultValue(field, field.getType()).x, forceUpdate),
                y = new NumberConfigurator("y", () -> supplier.get().y,
                        v -> consumer.accept(Pivot.of(supplier.get().x, v.floatValue())),
                        defaultValue(field, field.getType()).y, forceUpdate)
        ).layout(layout -> {
            layout.setGap(YogaGutter.ALL, 2);
            layout.marginLeft(2);
            layout.flexDirection(FlexDirection.ROW);
            layout.wrap(FlexWrap.WRAP);
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
        if (field.isAnnotationPresent(ConfigNumber.class)) {
            var config = field.getAnnotation(ConfigNumber.class);
            x.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
            y.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
        }

        configurator.setCopiable(() -> {
            var current = supplier.get();
            return () -> current;
        });
        configurator.setPastable(Pivot.class, consumer);
        return configurator;
    }

}
