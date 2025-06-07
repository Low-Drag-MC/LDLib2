package com.lowdragmc.lowdraglib2.configurator.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface ConfigNumber {
    enum Type {
        AUTO,
        INTEGER,
        FLOAT,
        DOUBLE,
        LONG,
        CHAR,
        SHORT,
        BYTE,
    }
    double[] range();
    double wheel() default 0;
    Type type() default Type.AUTO;
}
