package com.lowdragmc.lowdraglib2.configurator.annotation;

import org.jetbrains.annotations.Nullable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface ConfigNumber {
    enum Type {
        AUTO(null, null, null),
        INTEGER(Integer.MIN_VALUE, Integer.MAX_VALUE, 1),
        FLOAT(-Float.MAX_VALUE, Float.MAX_VALUE, 0.1f),
        DOUBLE(-Double.MAX_VALUE, Double.MAX_VALUE, 0.1),
        LONG(Long.MIN_VALUE, Long.MAX_VALUE, 1L),
        SHORT(Short.MIN_VALUE, Short.MAX_VALUE, (short) 1),
        BYTE(Byte.MIN_VALUE, Byte.MAX_VALUE, (byte) 1)
        ;

        @Nullable
        public final Number min;
        @Nullable
        public final Number max;
        @Nullable
        public final Number wheel;

        Type(@Nullable Number min, @Nullable Number max, @Nullable Number wheel) {
            this.min = min;
            this.max = max;
            this.wheel = wheel;
        }
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
