package com.lowdragmc.lowdraglib2.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author KilaBash
 * @date 2023/6/21
 * @implNote LDLibPlugin
 * @port ELB_GG 
 * @date_port 2026/03/29 
 * @port_to fabric
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface LDLibPlugin {
}
