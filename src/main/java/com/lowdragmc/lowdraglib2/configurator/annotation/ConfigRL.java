package com.lowdragmc.lowdraglib2.configurator.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for specifying that a {@link net.minecraft.resources.ResourceLocation} field refers to a specific registry of objects,
 * and have specific component for it.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface ConfigRL {
    enum Type {
        FONT,
        ITEM_TAG_KEY,
        BLOCK_TAG_KEY,
        ENTITY_TYPE_TAG_KEY,
        FLUID_TAG_KEY,
        ;
    }

    Type value();
}
