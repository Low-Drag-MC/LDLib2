package com.lowdragmc.lowdraglib.editor.accessors;

import com.lowdragmc.lowdraglib.editor.annotation.ConfigAccessor;
import com.lowdragmc.lowdraglib.editor.annotation.DefaultValue;
import com.lowdragmc.lowdraglib.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.editor.configurator.Configurator;
import com.lowdragmc.lowdraglib.editor.configurator.Vector3Configurator;
import com.lowdragmc.lowdraglib.utils.ReflectionUtils;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2023/5/27
 * @implNote Vector3Accessor
 */
@ConfigAccessor
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
        var configurator = new Vector3Configurator(name, supplier, consumer, defaultValue(field, ReflectionUtils.getRawType(field.getGenericType())), forceUpdate);
        if (field.isAnnotationPresent(NumberRange.class)) {
            NumberRange range = field.getAnnotation(NumberRange.class);
            configurator = configurator.setRange(range.range()[0], range.range()[1]).setWheel(range.wheel());
        }
        return configurator;
    }

}
