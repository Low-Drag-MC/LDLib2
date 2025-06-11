package com.lowdragmc.lowdraglib2.syncdata.annotation;

import com.lowdragmc.lowdraglib2.syncdata.ref.ReadOnlyRef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark a read-only field that is managed by the user.
 * <br>
 * Read-only types (such as {@link com.lowdragmc.lowdraglib2.syncdata.IManaged} and {@link net.neoforged.neoforge.common.util.INBTSerializable})
 * requires the field to be non-null and the field instance won't be changed (a final field). Because we don't know how to create a new instance for these types.
 * In this case, you can use this annotation and provide methods to
 * store a unique id from server with {@link #serializeMethod()} and create a new instance at the client with {@link #deserializeMethod()}.
 * <br>
 * Furthermore, you can provide a method to self-control whether the field has changed with {@link #onDirtyMethod()}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ReadOnlyManaged {

    /**
     * specify a method e.g. {@code boolean methodName()}
     * return whether it has changed.
     * if this method is not defined, it will check dirty using default method {@link ReadOnlyRef#readOnlyUpdate()}.
     */
    String onDirtyMethod() default "";

    /**
     * return a unique id (Tag) of given instance.
     * e.g. {@code Tag methodName(@Nonnull T obj)}
     * T - field type
     */
    String serializeMethod();

    /**
     * create an instance via given uid from server.
     * e.g. {@code T methodName(@Nonnull Tag tag)}
     * T - field type
     */
    String deserializeMethod();
}
