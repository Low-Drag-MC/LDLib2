package com.lowdragmc.lowdraglib2.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author KilaBash
 * @date 2023/6/21
 * @implNote LDLibPlugin
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface LDLibPlugin {
}
