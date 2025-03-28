package com.lowdragmc.lowdraglib.editor.accessors;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib.editor.annotation.DefaultValue;
import com.lowdragmc.lowdraglib.editor.configurator.Configurator;
import com.lowdragmc.lowdraglib.editor.configurator.FluidConfigurator;
import com.lowdragmc.lowdraglib.utils.ReflectionUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name = "fluid", registry = "ldlib:configurator_accessor")
public class FluidAccessor extends TypesAccessor<Fluid> {

    public FluidAccessor() {
        super(Fluid.class);
    }

    @Override
    public Fluid defaultValue(Field field, Class<?> type) {
        if (field.isAnnotationPresent(DefaultValue.class)) {
            var annotation = field.getAnnotation(DefaultValue.class);
            if (annotation.stringValue().length > 0) {
                return BuiltInRegistries.FLUID.get(ResourceLocation.parse(annotation.stringValue()[0]));
            }
        }
        return Fluids.EMPTY;
    }

    @Override
    public Configurator create(String name, Supplier<Fluid> supplier, Consumer<Fluid> consumer, boolean forceUpdate, Field field) {
        return new FluidConfigurator(name, supplier, consumer, defaultValue(field, ReflectionUtils.getRawType(field.getGenericType())), forceUpdate);
    }

}
