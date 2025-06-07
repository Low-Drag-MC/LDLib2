package com.lowdragmc.lowdraglib2.graphprocessor.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface CustomPortOutput {
    /**
     * The field which should be handled by a custom method
     */
    String field();
    Class type() default Object.class;
}
