package com.lowdragmc.lowdraglib2.nodegraphtookit.api.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Registers a node type and binds it to one or more graph types.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NodeAttribute {
    String name();

    String group() default "";

    String modID() default "";

    int priority() default 0;

    /**
     * The environment in which this node should be registered.
     */
    RegistrationEnvironment environment() default RegistrationEnvironment.ALWAYS;

    Class<? extends Graph>[] graphTypes();
}
