package com.lowdragmc.lowdraglib2.syncdata.annotation;

import com.lowdragmc.lowdraglib2.syncdata.blockentity.IAutoSyncBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.blockentity.IAsyncAutoSyncBlockEntity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that marks a field as being managed lazily. This means that the field will only be marked as dirty manually.
 * In general, {@link IAutoSyncBlockEntity#defaultServerTick()} and {@link IAsyncAutoSyncBlockEntity#asyncTick(long)} will check field dirty automatically.
 * This annotation is useful for fields that are not updated frequently, or for fields that are updated in a batch.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface LazyManaged {
}
