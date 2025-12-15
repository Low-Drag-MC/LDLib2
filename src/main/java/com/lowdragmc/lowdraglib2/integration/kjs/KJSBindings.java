package com.lowdragmc.lowdraglib2.integration.kjs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface KJSBindings {
    String value() default "";
    String modId() default "";
    boolean clientOnly() default false;
}
