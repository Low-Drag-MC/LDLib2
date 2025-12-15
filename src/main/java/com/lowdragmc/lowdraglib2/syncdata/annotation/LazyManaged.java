package com.lowdragmc.lowdraglib2.syncdata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that marks a field as being managed lazily. This means that the field will only be marked as dirty manually.
 * This annotation is useful for fields that are not updated frequently, or for fields that are updated in a batch.
 * <pre>{@code
 * @DescSynced
 * @Persisted
 * int a;
 *
 * @DescSynced
 * @Persisted
 * @LayzManaged
 * int b;
 *
 * public void setA(int value) {
 *     this.a = value;  // will be sync/persist automatically, in general
 * }
 *
 * public void setB(int value) {
 *     this.b = value;
 *     markDirty("b"); // mannually notify chagned
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface LazyManaged {
}
