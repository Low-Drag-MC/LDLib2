package com.lowdragmc.lowdraglib.editor.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Configurable {
    String name() default "";
    boolean showName() default true;
    String[] tips() default {};
    boolean collapse() default true;
    boolean canCollapse() default true;
    boolean forceUpdate() default true;
    String key() default "";
    boolean subConfigurable() default false;
    boolean persisted() default true;
}
