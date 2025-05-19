package com.lowdragmc.lowdraglib.configurator.accessors;

import com.lowdragmc.lowdraglib.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib.configurator.ui.ColorConfigurator;
import com.lowdragmc.lowdraglib.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name = "number", registry = "ldlib:configurator_accessor")
public class NumberAccessor extends TypesAccessor<Number> {

    public NumberAccessor() {
        super(int.class, long.class, float.class, byte.class, double.class, short.class, Integer.class, Long.class, Float.class, Byte.class, Double.class, Short.class);
    }

    @Override
    public Number defaultValue(Field field, Class<?> type) {
        Number number = 0;
        if (field.isAnnotationPresent(ConfigNumber.class)) {
            var configNumber = field.getAnnotation(ConfigNumber.class);
            number = configNumber.range()[0];
            if (field.isAnnotationPresent(DefaultValue.class)) {
                number = field.getAnnotation(DefaultValue.class).numberValue()[0];
            }
            if (configNumber.type() == ConfigNumber.Type.AUTO) {
                if (type == int.class || type == Integer.class) {
                    return number.intValue();
                } else if (type == byte.class || type == Byte.class) {
                    return number.byteValue();
                } else if (type == long.class || type == Long.class) {
                    return number.longValue();
                } else if (type == float.class || type == Float.class) {
                    return number.floatValue();
                } else if (type == double.class || type == Double.class) {
                    return number.doubleValue();
                } else if (type == short.class || type == Short.class) {
                    return number.shortValue();
                }
            } else if (configNumber.type() == ConfigNumber.Type.INTEGER) {
                return number.intValue();
            } else if (configNumber.type() == ConfigNumber.Type.LONG) {
                return number.longValue();
            } else if (configNumber.type() == ConfigNumber.Type.FLOAT) {
                return number.floatValue();
            } else if (configNumber.type() == ConfigNumber.Type.DOUBLE) {
                return number.doubleValue();
            } else if (configNumber.type() == ConfigNumber.Type.SHORT) {
                return number.shortValue();
            } else if (configNumber.type() == ConfigNumber.Type.BYTE) {
                return number.byteValue();
            }
        }

        if (field.isAnnotationPresent(DefaultValue.class)) {
            number = field.getAnnotation(DefaultValue.class).numberValue()[0];
        }

        return number;
    }

    @Override
    public Configurator create(String name, Supplier<Number> supplier, Consumer<Number> consumer, boolean forceUpdate, Field field) {
        if (field.isAnnotationPresent(ConfigColor.class)) {
            return new ColorConfigurator(name, () -> supplier.get().intValue(), consumer::accept, defaultValue(field, ReflectionUtils.getRawType(field.getGenericType())).intValue(), forceUpdate);
        }
        var configurator = new NumberConfigurator(name, supplier, consumer, defaultValue(field, ReflectionUtils.getRawType(field.getGenericType())), forceUpdate);
        if (field.isAnnotationPresent(ConfigNumber.class)) {
            ConfigNumber range = field.getAnnotation(ConfigNumber.class);
            configurator = configurator
                    .setRange(range.range()[0], range.range()[1])
                    .setWheel(range.wheel());
        }
        return configurator;
    }
}
