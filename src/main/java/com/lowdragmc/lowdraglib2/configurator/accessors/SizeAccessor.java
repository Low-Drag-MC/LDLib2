package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.math.Size;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaGutter;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/2
 * @implNote PositionAccessor
 */
@LDLRegisterClient(name = "size", registry = "ldlib2:configurator_accessor")
public class SizeAccessor extends TypesAccessor<Size> {

    public SizeAccessor() {
        super(Size.class);
    }

    @Override
    public Size defaultValue(Field field, Class<?> type) {
        if (field.isAnnotationPresent(DefaultValue.class)) {
            return Size.of((int) field.getAnnotation(DefaultValue.class).numberValue()[0], (int) field.getAnnotation(DefaultValue.class).numberValue()[1]);
        }
        return Size.ZERO;
    }

    @Override
    public Configurator create(String name, Supplier<Size> supplier, Consumer<Size> consumer, boolean forceUpdate, Field field, Object owner) {
        var configurator = new Configurator(name);
        NumberConfigurator width, height;
        configurator.inlineContainer.addChildren(
                width = new NumberConfigurator("width", () -> supplier.get().width,
                        v -> consumer.accept(Size.of(v.intValue(), supplier.get().height)),
                        defaultValue(field, field.getType()).width, forceUpdate),
                height = new NumberConfigurator("height", () -> supplier.get().height,
                        v -> consumer.accept(Size.of(supplier.get().width, v.intValue())),
                        defaultValue(field, field.getType()).height, forceUpdate)
        ).layout(layout -> {
            layout.gapAll(2);
            layout.marginLeft(2);
            layout.flexDirection(FlexDirection.ROW);
            layout.wrap(FlexWrap.WRAP);
        });
        width.layout(layout -> {
            layout.setFlex(1);
            layout.setMinWidth(40);
        });
        height.layout(layout -> {
            layout.setFlex(1);
            layout.setMinWidth(40);
        });
        if (field.isAnnotationPresent(ConfigNumber.class)) {
            var config = field.getAnnotation(ConfigNumber.class);
            width.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
            height.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
        }

        configurator.setCopiable(() -> {
            var current = supplier.get();
            return () -> current;
        });
        configurator.setPastable(Size.class, consumer);
        return configurator;
    }

}
