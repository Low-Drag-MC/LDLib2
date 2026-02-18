package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigRL;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.TagKeySearchComponent;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.*;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/3
 * @implNote ResourceLocationAccessor
 */
@LDLRegisterClient(name = "resource_location", registry = "ldlib2:configurator_accessor")
public class ResourceLocationAccessor extends TypesAccessor<ResourceLocation> {

    public ResourceLocationAccessor() {
        super(ResourceLocation.class);
    }

    @Override
    public ResourceLocation defaultValue(@Nullable Field field, @Nullable Class<?> type) {
        if (field != null && field.isAnnotationPresent(DefaultValue.class)) {
            return ResourceLocation.parse(field.getAnnotation(DefaultValue.class).stringValue()[0]);
        }
        return LDLib2.id("default");
    }

    @Override
    public Configurator create(String name, Supplier<ResourceLocation> supplier, Consumer<ResourceLocation> consumer, boolean forceUpdate, @Nullable Field field, @Nullable Object owner) {
        if (field != null && field.isAnnotationPresent(ConfigRL.class)) {
            var rlConfig = field.getAnnotation(ConfigRL.class);
            return switch (rlConfig.value()) {
                case FONT -> new SearchComponentConfigurator<>(name, supplier, consumer, defaultValue(field, String.class), forceUpdate,
                        (word, handler) -> {
                            var search = word.toLowerCase();
                            for (var fontName : Minecraft.getInstance().fontManager.fontSets.keySet()) {
                                if (Thread.currentThread().isInterrupted()) return;
                                if (fontName.toString().contains(search)) {
                                    handler.accept(fontName);
                                }
                            }
                        }, ResourceLocation::toString, UIElementProvider.text(font -> font == null ?
                        Component.literal("---") : Component.literal(font.toString()))
                );
                case ITEM_TAG_KEY -> new TagKeySearchComponent.Item(name,
                        () -> ItemTags.create(supplier.get()), tagKey -> consumer.accept(tagKey.location()),
                        ItemTags.create(defaultValue(field, ResourceLocation.class)),
                        forceUpdate
                );
                case BLOCK_TAG_KEY -> new TagKeySearchComponent.Block(name,
                        () -> BlockTags.create(supplier.get()), tagKey -> consumer.accept(tagKey.location()),
                        BlockTags.create(defaultValue(field, ResourceLocation.class)),
                        forceUpdate
                );
                case FLUID_TAG_KEY -> new TagKeySearchComponent.Fluid(name,
                        () -> FluidTags.create(supplier.get()), tagKey -> consumer.accept(tagKey.location()),
                        FluidTags.create(defaultValue(field, ResourceLocation.class)),
                        forceUpdate
                );
                case ENTITY_TYPE_TAG_KEY -> new TagKeySearchComponent.EntityType(name,
                        () -> TagKey.create(Registries.ENTITY_TYPE, supplier.get()), tagKey -> consumer.accept(tagKey.location()),
                        TagKey.create(Registries.ENTITY_TYPE, defaultValue(field, ResourceLocation.class)),
                        forceUpdate
                );
            };
        }
        var configurator = new StringConfigurator(name,
                () -> supplier.get().toString(),
                s -> consumer.accept(ResourceLocation.parse(s)),
                defaultValue(field, String.class).toString(),
                forceUpdate).setResourceLocation(true);
        configurator.setPastable(String.class, pasted -> {
            if (pasted != null && LDLib2.isValidResourceLocation(pasted)) {
                consumer.accept(ResourceLocation.parse(pasted));
                configurator.notifyChanges();
            }
        });
        return configurator;
    }
}
