package com.lowdragmc.lowdraglib2.configurator.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ConfigSetter {
    /**
     * Specifies the configuration field name associated with this method.
     * This value is used to map the annotated method to a specific field
     * within the configuration system.
     *
     * @return the name of the associated configuration field as a String
     */
    String field();
}
