package com.lowdragmc.lowdraglib2.registry.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation for registering elements in the auto registry. see{@link com.lowdragmc.lowdraglib2.registry.AutoRegistry.LDLibRegister}
 * <br>
 * make sure the class with this annotation has implemented the interface {@link com.lowdragmc.lowdraglib2.registry.ILDLRegister}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface LDLRegister {

    /**
     * The registry name of the element. it must be {@link net.minecraft.resources.ResourceLocation} format.
     */
    String registry();

    /**
     * Should be unique in the same registry. It will be registered as the key in the registry.
     */
    String name();

    /**
     * In general, it used to group the elements in the same category.
     */
    String group() default "";

    /**
     * Register it while such mod is installed.
     */
    String modID() default "";

    /**
     * The priority of the element during iteration of the registry, the higher the value, the higher the priority.
     */
    int priority() default 0;

    /**
     * Whether the element should be registered manually. If true, the element will not be registered automatically.
     * If false you HAVE TO register it manually in the {@link com.lowdragmc.lowdraglib2.registry.AutoRegistry.LDLibRegister}
     */
    boolean manual() default false;
}
