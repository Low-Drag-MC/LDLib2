package com.lowdragmc.lowdraglib2.gui.sync.bindings.impl;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IDataSource;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.SyncStrategy;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;
import org.apache.commons.lang3.function.Consumers;
import org.apache.commons.lang3.function.Suppliers;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Accessors(chain = true, fluent = true)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@KJSBindings
public class DataBindingBuilder<T> {
    @Getter @Setter
    private String name = "unknown";
    @Setter
    private SyncStrategy s2cStrategy = SyncStrategy.CHANGED_PERIODIC;
    @Setter
    private SyncStrategy c2sStrategy = SyncStrategy.CHANGED_PERIODIC;
    @Setter
    @Nullable
    private Type type;
    @Setter
    @Nonnull
    private Supplier<T> getter;
    @Setter
    @Nonnull
    private Consumer<T> setter;
    @Setter
    @Nullable
    private Supplier<T> remoteGetter;
    @Setter
    @Nullable
    private Consumer<T> remoteSetter;
    @Nullable
    private T initialValue;
    // sync hooks, additive. do NOT turn these into @Setter fields, they must stack.
    private final List<Consumer<T>> onSyncReceived = new ArrayList<>();
    private final List<Consumer<T>> onRemoteSyncReceived = new ArrayList<>();
    private final List<Consumer<T>> onServerSyncReceived = new ArrayList<>();
    private final List<Consumer<T>> onBeforeSync = new ArrayList<>();
    private final List<Consumer<T>> onAfterSync = new ArrayList<>();

    protected DataBindingBuilder(Supplier<T> getter, Consumer<T> setter) {
        this.getter = getter;
        this.setter = setter;
    }

    public static <T> DataBindingBuilder<T> create(Supplier<T> getter, Consumer<T> setter) {
        return new DataBindingBuilder<>(getter, setter);
    }

    public DataBindingBuilder<T> syncType(Type type) {
        this.type = type;
        return this;
    }

    public DataBindingBuilder<T> syncType(Class<?> clazz) {
        return syncType((Type) clazz);
    }

    public DataBindingBuilder<T> data(Supplier<T> getter, Consumer<T> setter) {
        this.getter = getter;
        this.setter = setter;
        return this;
    }

    public DataBindingBuilder<T> initialValue(@Nullable T initialValue) {
        this.initialValue = initialValue;
        if (initialValue == null) return this;
        if (type == null) {
            type = initialValue.getClass();
        }
        return this;
    }

    ///  Sync Hooks

    /**
     * Adds a hook invoked on the <b>receiving</b> side (whichever side that is), after the incoming value has
     * been pushed into the local data source. Hooks stack, calling this twice registers two hooks.
     * <p>
     * Note: for collection / map values the accessor mutates the client's existing instance in place, so the hook
     * receives the same instance every time. Do not diff it against a cached reference to detect changes.
     */
    public DataBindingBuilder<T> onSyncReceived(Consumer<T> hook) {
        this.onSyncReceived.add(hook);
        return this;
    }

    /**
     * Same as {@link #onSyncReceived(Consumer)}, but only installed on the client side,
     * i.e. it fires when S2C data arrives.
     */
    public DataBindingBuilder<T> onRemoteSyncReceived(Consumer<T> hook) {
        this.onRemoteSyncReceived.add(hook);
        return this;
    }

    /**
     * Same as {@link #onSyncReceived(Consumer)}, but only installed on the server side,
     * i.e. it fires when C2S data arrives.
     */
    public DataBindingBuilder<T> onServerSyncReceived(Consumer<T> hook) {
        this.onServerSyncReceived.add(hook);
        return this;
    }

    /**
     * Adds a hook invoked on the <b>sending</b> side, right before the value is written to the sync buffer.
     * <p>
     * Note: this runs while {@link com.lowdragmc.lowdraglib2.gui.sync.UISyncManager#tick()} is iterating the
     * registered sync values. Do not add / remove sync values (i.e. do not mutate the UI structure) from here;
     * mark a flag and do it on the next tick instead.
     */
    public DataBindingBuilder<T> onBeforeSync(Consumer<T> hook) {
        this.onBeforeSync.add(hook);
        return this;
    }

    /**
     * Adds a hook invoked on the <b>sending</b> side, right after the value has been written to the sync buffer.
     * Same iteration caveat as {@link #onBeforeSync(Consumer)} applies.
     */
    public DataBindingBuilder<T> onAfterSync(Consumer<T> hook) {
        this.onAfterSync.add(hook);
        return this;
    }

