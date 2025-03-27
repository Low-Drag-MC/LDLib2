package com.lowdragmc.lowdraglib.graphprocessor.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface InputPort {
    /**
     * Port display name in the editor
     */
    String name() default "";
    /**
     * Whether the port can be connected by multiple edges
     */
    boolean allowMultiple() default false;
    /**
     * Tooltips about this port.
     */
    String[] tips() default {};
    /**
     * Port color shown in the editor.
     * @return 0 - auto color
     */
    int color() default 0;
    /**
     * Port priority of display order, the smallest one will be displayed on the top of the list.
     */
    int priority() default 0;
}
