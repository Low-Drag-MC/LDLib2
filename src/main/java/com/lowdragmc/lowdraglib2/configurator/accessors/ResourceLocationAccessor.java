package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigFont;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

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
    public ResourceLocation defaultValue(Field field, Class<?> type) {
        if (field.isAnnotationPresent(DefaultValue.class)) {
            return ResourceLocation.parse(field.getAnnotation(DefaultValue.class).stringValue()[0]);
        }
        return LDLib2.id("default");
    }

    @Override
    public Configurator create(String name, Supplier<ResourceLocation> supplier, Consumer<ResourceLocation> consumer, boolean forceUpdate, Field field, Object owner) {
        if (field.isAnnotationPresent(ConfigFont.class)) {
            return new SearchComponentConfigurator<>(name, supplier, consumer, defaultValue(field, String.class), forceUpdate,
                    (word, handler) -> {
                        var search = word.toLowerCase();
                        for (var fontName : Minecraft.getInstance().fontManager.fontSets.keySet()) {
                            if (Thread.currentThread().isInterrupted()) return;
                            if (fontName.toString().contains(search)) {
                                handler.accept(fontName);
                            }
                        }
                    }, ResourceLocation::toString, ResourceLocation::toString
            );
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