    public SimpleBinding<T> build() {
        if (LDLib2.isRemote()) return build(true);
        if (LDLib2.isServer()) return build(false);
        throw new IllegalStateException("Cannot build the binding '%s': the current thread is neither a client nor a server thread. Use build(boolean isRemote) to pick a side explicitly.".formatted(name));
    }

    public SimpleBinding<T> build(boolean isRemote) {
        Objects.requireNonNull(getter);

        if (type == null) {
            type = resolveType();
        }

        var binding = new SimpleBinding<>(isRemote, name, type, initialValue, c2sStrategy, s2cStrategy);

        if (isRemote) {
            if (remoteSetter != null || remoteGetter != null) {
                binding.setRemoteDataSource(IDataSource.of(
                        remoteSetter == null ? Consumers.nop() : remoteSetter,
                        remoteGetter == null ? Suppliers.nul() : remoteGetter
                        )
                );
            }
            onRemoteSyncReceived.forEach(binding::registerListener);
        } else {
            binding.setServerDataSource(IDataSource.of(setter, getter));
            onServerSyncReceived.forEach(binding::registerListener);
        }
        onSyncReceived.forEach(binding::registerListener);
        onBeforeSync.forEach(binding::registerPreSyncListener);
        onAfterSync.forEach(binding::registerPostSyncListener);

        return binding;
    }

    /**
     * Best effort inference of the synced type when {@link #syncType} was not called. Note that this can never
     * recover generic information, so generic types such as {@code List<String>} always require an explicit
     * {@link #syncType(Type)} with a type token.
     */
    private Type resolveType() {
        var value = getter.get();
        if (value != null) return value.getClass();
        if (initialValue != null) return initialValue.getClass();
        throw new IllegalStateException("Cannot infer the sync type of the binding '%s': the getter returned null and no initial value was given. Call syncType(...) explicitly.".formatted(name));
    }

    ///  Built-in
    /**
     * Creates and returns a data binding holder for {@link ItemStack} that supports synchronization
     * and data manipulation using the provided getter and setter functions.
     */
    public static DataBindingBuilder<ItemStack> itemStack(Supplier<ItemStack> getter, Consumer<ItemStack> setter) {
        return create(getter, setter).syncType(ItemStack.class);
    }

