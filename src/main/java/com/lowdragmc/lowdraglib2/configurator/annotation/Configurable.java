package com.lowdragmc.lowdraglib2.configurator.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The Configurable annotation is used to mark fields or types that can be
 * configured through a configuration system. It provides several properties
 * to customize the display, behavior, and metadata of the annotated fields
 * or types.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Configurable {
    /**
     * Specifies a custom name for the configurable field or type.
     * If no name is provided, the default name derived from the field or type will be used.
     *
     * @return the custom name for the configurable field or type, or an empty string for default behavior
     */
    String name() default "";

    /**
     * Determines whether the name of the configurable field or type should be displayed.
     *
     * @return true if the name should be displayed, false otherwise
     */
    boolean showName() default true;

    /**
     * Provides customizable tips or hints associated with a configurable field or type.
     * These tips can be displayed in user interfaces or documentation to provide
     * additional information or context for a field or type.
     *
     * @return an array of tips or hints related to the configurable field or type, or an empty array if no tips are specified
     */
    String[] tips() default {};

    /**
     * Indicates whether the configuration should be collapsible in a user interface or other
     * contexts where it is applicable. When set to true, the associated group or section
     * may be collapsible; when set to false, it remains expanded or non-collapsible.
     *
     * @return true if the configuration is collapsible, false otherwise
     */
    boolean collapse() default true;

    /**
     * Indicates whether the configuration can be interactively collapsed in user interfaces or similar contexts.
     * This property allows enhanced control over the behavior of collapsible sections in configuration settings.
     *
     * @return true if the configuration can be collapsed, false otherwise
     */
    boolean canCollapse() default true;

    /**
     * Indicates whether the field or type annotated with {@link Configurable} should force an update
     * when its value is changed. This can be used to ensure that any modifications are immediately
     * applied or reflected in the relevant systems or configurations.
     *
     * @return true if updates should be forced when the annotated field or type's value changes, false otherwise
     */
    boolean forceUpdate() default true;

    /**
     * Specifies a custom key for the configurable or persisted field.
     * If no key is provided, the default field name will be used as the key.
     *
     * @return the custom key for the configurable or persisted field, or an empty string
     *         to use the default field name
     */
    String key() default "";

    /**
     * Indicates whether the annotated field or type represents a sub-configurable entity.
     * When set to true, the field's value is treated as a nested group of configurations,
     * whose configurators will be created under a new group.
     *
     * @return true if the annotated field or type is a sub-configurable entity, false otherwise
     */
    boolean subConfigurable() default false;

    /**
     * Indicates whether the annotated field or type should be persisted during {@link com.lowdragmc.lowdraglib2.utils.PersistedParser}
     *
     * @return true if the field or type should be persisted, false otherwise
     */
    boolean persisted() default true;
}
