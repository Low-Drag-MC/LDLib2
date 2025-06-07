package com.lowdragmc.lowdraglib2.syncdata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field should be persisted for persistence.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Persisted {
    /**
     * The key of the field in the persistence.
     * @return The key of the field in the persistence.
     */
    String key() default "";

    /**
     * If true, it will wrap the field as a map, and serialize the field's internal values into the map
     */
    boolean subPersisted() default false;
}
