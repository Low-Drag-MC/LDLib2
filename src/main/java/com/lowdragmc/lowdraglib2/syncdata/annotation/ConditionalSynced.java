package com.lowdragmc.lowdraglib2.syncdata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConditionalSynced {
    /**
     * Specifies the method name that will be used to determine the conditional sync behavior
     * for the annotated field.
     * This value is used to map the annotated field to control whether it syncs if changes.
     *
     * <pre>{@code
     * @Configurable
     * @ConditionalSynced(methodName = "shouldSync")
     * int intField = 10;
     *
     * public boolean shouldSync(int value) {
     *     return value > 0;
     * }}</pre>
     *
     * @return the name of the method used to evaluate the conditional sync operation.
     */
    String methodName();
}
