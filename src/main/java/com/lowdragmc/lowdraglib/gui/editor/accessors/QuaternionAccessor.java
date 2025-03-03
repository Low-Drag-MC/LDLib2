package com.lowdragmc.lowdraglib.gui.editor.accessors;

import com.lowdragmc.lowdraglib.gui.editor.annotation.ConfigAccessor;
import com.lowdragmc.lowdraglib.gui.editor.annotation.DefaultValue;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.editor.configurator.Configurator;
import com.lowdragmc.lowdraglib.gui.editor.configurator.Vector3Configurator;
import com.lowdragmc.lowdraglib.utils.ReflectionUtils;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@ConfigAccessor
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
    public Configurator create(String name, Supplier<Quaternionf> supplier, Consumer<Quaternionf> consumer, boolean forceUpdate, Field field) {
        var configurator = new Vector3Configurator(name,
                () -> supplier.get().getEulerAnglesXYZ(new Vector3f()).mul(57.29577951308232f),
                v -> {
                    var q = new Quaternionf();
                    q.rotateXYZ(
                            (float) Math.toRadians(v.x),
                            (float) Math.toRadians(v.y),
                            (float) Math.toRadians(v.z));
                    consumer.accept(q);
                },
                defaultValue(field, ReflectionUtils.getRawType(field.getGenericType())).getEulerAnglesXYZ(new Vector3f()), forceUpdate);
        if (field.isAnnotationPresent(NumberRange.class)) {
            NumberRange range = field.getAnnotation(NumberRange.class);
            configurator = configurator.setRange(range.range()[0], range.range()[1]).setWheel(range.wheel());
        }
        return configurator;
    }

}
