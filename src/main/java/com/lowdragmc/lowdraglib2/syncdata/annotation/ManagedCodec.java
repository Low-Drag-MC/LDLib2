package com.lowdragmc.lowdraglib2.syncdata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Saves and syncs a managed field with a codec of your own instead of the accessor registered for its type.
 * <pre>{@code
 * @Persisted
 * @DescSynced
 * @ManagedCodec("posCodec")
 * private BlockPos pos = BlockPos.ZERO;
 *
 * protected Codec<BlockPos> posCodec() {
 *     return Codec.LONG.xmap(BlockPos::of, BlockPos::asLong);
 * }
 * }</pre>
 * The named methods take no arguments and are called once per holder class, so a subclass may override them,
 * but what they return must not depend on the instance.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ManagedCodec {
    /**
     * A method returning a {@code Codec<T>}, which detects changes by encoding the value on every update,
     * or an {@link com.lowdragmc.lowdraglib2.syncdata.accessor.IAccessor IAccessor&lt;T&gt;} for full control.
     */
    String value();

    /**
     * A method returning a {@code StreamCodec<? super RegistryFriendlyByteBuf, T>} for sync packets, which must
     * agree with the codec: saves and the initial sync still use the codec. When empty, the codec is sent as NBT.
     */
    String streamCodec() default "";

    /**
     * A class whose static methods to call instead of the holder's own.
     */
    Class<?> owner() default void.class;
}
