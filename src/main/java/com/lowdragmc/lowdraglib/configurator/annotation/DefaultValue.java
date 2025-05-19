package com.lowdragmc.lowdraglib.configurator.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface DefaultValue {
    double[] numberValue() default {};
    String[] stringValue() default {};
    boolean[] booleanValue() default {};
}
