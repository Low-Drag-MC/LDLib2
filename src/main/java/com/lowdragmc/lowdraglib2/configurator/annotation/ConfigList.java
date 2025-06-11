package com.lowdragmc.lowdraglib2.configurator.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface ConfigList {
    /**
     * Specifies the name of a method used to configure elements of the annotated field.
     * This method is expected to handle the customization or initialization of the
     * configuration for elements in the list.
     * <br> e.g. {@code Configurator methodName(Supplier<T> getter, Consumer<T> setter)}
     *
     * @return the name of the configurator method as a String; returns an empty string
     *         if no method is specified by default
     */
    String configuratorMethod() default "";

    /**
     * Specifies the name of a method used to add default configurations for the
     * annotated field. This method is expected to handle the creation of default
     * values or elements to the configuration list, if applicable.
     *
     * <br> e.g. {@code T methodName()}
     *
     * @return the name of the method responsible for adding default configurations
     *         as a String; returns an empty string if no method is specified by default
     */
    String addDefaultMethod() default "";

    /**
     * Determines whether new elements can be added to the configuration list
     * associated with the annotated field.
     *
     * @return true if elements can be added; false otherwise
     */
    boolean canAdd() default true;

    /**
     * Determines whether elements can be removed from the configuration list
     * associated with the annotated field.
     *
     * @return true if elements can be removed; false otherwise
     */
    boolean canRemove() default true;

    /**
     * Determines whether the elements in the configuration list associated with the
     * annotated field can be reordered. This setting is used to control whether the
     * ordering of elements within the list can be customized by the user or through
     * programmatic mechanisms.
     *
     * @return true if elements can be reordered; false otherwise
     */
    boolean canReorder() default true;
}
