package com.lowdragmc.lowdraglib2.client.renderer;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * A Fabric proxy for NeoForge's ModelData system.
 */
public class ModelData {
    public static final ModelData EMPTY = new ModelData(Map.of());

    private final Map<ModelProperty<?>, Object> properties;

    private ModelData(Map<ModelProperty<?>, Object> properties) {
        this.properties = properties;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(ModelProperty<T> prop) {
        return (T) properties.get(prop);
    }

    public boolean has(ModelProperty<?> prop) {
        return properties.containsKey(prop);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<ModelProperty<?>, Object> properties = new IdentityHashMap<>();

        public <T> Builder with(ModelProperty<T> prop, T data) {
            properties.put(prop, data);
            return this;
        }

        public ModelData build() {
            return new ModelData(properties);
        }
    }
}
