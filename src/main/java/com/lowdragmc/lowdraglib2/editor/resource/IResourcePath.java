package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public interface IResourcePath {
    Codec<IResourcePath> V0 = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("built-in").forGetter(path -> path.getType() == BuiltinResourceProvider.TYPE),
            Codec.STRING.fieldOf("path").forGetter(IResourcePath::getPath)
    ).apply(instance, (builtin, path) -> {
        if (builtin) {
            return new BuiltinPath(path);
        } else {
            return new FilePath(path);
        }
    }));

    Codec<IResourcePath> V1 = RecordCodecBuilder.create(instance -> instance.group(
            LDLib2Registries.RESOURCE_PROVIDER_TYPES.codec().fieldOf("type").forGetter(IResourcePath::getType),
            Codec.STRING.fieldOf("path").forGetter(IResourcePath::getPath)
    ).apply(instance, ResourceProviderType::createFullPath));

    Codec<IResourcePath> CODEC = Codec.either(V1, V0).xmap(
            e -> e.map(v1 -> v1, v0 -> v0),
            Either::left
    );

    ResourceProviderType getType();

    String getPath();

    /**
     * Get the resource name.
     */
    String getResourceName();
}
