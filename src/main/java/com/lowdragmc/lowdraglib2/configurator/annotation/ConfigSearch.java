package com.lowdragmc.lowdraglib2.configurator.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface ConfigSearch {
    /**
     * Retrieves the name of the method which returns {@link com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator.ISearchConfigurator} used for searching or configuring
     * related configurations within the annotated field.
     *
     * <pre>{@code
     * @ConfigSearch(searchConfiguratorMethod = "createSearchConfigurator")
     * Block block = Blocks.STONE;
     *
     * public ISearchConfigurator createSearchConfigurator() {
     *     return ...;
     * }
     * }</pre>
     *
     * @return the name of the search configurator method as a String
     */
    String searchConfiguratorMethod();
}
