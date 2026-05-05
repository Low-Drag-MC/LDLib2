package com.lowdragmc.lowdraglib2.nodegraphtookit.api.type;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.utils.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Objects;

public final class TypeHandle implements Comparable<TypeHandle> {
    private final String identification;

    // lazy caches (not serialized)
    private transient String nameCache;
    private transient String friendlyNameCache;
    private transient Type typeCache;
    private transient IGuiTexture iconCache;
    private transient int colorCache;
    private transient ITypeConfigurable configurableCache;
    private transient Object defaultValueCache;

    private TypeHandle(String identification) {
        this.identification = TypeHandleHelpers.convertTypeName(identification);
    }

    public static TypeHandle create(String identification) {
        return new TypeHandle(identification);
    }

    public boolean isValid() {
        return identification != null && !identification.isEmpty();
    }

    public String getIdentification() {
        return identification;
    }

    /**
     * Unity: Name => custom? Identification : Resolve().Name
     */
    public String getName() {
        if (nameCache != null) return nameCache;

        if (TypeHandleHelpers.isCustomTypeHandle(this)) {
            nameCache = identification;
        } else {
            Type resolved = TypeHandleHelpers.resolveType(this);
            if (resolved instanceof Class<?> c) {
                nameCache = c.getName();
            } else {
                nameCache = (resolved != null) ? resolved.getTypeName() : identification;
            }
        }
        return nameCache;
    }

    /**
     * Unity: FriendlyName => internal override else custom? Identification : friendly(Resolve())
     */
    public String getFriendlyName() {
        if (friendlyNameCache != null) return friendlyNameCache;

        String internal = TypeHandleHelpers.getFriendlyNameInternal(this);
        if (internal != null) {
            friendlyNameCache = internal;
            return friendlyNameCache;
        }

        if (TypeHandleHelpers.isCustomTypeHandle(this)) {
            friendlyNameCache = identification;
        } else {
            Type resolved = TypeHandleHelpers.resolveType(this);
            friendlyNameCache = TypeHandleHelpers.friendlyNameOf(resolved);
        }
        return friendlyNameCache;
    }

    public Type resolve() {
        if (typeCache == null) {
            typeCache = TypeHandleHelpers.resolveType(this);
        }
        return typeCache;
    }

    public IGuiTexture getIcon() {
        if (iconCache == null) {
            iconCache = TypeHandleHelpers.resolveIcon(this);
        }
        return iconCache;
    }

    public Object getDefaultValue() {
        if (defaultValueCache == null) {
            defaultValueCache = TypeHandleHelpers.resolveDefaultValue(this).get();
        }
        return defaultValueCache;
    }

    public int getTypeColor() {
        if (colorCache == 0) {
            colorCache = TypeHandleHelpers.resolveColor(this);
        }
        return colorCache;
    }

    public ITypeConfigurable resolveConfigurable() {
        if (configurableCache == null) {
            configurableCache = TypeHandleHelpers.resolveConfigurable(this);
        }
        return configurableCache;
    }

    public boolean isAssignableFrom(TypeHandle other) {
        var selfType = resolve();
        var otherType = resolve();
        return TypeUtils.isAssignableFrom(selfType, otherType);
    }

    @Override
    public int compareTo(@NotNull TypeHandle other) {
        return this.identification.compareTo(other.identification);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TypeHandle that)) return false;
        return Objects.equals(this.identification, that.identification);
    }

    @Override
    public int hashCode() {
        return (identification != null) ? identification.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "TypeName:" + identification;
    }

    public boolean isCustomTypeHandle() {
        return TypeHandleHelpers.isCustomTypeHandle(this);
    }
}
