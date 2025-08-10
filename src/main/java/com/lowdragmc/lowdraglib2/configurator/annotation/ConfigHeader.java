package com.lowdragmc.lowdraglib2.configurator.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface ConfigHeader {
    /**
     * Specifies the header value for the annotated configuration field.
     * This value represents additional metadata or a descriptive identifier
     * associated with the configuration field.
     *
     * @return the header value as a String
     */
    String value();

    int topMargin() default 5;
}
