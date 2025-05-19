package com.lowdragmc.lowdraglib.editor.accessors;

import com.lowdragmc.lowdraglib.editor.configurator.RangeConfigurator;
import com.lowdragmc.lowdraglib.math.Range;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib.editor.configurator.Configurator;
import com.lowdragmc.lowdraglib.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/1
 * @implNote RangeAccessor
 */
@LDLRegisterClient(name = "range", registry = "ldlib:configurator_accessor")
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
        var configurator = new RangeConfigurator(name, supplier, consumer, defaultValue(field, ReflectionUtils.getRawType(field.getGenericType())), forceUpdate);
        if (field.isAnnotationPresent(ConfigNumber.class)) {
            ConfigNumber range = field.getAnnotation(ConfigNumber.class);
            configurator = configurator.setRange(range.range()[0], range.range()[1]).setWheel(range.wheel());
        }
        return configurator;
    }
}
