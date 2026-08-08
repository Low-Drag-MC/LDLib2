package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.Platform;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.Nullable;
import java.io.File;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class FilePath implements IResourcePath {
    @Getter
    @EqualsAndHashCode.Include
    public final String path;
    @Getter
    public final File file;
    @Nullable
    @Getter
    public final Identifier location;

    public static String normalizePath(String path) {
        if (path == null) return null;
        return path.replace('\\', '/')
                .replaceAll("/+", "/")
                .replaceAll("/$", "");
    }

    /**
     * The canonical, portable IDENTITY of a resource file path: relative to the game directory,
     * anchored with a leading {@code ./}. It is what {@link #path} stores and what equality/serialization
     * use. Editor references are saved with this string; the resource panel enumerates files under an
     * ABSOLUTE game dir (some launchers force one). Both — plus any legacy absolute save — must collapse
     * to the SAME string so a saved reference and the panel resolve to ONE cached resource instance
     * (otherwise editing a texture/renderer/... in the panel doesn't affect a project referencing it).
     * <p>Examples (all → {@code ./ldlib2/assets/ldlib2/resources/global/x.nbt}):
     * {@code C:/.../run/ldlib2/assets/ldlib2/resources/global/x.nbt}, {@code ./ldlib2/assets/...},
     * a legacy absolute save from another machine.
     */
    public static String toGameRelative(String rawPath) {
        var normalized = normalizePath(rawPath);
        if (normalized == null) return null;
        // already canonical. resolving it again would go through the process CWD, which is not
        // guaranteed to be the game dir, so keep it as is.
        if (normalized.startsWith("./")) return normalized;
        // 1) precise: strip the actual game-dir prefix (robust even if the game dir itself contains "ldlib2")
        try {
            var abs = new File(normalized).getAbsoluteFile().toPath().normalize();
            var gameDir = Platform.getGamePath().toAbsolutePath().normalize();
            if (abs.startsWith(gameDir) && !abs.equals(gameDir)) {
                return normalizePath("./" + gameDir.relativize(abs));
            }
        } catch (Exception ignored) {
        }
        // 2) fallback (legacy / cross-machine absolute saves): anchor at the ldlib2 assets marker
        var idx = normalized.indexOf("ldlib2/assets/");
        if (idx >= 0) {
            return "./" + normalized.substring(idx);
        }
        return normalized;
    }

    /**
     * Resolve a (possibly game-relative {@code ./...}) path to an ABSOLUTE file for I/O, so access
     * works regardless of the process working directory.
     */
    public static File resolveFile(String path) {
        if (path != null && path.startsWith("./")) {
            try {
                return Platform.getGamePath().resolve(path.substring(2)).toFile();
            } catch (Exception ignored) {
            }
        }
        return new File(path);
    }

    public FilePath(String path) {
        this.path = toGameRelative(path);
        this.file = resolveFile(this.path);
        this.location = toResourceLocation();
    }

    public FilePath(File file) {
        this.path = toGameRelative(file.getPath());
        // keep the given (absolute) file for I/O; resolve if a relative file was somehow passed
        this.file = file.isAbsolute() ? file : resolveFile(this.path);
        this.location = toResourceLocation();
    }

    public FilePath(Identifier location) {
        this.path = "assets/" + location.getNamespace() + "/" + location.getPath();
        this.file = new File(this.path);
        this.location = location;
    }

    @Override
    public ResourceProviderType getType() {
        return FileResourceProvider.TYPE;
    }

    @Override
    public String getResourceName() {
        var name = file.getName();
        var dotIndex = name.lastIndexOf('.');
        return (dotIndex == -1) ? name : name.substring(0, dotIndex);
    }

    @Nullable
    private Identifier toResourceLocation() {
        var assetsIndex = path.indexOf("assets");
        if (assetsIndex == -1) return null;
        
        if (assetsIndex + 7 >= path.length() || path.charAt(assetsIndex + 6) != '/') {
            return null;
        }
        
        var remainPath = path.substring(assetsIndex + 7);
        var firstSlash = remainPath.indexOf('/');
        if (firstSlash == -1) {
            return null;
        }
        var namespace = remainPath.substring(0, firstSlash);
        var resourcePath = remainPath.substring(firstSlash + 1);
        
        if (namespace.isEmpty() || resourcePath.isEmpty()) {
            return null;
        }
        
        try {
            return Identifier.fromNamespaceAndPath(namespace, resourcePath);
        } catch (Exception e) {
            return null;
        }
    }
}