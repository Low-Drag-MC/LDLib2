package com.lowdragmc.lowdraglib.editor.accessors;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.client.renderer.impl.IModelRenderer;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib.editor.annotation.DefaultValue;
import com.lowdragmc.lowdraglib.editor.configurator.Configurator;
import com.lowdragmc.lowdraglib.editor.configurator.IRendererConfigurator;
import com.lowdragmc.lowdraglib.utils.ReflectionUtils;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name = "renderer", registry = "ldlib:configurator_accessor")
public class IRendererAccessor extends TypesAccessor<IRenderer> {

    public IRendererAccessor() {
        super(IRenderer.class);
    }

    @Override
    public IRenderer defaultValue(Field field, Class<?> type) {
        if (field.isAnnotationPresent(DefaultValue.class)) {
            return new IModelRenderer(ResourceLocation.parse(field.getAnnotation(DefaultValue.class).stringValue()[0]));
        }
        return IRenderer.EMPTY;
    }

    @Override
    public Configurator create(String name, Supplier<IRenderer> supplier, Consumer<IRenderer> consumer, boolean forceUpdate, Field field) {
        return new IRendererConfigurator(name, supplier, consumer, defaultValue(field, ReflectionUtils.getRawType(field.getGenericType())), forceUpdate);
    }
}
