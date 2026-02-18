package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.RegistrySearchComponent;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name = "entity_type", registry = "ldlib2:configurator_accessor")
public class EntityTypeAccessor extends TypesAccessor<EntityType<?>> {
    public EntityTypeAccessor() {
        super(EntityType.class);
    }

    @Override
    public EntityType<?> defaultValue(@Nullable Field field, @Nullable Class<?> type) {
        if (field != null && field.isAnnotationPresent(DefaultValue.class)) {
            return BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(field.getAnnotation(DefaultValue.class).stringValue()[0]));
        }
        return EntityType.PIG;
    }

    @Override
    public Configurator create(String name, Supplier<EntityType<?>> supplier, Consumer<EntityType<?>> consumer, boolean forceUpdate, @Nullable Field field, @Nullable Object owner) {
        return new RegistrySearchComponent.EntityType(name, supplier, consumer, defaultValue(field), forceUpdate);
    }
}
