package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.RegistrySearchComponent;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name = "item", registry = "ldlib2:configurator_accessor")
public class ItemAccessor extends TypesAccessor<Item> {

    public ItemAccessor() {
        super(Item.class);
    }

    @Override
    public Item defaultValue(@Nullable Field field, @Nullable Class<?> type) {
        if (field != null && field.isAnnotationPresent(DefaultValue.class)) {
            return BuiltInRegistries.ITEM.get(ResourceLocation.parse(field.getAnnotation(DefaultValue.class).stringValue()[0]));
        }
        return Items.AIR;
    }

    @Override
    public Configurator create(String name, Supplier<Item> supplier, Consumer<Item> consumer, boolean forceUpdate, @Nullable Field field, @Nullable Object owner) {
        return new RegistrySearchComponent.Item(name, supplier, consumer, defaultValue(field), forceUpdate);
    }
}
