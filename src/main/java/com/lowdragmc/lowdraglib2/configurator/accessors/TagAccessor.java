package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.TagConfigurator;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.*;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name = "tag", registry = "ldlib2:configurator_accessor")
public class TagAccessor extends TypesAccessor<Tag> {

    public TagAccessor() {
        super(Tag.class, CompoundTag.class, ListTag.class);
    }

    @Override
    public Tag defaultValue(@Nullable Field field, @Nullable Class<?> type) {
        if (field != null && field.isAnnotationPresent(DefaultValue.class)) {
            try {
                return new TagParser(new StringReader(field.getAnnotation(DefaultValue.class).stringValue()[0])).readValue();
            } catch (CommandSyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        return EndTag.INSTANCE;
    }

    @Override
    public Configurator create(String name, Supplier<Tag> supplier, Consumer<Tag> consumer, boolean forceUpdate, @Nullable Field field, @Nullable Object owner) {
        var type = field == null ? null : field.getType();
        TagConfigurator tagConfigurator;
        if (type == CompoundTag.class) {
            tagConfigurator = new TagConfigurator(name, supplier,
                    tag -> consumer.accept(tag instanceof CompoundTag compoundTag ? compoundTag : new CompoundTag()),
                    defaultValue(field, field.getType()), forceUpdate);
            tagConfigurator.tagField.setCompoundTagOnly();
        } else if (type == ListTag.class) {
            tagConfigurator = new TagConfigurator(name, supplier,
                    tag -> consumer.accept(tag instanceof ListTag listTag ? listTag : new ListTag()),
                    defaultValue(field, field.getType()), forceUpdate);
            tagConfigurator.tagField.setListOnly();
        } else {
            tagConfigurator = new TagConfigurator(name, supplier, consumer, defaultValue(field, type), forceUpdate);
        }
        return tagConfigurator;
    }
}
