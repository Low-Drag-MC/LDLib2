package com.lowdragmc.lowdraglib.editor.accessors;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib.editor.configurator.Configurator;
import com.lowdragmc.lowdraglib.editor.configurator.Vector3Configurator;
import com.lowdragmc.lowdraglib.utils.ReflectionUtils;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name = "quaternion", registry = "ldlib:configurator_accessor")
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
        if (field.isAnnotationPresent(ConfigNumber.class)) {
            ConfigNumber range = field.getAnnotation(ConfigNumber.class);
            configurator = configurator.setRange(range.range()[0], range.range()[1]).setWheel(range.wheel());
        }
        return configurator;
    }

}
