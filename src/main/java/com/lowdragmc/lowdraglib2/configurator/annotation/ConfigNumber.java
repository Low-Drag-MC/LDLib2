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

    /**
     * Defines the range of valid numeric values.
     *
     * @return an array of two double values, where the first value specifies the lower bound
     *         and the second value specifies the upper bound of the range
     */
    double[] range();

    /**
     * Defines the default wheel value associated with the annotated field or resource texture operation.
     */
    double wheel() default 0;

    /**
     * Specifies the numeric type for the annotated field.
     */
    Type type() default Type.AUTO;
}
