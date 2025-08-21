package com.lowdragmc.lowdraglib2.gui.sync.bindings.impl;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.IDataSource;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.SyncStrategy;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.apache.commons.lang3.function.Consumers;
import org.apache.commons.lang3.function.Suppliers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Accessors(chain = true, fluent = true)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DataBindingBuilder<T> {
    @Getter @Setter
    private static boolean isRemote;
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
    @Nonnull
    private Supplier<T> remoteGetter = Suppliers.nul();
    @Setter
    @Nonnull
    private Consumer<T> remoteSetter = Consumers.nop();
    @Nullable
    private T initialValue;

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

    public DataBindingBuilder<T> initialValue(T initialValue) {
        this.initialValue = initialValue;
        if (type == null) {
            type = initialValue.getClass();
        }
        return this;
    }

    public SimpleBinding<T> build() {
        Objects.requireNonNull(getter);

        if (type == null) {
            type = getter.get().getClass();
        }

        var binding = new SimpleBinding<>(isRemote, name, type, initialValue);
        binding.s2cStrategy(s2cStrategy);
        binding.c2sStrategy(c2sStrategy);

        if (isRemote) {
            binding.setRemoteDataSource(IDataSource.of(remoteSetter, remoteGetter));
        } else {
            binding.setServerDataSource(IDataSource.of(setter, getter));
        }

        return binding;
    }

    ///  Built-in
    /**
     * Creates and returns a data binding holder for {@link ItemStack} that supports synchronization
     * and data manipulation using the provided getter and setter functions.
     */
    public static DataBindingBuilder<ItemStack> itemStack(Supplier<ItemStack> getter, Consumer<ItemStack> setter) {
        return create(getter, setter).syncType(ItemStack.class);
    }

    /**
     * Creates a data binding holder for {@link ItemStack} that supports synchronization from
     * server to client (S2C). This binding is configured with a {@code SyncStrategy.NONE},
     * meaning no synchronization will occur for the client to server (C2S) to avoid attacks.
     */
    public static DataBindingBuilder<ItemStack> itemStackS2C(Supplier<ItemStack> getter) {
        return itemStack(getter, Consumers.nop()).c2sStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<FluidStack> fluidStack(Supplier<FluidStack> getter, Consumer<FluidStack> setter) {
        return create(getter, setter).syncType(FluidStack.class);
    }

    public static DataBindingBuilder<FluidStack> fluidStackS2C(Supplier<FluidStack> getter) {
        return fluidStack(getter, Consumers.nop()).c2sStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<Integer> intVal(Supplier<Integer> getter, Consumer<Integer> setter) {
        return create(getter, setter).syncType(Integer.class);
    }

    public static DataBindingBuilder<Integer> intValS2C(Supplier<Integer> getter) {
        return intVal(getter, Consumers.nop()).c2sStrategy(SyncStrategy.NONE);
    }

    public static DataBindingBuilder<Boolean> bool(Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return create(getter, setter).syncType(Boolean.class);
    }

    public static DataBindingBuilder<Float> floatVal(Supplier<Float> getter, Consumer<Float> setter) {
        return create(getter, setter).syncType(Float.class);
    }

    public static DataBindingBuilder<Double> doubleVal(Supplier<Double> getter, Consumer<Double> setter) {
        return create(getter, setter).syncType(Double.class);
    }

    public static DataBindingBuilder<Long> longVal(Supplier<Long> getter, Consumer<Long> setter) {
        return create(getter, setter).syncType(Long.class);
    }

    public static DataBindingBuilder<Byte> byteVal(Supplier<Byte> getter, Consumer<Byte> setter) {
        return create(getter, setter).syncType(Byte.class);
    }

    public static DataBindingBuilder<Short> shortVal(Supplier<Short> getter, Consumer<Short> setter) {
        return create(getter, setter).syncType(Short.class);
    }

    public static DataBindingBuilder<Character> charVal(Supplier<Character> getter, Consumer<Character> setter) {
        return create(getter, setter).syncType(Character.class);
    }

    public static <T extends Enum<?>> DataBindingBuilder<T> enumVal(Class<T> clazz, Supplier<T> getter, Consumer<T> setter) {
        return create(getter, setter).syncType(clazz);
    }

    public static DataBindingBuilder<String> string(Supplier<String> getter, Consumer<String> setter) {
        return create(getter, setter).syncType(String.class);
    }

}
