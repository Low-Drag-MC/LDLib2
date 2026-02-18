package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.TransformRefConfigurator;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.TransformRef;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name = "transform_ref", registry = "ldlib2:configurator_accessor")
public class TransformRefAccessor extends TypesAccessor<TransformRef> {

    public TransformRefAccessor() {
        super(TransformRef.class);
    }

    @Override
    public TransformRef defaultValue(@Nullable Field field, @Nullable Class<?> type) {
        if (field != null && field.isAnnotationPresent(DefaultValue.class)) {
            return new TransformRef(UUID.fromString(field.getAnnotation(DefaultValue.class).stringValue()[0]));
        }
        return new TransformRef();
    }

    @Override
    public Configurator create(String name, Supplier<TransformRef> supplier, Consumer<TransformRef> consumer, boolean forceUpdate, @Nullable Field field, @Nullable Object owner) {
        return new TransformRefConfigurator(name, supplier, consumer, defaultValue(field, TransformRef.class), forceUpdate);
    }
}
