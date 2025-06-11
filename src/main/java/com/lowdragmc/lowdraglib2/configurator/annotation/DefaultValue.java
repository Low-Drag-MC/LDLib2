package com.lowdragmc.lowdraglib2.configurator.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface DefaultValue {
    /**
     * Specifies the default numeric values to be used.
     *
     * @return an array of double values representing the default numeric values;
     *         returns an empty array if no default values are specified
     */
    double[] numberValue() default {};

    /**
     * Specifies the default string values to be used.
     *
     * @return an array of strings representing the default values;
     *         returns an empty array if no default values are specified
     */
    String[] stringValue() default {};

    /**
     * Specifies the default boolean values to be used.
     *
     * @return an array of boolean values representing the default values;
     *         returns an empty array if no default values are specified
     */
    boolean[] booleanValue() default {};
}
