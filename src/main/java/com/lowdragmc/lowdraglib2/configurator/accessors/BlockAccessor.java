package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.RegistrySearchComponent;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name = "block", registry = "ldlib2:configurator_accessor")
public class BlockAccessor extends TypesAccessor<Block> {

    public BlockAccessor() {
        super(Block.class);
    }

    @Override
    public Block defaultValue(@Nullable Field field, @Nullable Class<?> type) {
        if (field != null && field.isAnnotationPresent(DefaultValue.class)) {
            return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(field.getAnnotation(DefaultValue.class).stringValue()[0]));
        }
        return Blocks.AIR;
    }

    @Override
    public Configurator create(String name, Supplier<Block> supplier, Consumer<Block> consumer, boolean forceUpdate, @Nullable Field field, @Nullable Object owner) {
        return new RegistrySearchComponent.Block(name, supplier, consumer, defaultValue(field), forceUpdate);
    }
}
