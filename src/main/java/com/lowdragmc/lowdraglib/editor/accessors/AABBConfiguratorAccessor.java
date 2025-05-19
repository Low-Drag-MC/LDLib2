package com.lowdragmc.lowdraglib.editor.accessors;

import com.lowdragmc.lowdraglib.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib.editor.configurator.AABBConfigurator;
import com.lowdragmc.lowdraglib.editor.configurator.Configurator;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegisterClient;
import net.minecraft.world.phys.AABB;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name = "aabb", registry = "ldlib:configurator_accessor")
public class AABBConfiguratorAccessor extends TypesAccessor<AABB>{

    public AABBConfiguratorAccessor() {
        super(AABB.class);
    }

    @Override
    public AABB defaultValue(Field field, Class<?> type) {
        if (field.isAnnotationPresent(DefaultValue.class)) {
            var annotation = field.getAnnotation(DefaultValue.class);
            return new AABB(annotation.numberValue()[0], annotation.numberValue()[1], annotation.numberValue()[2], annotation.numberValue()[3], annotation.numberValue()[4], annotation.numberValue()[5]);
        }
        return new AABB(0, 0, 0, 1, 1, 1);
    }

    @Override
    public Configurator create(String name, Supplier<AABB> supplier, Consumer<AABB> consumer, boolean forceUpdate, Field field) {
        return new AABBConfigurator(name, supplier, consumer, defaultValue(field, AABB.class), forceUpdate);
    }
}
