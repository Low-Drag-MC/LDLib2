package com.lowdragmc.lowdraglib2.editor_outdated.accessors;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.editor_outdated.configurator.BlockConfigurator;
import com.lowdragmc.lowdraglib2.editor_outdated.configurator.Configurator;
import com.lowdragmc.lowdraglib2.utils.ReflectionUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name = "block", registry = "ldlib2:configurator_accessor")
public class BlockAccessor extends TypesAccessor<Block> {

    public BlockAccessor() {
        super(Block.class);
    }

    @Override
    public Block defaultValue(Field field, Class<?> type) {
        if (field.isAnnotationPresent(DefaultValue.class)) {
            var annotation = field.getAnnotation(DefaultValue.class);
            if (annotation.stringValue().length > 0) {
                return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(annotation.stringValue()[0]));
            }
        }
        return Blocks.AIR;
    }

    @Override
    public Configurator create(String name, Supplier<Block> supplier, Consumer<Block> consumer, boolean forceUpdate, Field field) {
        return new BlockConfigurator(name, supplier, consumer, defaultValue(field, ReflectionUtils.getRawType(field.getGenericType())), forceUpdate);
    }

}
