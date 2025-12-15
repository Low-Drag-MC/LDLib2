package com.lowdragmc.lowdraglib2.syncdata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When the annotated fields updated (synced from server) will call the listener method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface UpdateListener {
    /**
     * Specifies the name of the method to be called (remote side) when the annotated field is updated from the server.
     * The first parameter is the old value, and the second parameter is the new value.
     *
     * <pre>{@code
     * @DescSynced
     * @UpdateListener(methodName = "onIntValueChanged")
     * private int intValue = 10;
     *
     * private void onIntValueChanged(int oldValue, int newValue) {
     *
     * }
     * }</pre>
     *
     * @return the method name to be invoked as a {@code String}.
     */

    String methodName();
}
