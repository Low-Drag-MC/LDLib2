package com.lowdragmc.lowdraglib2.editor.resource;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class UniqueResourceManager<T> {
    private final Map<UUID, T> resources = new ConcurrentHashMap<>();

    public T getOrCreateResource(UUID uuid, Supplier<T> supplier) {
        return resources.computeIfAbsent(uuid, k -> supplier.get());
    }

    @Nullable
    public T getResource(UUID uuid) {
        return resources.get(uuid);
    }

    public boolean hasResource(UUID uuid) {
        return resources.containsKey(uuid);
    }

    public void addResource(UUID uuid, T resource) {
        resources.put(uuid, resource);
    }
}
