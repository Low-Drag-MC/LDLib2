package com.lowdragmc.lowdraglib2.syncdata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Saves and syncs a managed field with a codec of your own instead of the accessor registered for its type.
 * <br>
 * It applies wherever the field is saved or synced: {@link Persisted} and {@link DescSynced} fields of a managed
 * holder (such as a block entity), and {@link Persisted} or persisted {@code @Configurable} fields serialized by
 * {@link com.lowdragmc.lowdraglib2.utils.PersistedParser PersistedParser}.
 * <pre>{@code
 * public class MachineBlockEntity extends BlockEntity implements ISyncPersistRPCBlockEntity {
 *     @Persisted
 *     @DescSynced
 *     @ManagedCodec(value = "targetCodec", streamCodec = "targetStreamCodec")
 *     private BlockPos target = BlockPos.ZERO;
 *
 *     protected Codec<BlockPos> targetCodec() {
 *         return Codec.LONG.xmap(BlockPos::of, BlockPos::asLong);
 *     }
 *
 *     protected StreamCodec<ByteBuf, BlockPos> targetStreamCodec() {
 *         return ByteBufCodecs.VAR_LONG.map(BlockPos::of, BlockPos::asLong);
 *     }
 * }
 *
 * public class AdvancedMachineBlockEntity extends MachineBlockEntity {
 *     @Override
 *     protected Codec<BlockPos> targetCodec() {
 *         return BlockPos.CODEC;
 *     }
 * }
 * }</pre>
 *
 * <h2>Naming the methods</h2>
 * {@link #value()} and {@link #streamCodec()} name methods that take no arguments. They are looked up on the
 * holder's own class first and then on its superclasses, whatever their visibility, and last among its public
 * methods, which takes in interface default methods. An instance method is called on the holder, so a subclass
 * may override it to save or sync the field differently; a static method works as well. With {@link #owner()}
 * set, the methods are looked up on that class instead and must be static.
 *
 * <h2>When they are called</h2>
 * The methods are called when a ref is first created for the field on an object of a given class: when a managed
 * holder's storage initializes, or the first time {@code PersistedParser} saves or loads such an object. What they
 * return is kept for the field and that class and shared by all its instances, so:
 * <ul>
 *     <li>it may differ between a class and its subclasses, but must not depend on the state of the instance the
 *     method happens to be called on;</li>
 *     <li>the server and the client must arrive at the same codecs, as each side builds its own;</li>
 *     <li>the methods may run more than once when two threads get there first together, so they must have no side
 *     effects.</li>
 * </ul>
 *
 * <h2>How the value travels</h2>
 * <ul>
 *     <li>Saves and the initial sync, the one a client gets with the block entity's update tag when it loads the
 *     chunk, use the codec, with registry-aware ops, so codecs such as {@code ItemStack.CODEC} work.</li>
 *     <li>Anything that writes the field to a byte buffer uses the stream codec, or the codec's value as NBT when
 *     there is none: a managed holder's sync packets after the initial sync, and {@code PersistedParser} writing an
 *     object to a buffer, as {@link com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable#writeToBuff
 *     IPersistedSerializable#writeToBuff} and {@code PersistedParser#createStreamCodec} do, which reaches
 *     {@link Persisted} fields too.</li>
 *     <li>A {@code null} field is saved and synced as null without calling either.</li>
 *     <li>Reading a value back assigns a new instance to the field rather than filling in the one it holds.</li>
 *     <li>Changes are detected by encoding the value on every update and comparing it with the last encoding,
 *     which also catches changes made in place, such as adding to a list. That costs one encode per update; to
 *     check more cheaply, return an {@link com.lowdragmc.lowdraglib2.syncdata.accessor.IAccessor IAccessor}
 *     instead, built for example with
 *     {@link com.lowdragmc.lowdraglib2.syncdata.accessor.direct.CustomDirectAccessor#builder(Class) CustomDirectAccessor.builder}
 *     and its {@code copyMark} or {@code customMark}.</li>
 * </ul>
 *
 * <h2>Mistakes</h2>
 * A method that cannot be found or called, that throws or returns {@code null}, or that returns the wrong kind of
 * value fails with an {@link IllegalArgumentException} naming the field, at the moment described above, rather
 * than falling back to the registered accessor and silently changing what gets saved. Nothing checks the codec's
 * type against the field's: a mismatch shows as a {@link ClassCastException} the first time a value is saved or
 * synced. Use the boxed type for a primitive field, {@code Codec<Integer>} for an {@code int}.
 * <br>
 * {@code PersistedParser} does not use it on a field marked {@code subPersisted} or {@code subConfigurable}, which
 * saves the value's own fields instead of the value.
 *
 * @see Persisted
 * @see DescSynced
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ManagedCodec {
    /**
     * Name of a method returning either
     * <ul>
     *     <li>a {@code Codec<T>} for the field's type {@code T}, used for saves and the initial sync, and for byte
     *     buffers unless {@link #streamCodec()} names a stream codec of its own; or</li>
     *     <li>an {@link com.lowdragmc.lowdraglib2.syncdata.accessor.IAccessor IAccessor&lt;T&gt;} that takes over
     *     saving, syncing and change detection entirely, in which case {@link #streamCodec()} must be left empty.</li>
     * </ul>
     */
    String value();

    /**
     * Name of a method returning a {@code StreamCodec<? super RegistryFriendlyByteBuf, T>}, used wherever the field
     * is written to a byte buffer instead of being saved: a managed holder's sync packets after the initial sync,
     * and {@code PersistedParser} writing an object to a buffer. It is never used for saves.
     * <br>
     * Saves and the initial sync still use the codec, so the two must agree on every value: a client that loads
     * the chunk later gets what the codec decodes, while one that was watching got what this decodes.
     * <br>
     * When empty, the codec's value is written to the buffer as NBT, which works the same and is only larger and
     * slower, so a stream codec is an optimization.
     */
    String streamCodec() default "";

    /**
     * A class to look the methods up on instead of the holder's, for codecs kept in one place and shared between
     * holders. The methods must then be static, as there is no instance of it to call them on.
     */
    Class<?> owner() default void.class;
}