    public static DataBindingBuilder<ItemStack> itemStackS2C(Supplier<ItemStack> getter) {
        return itemStack(getter, Consumers.nop()).c2sStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<ItemStack> itemStackC2S(Consumer<ItemStack> setter) {
        return itemStack(Suppliers.nul(), setter).s2cStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<FluidStack> fluidStack(Supplier<FluidStack> getter, Consumer<FluidStack> setter) {
        return create(getter, setter).syncType(FluidStack.class);
    }

    public static DataBindingBuilder<FluidStack> fluidStackS2C(Supplier<FluidStack> getter) {
        return fluidStack(getter, Consumers.nop()).c2sStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<FluidStack> fluidStackC2S(Consumer<FluidStack> setter) {
        return fluidStack(Suppliers.nul(), setter).s2cStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<Block> block(Supplier<Block> getter, Consumer<Block> setter) {
        return create(getter, setter).syncType(Block.class);
    }

    public static DataBindingBuilder<Block> blockS2C(Supplier<Block> getter) {
        return block(getter, Consumers.nop()).c2sStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<Block> blockC2S(Consumer<Block> setter) {
        return block(Suppliers.nul(), setter).s2cStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<Tag> tag(Supplier<Tag> getter, Consumer<Tag> setter) {
        return create(getter, setter).syncType(Tag.class);
    }

    public static DataBindingBuilder<Tag> tagS2C(Supplier<Tag> getter) {
        return tag(getter, Consumers.nop()).c2sStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<Tag> tagC2S(Consumer<Tag> setter) {
        return tag(Suppliers.nul(), setter).s2cStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<Component> component(Supplier<Component> getter, Consumer<Component> setter) {
        return create(getter, setter).syncType(Component.class);
    }

    public static DataBindingBuilder<Component> componentS2C(Supplier<Component> getter) {
        return component(getter, Consumers.nop()).c2sStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<Component> componentC2S(Consumer<Component> setter) {
        return component(Suppliers.nul(), setter).s2cStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<Integer> intVal(Supplier<Integer> getter, Consumer<Integer> setter) {
        return create(getter, setter).syncType(Integer.class);
    }

    public static DataBindingBuilder<Integer> intValS2C(Supplier<Integer> getter) {
        return intVal(getter, Consumers.nop()).c2sStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<Integer> intValC2S(Consumer<Integer> setter) {
        return intVal(Suppliers.nul(), setter).s2cStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<Boolean> bool(Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return create(getter, setter).syncType(Boolean.class);
    }

    public static DataBindingBuilder<Boolean> boolS2C(Supplier<Boolean> getter) {
        return bool(getter, Consumers.nop()).c2sStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<Boolean> boolC2S(Consumer<Boolean> setter) {
        return bool(Suppliers.nul(), setter).s2cStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<Float> floatVal(Supplier<Float> getter, Consumer<Float> setter) {
        return create(getter, setter).syncType(Float.class);
    }

    public static DataBindingBuilder<Float> floatValS2C(Supplier<Float> getter) {
        return floatVal(getter, Consumers.nop()).c2sStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<Float> floatValC2S(Consumer<Float> setter) {
        return floatVal(Suppliers.nul(), setter).s2cStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<Double> doubleVal(Supplier<Double> getter, Consumer<Double> setter) {
        return create(getter, setter).syncType(Double.class);
    }

    public static DataBindingBuilder<Double> doubleValS2C(Supplier<Double> getter) {
        return doubleVal(getter, Consumers.nop()).c2sStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<Double> doubleValC2S(Consumer<Double> setter) {
        return doubleVal(Suppliers.nul(), setter).s2cStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<Long> longVal(Supplier<Long> getter, Consumer<Long> setter) {
        return create(getter, setter).syncType(Long.class);
    }

    public static DataBindingBuilder<Long> longValS2C(Supplier<Long> getter) {
        return longVal(getter, Consumers.nop()).c2sStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<Long> longValC2S(Consumer<Long> setter) {
        return longVal(Suppliers.nul(), setter).s2cStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<Byte> byteVal(Supplier<Byte> getter, Consumer<Byte> setter) {
        return create(getter, setter).syncType(Byte.class);
    }

    public static DataBindingBuilder<Byte> byteValS2C(Supplier<Byte> getter) {
        return byteVal(getter, Consumers.nop()).c2sStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<Byte> byteValC2S(Consumer<Byte> setter) {
        return byteVal(Suppliers.nul(), setter).s2cStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<Short> shortVal(Supplier<Short> getter, Consumer<Short> setter) {
        return create(getter, setter).syncType(Short.class);
    }

    public static DataBindingBuilder<Short> shortValS2C(Supplier<Short> getter) {
        return shortVal(getter, Consumers.nop()).c2sStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<Short> shortValC2S(Consumer<Short> setter) {
        return shortVal(Suppliers.nul(), setter).s2cStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<Character> charVal(Supplier<Character> getter, Consumer<Character> setter) {
        return create(getter, setter).syncType(Character.class);
    }

    public static DataBindingBuilder<Character> charValS2C(Supplier<Character> getter) {
        return charVal(getter, Consumers.nop()).c2sStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<Character> charValC2S(Consumer<Character> setter) {
        return charVal(Suppliers.nul(), setter).s2cStrategy(SyncStrategy.NONE);
    }

    public static <T extends Enum<?>> DataBindingBuilder<T> enumVal(Class<T> clazz, Supplier<T> getter, Consumer<T> setter) {
        return create(getter, setter).syncType(clazz);
    }

    public static <T extends Enum<?>> DataBindingBuilder<T> enumValS2C(Class<T> clazz, Supplier<T> getter) {
        return enumVal(clazz, getter, Consumers.nop()).c2sStrategy(SyncStrategy.NONE);
    }

    public static <T extends Enum<?>> DataBindingBuilder<T> enumValC2S(Class<T> clazz, Consumer<T> setter) {
        return enumVal(clazz, Suppliers.nul(), setter).s2cStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<String> string(Supplier<String> getter, Consumer<String> setter) {
        return create(getter, setter).syncType(String.class);
    }

    public static DataBindingBuilder<String> stringS2C(Supplier<String> getter) {
        return string(getter, Consumers.nop()).c2sStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<String> stringC2S(Consumer<String> setter) {
        return string(Suppliers.nul(), setter).s2cStrategy(SyncStrategy.NONE);
    }

}
