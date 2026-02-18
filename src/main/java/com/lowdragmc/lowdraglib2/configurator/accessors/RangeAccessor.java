package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.math.Range;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/1
 * @implNote RangeAccessor
 */
@LDLRegisterClient(name = "range", registry = "ldlib2:configurator_accessor")
public class RangeAccessor extends TypesAccessor<Range> {

    public RangeAccessor() {
        super(Range.class);
    }

    @Override
    public Range defaultValue(@Nullable Field field, @Nullable Class<?> type) {
        if (field != null && field.isAnnotationPresent(ConfigNumber.class)) {
            var range = field.getAnnotation(ConfigNumber.class);
            if (range.range().length > 1) {
                return Range.of(range.range()[0], range.range()[1]);
            }
        }
        return Range.of(0f, 1f);
    }

    @Override
    public Configurator create(String name, Supplier<Range> supplier, Consumer<Range> consumer, boolean forceUpdate, @Nullable Field field, @Nullable Object owner) {
        var configurator = new Configurator(name);
        NumberConfigurator min, max;

        configurator.inlineContainer.addChildren(
                min = new NumberConfigurator("min", () -> supplier.get().getMin(),
                        v -> consumer.accept(Range.of(v.floatValue(), supplier.get().getMax())),
                        defaultValue(field).getMin(), forceUpdate),
                max = new NumberConfigurator("max", () -> supplier.get().getMax(),
                        v -> consumer.accept(Range.of(supplier.get().getMin(), v.floatValue())),
                        defaultValue(field).getMax(), forceUpdate)
        ).layout(layout -> {
            layout.gapAll(2);
            layout.marginLeft(2);
            layout.flexDirection(FlexDirection.ROW);
            layout.wrap(FlexWrap.WRAP);
        });
        min.layout(layout -> {
            layout.flex(1);
            layout.minWidth(40);
            layout.height(14);
        });
        max.layout(layout -> {
            layout.flex(1);
            layout.minWidth(40);
            layout.height(14);
        });
        if (field != null && field.isAnnotationPresent(ConfigNumber.class)) {
            var config = field.getAnnotation(ConfigNumber.class);
            min.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel()).setType(config.type());
            max.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel()).setType(config.type());
        }

        configurator.setCopiable(() -> {
            var current = supplier.get().copy();
            return current::copy;
        });
        configurator.setPastable(Range.class, consumer);
        return configurator;
    }
}
