package com.lowdragmc.lowdraglib2.syncdata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface SkipPersistedValue {
    /**
     * Specifies the field name associated with this method.
     * This value is used to map the annotated method to skip the persistence of the value if the method returns false.
     * <pre>{@code
     * @Configurable
     * int intField = 10;
     *
     * @SkipPersistedValue(field = "intField")
     * public boolean skipIntFieldPersisted(int value) {
     *     return value == 10;
     * }}</pre>
     * @return the name of the associated configuration field as a String
     */
    String field();
}
