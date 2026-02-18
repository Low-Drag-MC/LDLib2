package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.RegistrySearchComponent;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name = "fluid", registry = "ldlib2:configurator_accessor")
public class FluidAccessor extends TypesAccessor<Fluid> {

    public FluidAccessor() {
        super(Fluid.class);
    }

    @Override
    public Fluid defaultValue(@Nullable Field field, @Nullable Class<?> type) {
        if (field != null && field.isAnnotationPresent(DefaultValue.class)) {
            return BuiltInRegistries.FLUID.get(ResourceLocation.parse(field.getAnnotation(DefaultValue.class).stringValue()[0]));
        }
        return Fluids.WATER;
    }

    @Override
    public Configurator create(String name, Supplier<Fluid> supplier, Consumer<Fluid> consumer, boolean forceUpdate, @Nullable Field field, @Nullable Object owner) {
        return new RegistrySearchComponent.Fluid(name, supplier, consumer, defaultValue(field), forceUpdate);
    }
}
