package com.lowdragmc.lowdraglib.editor.accessors;

import com.lowdragmc.lowdraglib.editor.annotation.ConfigAccessor;
import com.lowdragmc.lowdraglib.editor.annotation.DefaultValue;
import com.lowdragmc.lowdraglib.editor.configurator.Configurator;
import com.lowdragmc.lowdraglib.editor.configurator.GuiTextureConfigurator;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/1
 * @implNote NumberAccessor
 */
@ConfigAccessor
public class GuiTextureAccessor extends TypesAccessor<IGuiTexture> {

    public GuiTextureAccessor() {
        super(IGuiTexture.class);
    }

    @Override
    public IGuiTexture defaultValue(Field field, Class<?> type) {
        if (field.isAnnotationPresent(DefaultValue.class)) {
            return new ResourceTexture(field.getAnnotation(DefaultValue.class).stringValue()[0]);
        }
        return IGuiTexture.EMPTY;
    }

    @Override
    public Configurator create(String name, Supplier<IGuiTexture> supplier, Consumer<IGuiTexture> consumer, boolean forceUpdate, Field field) {
        return new GuiTextureConfigurator(name, supplier, consumer, forceUpdate);
    }
}
