package com.lowdragmc.lowdraglib2.editor.resource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.io.File;

public sealed interface IResourcePath permits BuiltinPath, FilePath {
    Codec<IResourcePath> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("built-in").forGetter(IResourcePath::isBuiltin),
            Codec.STRING.fieldOf("path").forGetter(IResourcePath::getPath)
    ).apply(instance, (builtin, path) -> {
        if (builtin) {
            return new BuiltinPath(path);
        } else {
            return new FilePath(new File(path));
        }
    }) );

    boolean isBuiltin();

    String getPath();

    /**
     * Get the resource name.
     */
    String getResourceName();
}
