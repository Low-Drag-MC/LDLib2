package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.client.renderer.impl.IModelRenderer;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.IRendererConfigurator;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name = "renderer", registry = "ldlib2:configurator_accessor")
public class IRendererAccessor extends TypesAccessor<IRenderer> {

    public IRendererAccessor() {
        super(IRenderer.class);
    }

    @Override
    public IRenderer defaultValue(@Nullable Field field, @Nullable Class<?> type) {
        if (field != null && field.isAnnotationPresent(DefaultValue.class)) {
            return new IModelRenderer(ResourceLocation.parse(field.getAnnotation(DefaultValue.class).stringValue()[0]));
        }
        return IRenderer.EMPTY;
    }

    @Override
    public Configurator create(String name, Supplier<IRenderer> supplier, Consumer<IRenderer> consumer, boolean forceUpdate, @Nullable Field field, @Nullable Object owner) {
        return new IRendererConfigurator(name, supplier, consumer, defaultValue(field), forceUpdate);
    }
}
