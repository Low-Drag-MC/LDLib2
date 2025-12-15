package com.lowdragmc.lowdraglib2.configurator.annotation;

import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface ConfigSelector {
    /**
     * Specifies a list of candidate values that can be selected for a field.
     * When applied to an enumerated field, the specified candidates
     * are used to filter the valid options from the enum constants.
     *
     * @return an array of strings representing the names of the candidate values,
     *         or an empty array if no specific candidates are defined
     */
    String[] candidate() default {};

    /**
     * Specifies the maximum value or limitation associated with a configurable field or annotation.
     *
     * @return the maximum value as an integer, with a default value of 5
     */
    int max() default 5;

    /**
     * Specifies the name of the builder method responsible for creating
     * or managing sub-configurations related to the annotated field.
     * This method is expected to define or initialize sub-configurations
     * as required by the application logic.
     * <pre>{@code
     * @ConfigSelector(subConfiguratorBuilder = "subConfiguratorBuilder")
     * private Direction direction = Direction.NORTH;
     *
     * private void subConfiguratorBuilder(Direction direction, ConfiguratorGroup group) {
     *     switch (direction) {
     *         case NORTH -> group.addConfigurator(new Configurator("NORTH"));
     *         case WEST -> {}
     *         case EAST -> group.addConfigurator(new Configurator("EAST"));
     *         default -> group.addConfigurator(new Configurator("DEFAULT"));
     *     }
     * }
     * }</pre>
     *
     * @return the name of the sub-configurator builder method as a String;
     * returns an empty string if no builder method is specified
     */
    String subConfiguratorBuilder() default "";
}
