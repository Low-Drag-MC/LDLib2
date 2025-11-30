package com.lowdragmc.lowdraglib2.syncdata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that marks a field to be synchronized and sent for descriptive purposes.
 * This annotation is typically used to indicate that the value of the field should be
 * synchronized between server and client for presentation or informational purposes.
 * <pre>{@code
 * @DescSynced
 * int a;
 *
 * @DescSynced
 * private ItemStack b = ItemStack.EMPTY;
 *
 * @DescSynced
 * private List<ResourceLocation> c = new ArrayList<>();
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DescSynced {
}
