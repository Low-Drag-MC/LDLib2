package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.utils.LDLibExtraCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;
import java.util.regex.Pattern;

public interface IResourcePath {
    Pattern PATH_WITH_TYPE_PATTERN = Pattern.compile("^([a-zA-Z0-9-_]+)\\((.+)\\)$");

    @Deprecated(since = "1.22")
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

    @Deprecated(since = "1.22")
    Codec<IResourcePath> V1 = RecordCodecBuilder.create(instance -> instance.group(
            LDLib2Registries.RESOURCE_PROVIDER_TYPES.codec().fieldOf("type").forGetter(IResourcePath::getType),
            Codec.STRING.fieldOf("path").forGetter(IResourcePath::getPath)
    ).apply(instance, ResourceProviderType::createFullPath));

    Codec<IResourcePath> V2 = Codec.STRING.xmap(
            IResourcePath::parse,
            IResourcePath::getPathWithType
    );

    Codec<IResourcePath> CODEC = LDLibExtraCodecs.compat(V2, V1, V0);

    /**
     * Retrieves the type of the resource provider associated with this resource path.
     *
     * @return the {@link ResourceProviderType} representing the type of the resource provider.
     */
    ResourceProviderType getType();

    /**
     * Retrieves the path associated with this resource.
     *
     * @return the string representation of the path.
     */
    String getPath();

    /**
     * Get the resource name.
     */
    String getResourceName();

    /**
     * Constructs a string representation of the resource path including its type.
     * The format of the returned string is "{typeName}({path})", where "typeName" is obtained
     * from the resource provider type's name and "path" is the resource path.
     * for example {@code builtin(builtin:missing)}, {@code file(/textures/test.png)}
     *
     * @return a formatted string representing the type and path of the resource.
     */
    default String getPathWithType() {
        return "%s(%s)".formatted(getType().getTypeName(), getPath());
    }

    /**
     * Parses a string representing a resource path with its associated type
     * and constructs an {@link IResourcePath} object.
     * The input string must follow the format "{typeName}({path})", where "typeName"
     * specifies the resource provider type and "path" is the corresponding resource path.
     *
     * If the provided string does not match the required format, or if the type
     * specified in the string is unrecognized, this method will return {@code null}.
     *
     * @param pathWithType the input string containing the type and path information.
     *                     It should not be {@code null} and must conform to the expected format.
     *                     Leading and trailing whitespace will be trimmed.
     * @return an {@code IResourcePath} object representing the parsed type and path,
     *         or {@code null} if the string is invalid, unrecognized, or {@code null}.
     */
    @Nullable
    static IResourcePath parse(String pathWithType) {
        if (pathWithType == null) {
            return null;
        }
        pathWithType = pathWithType.trim();
        var matcher = PATH_WITH_TYPE_PATTERN.matcher(pathWithType);
        if (matcher.matches()) {
            var type = matcher.group(1);
            var resourcePath = matcher.group(2);
            var resourceType = LDLib2Registries.RESOURCE_PROVIDER_TYPES.get(type);
            if (resourceType != null) {
                return resourceType.createFullPath(resourcePath);
            } else if (type.equals("pack")) {
                return new FilePath(ResourceLocation.parse(resourcePath));
            }
        }
        return null;
    }
}
