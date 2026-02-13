package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.IGuiTextureConfigurator;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name = "gui_texture", registry = "ldlib2:configurator_accessor")
public class IGuiTextureAccessor extends TypesAccessor<IGuiTexture> {

    public IGuiTextureAccessor() {
        super(IGuiTexture.class);
    }

    @Override
    public IGuiTexture defaultValue(@Nullable Field field, @Nullable Class<?> type) {
        if (field != null && field.isAnnotationPresent(DefaultValue.class)) {
            return SpriteTexture.of(field.getAnnotation(DefaultValue.class).stringValue()[0]);
        }
        return IGuiTexture.EMPTY;
    }

    @Override
    public Configurator create(String name, Supplier<IGuiTexture> supplier, Consumer<IGuiTexture> consumer, boolean forceUpdate, @Nullable Field field, @Nullable Object owner) {
        return new IGuiTextureConfigurator(name, supplier, consumer, defaultValue(field), forceUpdate);
    }
}
