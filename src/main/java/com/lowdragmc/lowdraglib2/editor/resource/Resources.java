package com.lowdragmc.lowdraglib2.editor.resource;

import com.google.common.collect.ImmutableList;
import net.minecraft.MethodsReturnNonnullByDefault;

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

    public Resources(List<Resource<?>> resources) {
        this.resources = ImmutableList.copyOf(resources);
    }

    public static Resources of(Resource<?>... resources) { // default
        return new Resources(Arrays.stream(resources).toList());
    }

}
