package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.math.Range;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaGutter;
import org.appliedenergistics.yoga.YogaWrap;

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
    public Range defaultValue(Field field, Class<?> type) {
        if (field.isAnnotationPresent(ConfigNumber.class)) {
            var range = field.getAnnotation(ConfigNumber.class);
            if (range.range().length > 1) {
                return Range.of(range.range()[0], range.range()[1]);
            }
        }
        return Range.of(0f, 1f);
    }

    @Override
    public Configurator create(String name, Supplier<Range> supplier, Consumer<Range> consumer, boolean forceUpdate, Field field) {
        var configurator = new Configurator(name);
        NumberConfigurator min, max;

        configurator.inlineContainer.addChildren(
                min = new NumberConfigurator("min", () -> supplier.get().getMin(),
                        v -> consumer.accept(Range.of(v.floatValue(), supplier.get().getMax())),
                        defaultValue(field, field.getType()).getMin(), forceUpdate),
                max = new NumberConfigurator("max", () -> supplier.get().getMax(),
                        v -> consumer.accept(Range.of(supplier.get().getMin(), v.floatValue())),
                        defaultValue(field, field.getType()).getMax(), forceUpdate)
        ).layout(layout -> {
            layout.setGap(YogaGutter.ALL, 2);
            layout.setMargin(YogaEdge.LEFT, 2);
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setWrap(YogaWrap.WRAP);
        });
        min.layout(layout -> {
            layout.setFlex(1);
            layout.setMinWidth(40);
            layout.setHeight(14);
        });
        max.layout(layout -> {
            layout.setFlex(1);
            layout.setMinWidth(40);
            layout.setHeight(14);
        });
        if (field.isAnnotationPresent(ConfigNumber.class)) {
            var config = field.getAnnotation(ConfigNumber.class);
            min.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel()).setType(config.type());
            max.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel()).setType(config.type());
        }
        return configurator;
    }
}
