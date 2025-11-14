package com.lowdragmc.lowdraglib2.configurator.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ConfigSetter {
    /**
     * Specifies the configurable / persisted field name associated with this method.
     * This value is used to map the annotated method to set the value with a specific method.
     * <pre>{@code
     * @Configurable
     * int intField = 10;
     *
     * @ConfigSetter(field = "intField")
     * public void setField(int value) {
     *     this.intField = value;
     * }}</pre>
     * @return the name of the associated configuration field as a String
     */
    String field();
}
