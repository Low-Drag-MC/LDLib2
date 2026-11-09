package com.lowdragmc.lowdraglib2.editor.resource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

/**
 * @author KilaBash
 * @date 2022/12/2
 * @implNote Resource
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Resources {
    public static final Resources EMPTY = new Resources(List.of());

    public final ImmutableList<Resource<?>> resources;
    /**
     * The group each grouped resource is shown under. The project's to say, not the resource's: the same type can sit
     * in different groups in different editors.
     */
    private final ImmutableMap<Resource<?>, ResourceGroup> groups;

    public Resources(List<Resource<?>> resources) {
        this(resources, Map.of());
    }

    private Resources(List<Resource<?>> resources, Map<Resource<?>, ResourceGroup> groups) {
        this.resources = ImmutableList.copyOf(resources);
        this.groups = ImmutableMap.copyOf(groups);
    }

    public static Resources of(Resource<?>... resources) { // default
        return new Resources(Arrays.stream(resources).toList());
    }

    /** Resources listed in order, some of them in groups. */
    public static Builder builder() {
        return new Builder();
    }

    /** The group {@code resource} is shown under here, or {@code null} for a tab of its own. */
    @Nullable
    public ResourceGroup getGroup(Resource<?> resource) {
        return groups.get(resource);
    }

    /**
     * The order resources are added in is the order the resource view lists them; a group's tab stands where its first
     * resource is.
     */
    public static final class Builder {
        private final List<Resource<?>> resources = new ArrayList<>();
        private final Map<Resource<?>, ResourceGroup> groups = new HashMap<>();

        private Builder() {
        }

        public Builder add(Resource<?>... resources) {
            for (var resource : resources) {
                append(resource, null);
            }
            return this;
        }

        /** Adds {@code members} under {@code group}. A group can be added to again; its tab stays at its first. */
        public Builder group(ResourceGroup group, Resource<?>... members) {
            for (var resource : members) {
                append(resource, group);
            }
            return this;
        }

        public boolean contains(Resource<?> resource) {
            return resources.contains(resource);
        }

        public Resources build() {
            return new Resources(resources, groups);
        }

        private void append(Resource<?> resource, @Nullable ResourceGroup group) {
            if (resources.contains(resource)) {
                throw new IllegalArgumentException("resource " + resource.getName() + " is listed twice");
            }
            resources.add(resource);
            if (group != null) {
                groups.put(resource, group);
            }
        }
    }

}
