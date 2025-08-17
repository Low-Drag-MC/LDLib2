package com.lowdragmc.lowdraglib2.gui.sync.bindings.impl;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.IDataBindingHolder;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.SyncStrategy;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Accessors(chain = true, fluent = true)
public class DataBindingBuilder<T> {
    private final String name;
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
    @Nullable
    private T initialValue;

    protected DataBindingBuilder(String name, Supplier<T> getter, Consumer<T> setter) {
        this.name = name;
        this.getter = getter;
        this.setter = setter;
    }

    public static <T> DataBindingBuilder<T> create(String name, Supplier<T> getter, Consumer<T> setter) {
        return new DataBindingBuilder<>(name, getter, setter);
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

    public IDataBindingHolder<T> build(boolean isRemote) {
        Objects.requireNonNull(getter);

        if (type == null) {
            type = getter.get().getClass();
        }

        if (isRemote) {
            if (initialValue == null) {
                initialValue = getter.get();
            }
            var binding = new SimpleBinding<>(name, type, initialValue);
            binding.setC2sStrategy(c2sStrategy);
            binding.setAcceptS2C(s2cStrategy.doSync());
            return new IDataBindingHolder.Binding<>(binding);
        } else {
            Objects.requireNonNull(setter);
            var data = new SimpleData<>(name, type, setter, getter);
            data.setS2cStrategy(s2cStrategy);
            data.setAcceptC2S(c2sStrategy.doSync());
            return new IDataBindingHolder.Data<>(data);
        }
    }

    ///  Built-in
    /**
     * Creates and returns a data binding holder for {@link ItemStack} that supports synchronization
     * and data manipulation using the provided getter and setter functions.
     */
    public static DataBindingBuilder<ItemStack> itemStack(String name, Supplier<ItemStack> getter, Consumer<ItemStack> setter) {
        return create(name, getter, setter).syncType(ItemStack.class);
    }

    /**
     * Creates a data binding holder for {@link ItemStack} that supports synchronization from
     * server to client (S2C). This binding is configured with a {@code SyncStrategy.NONE},
     * meaning no synchronization will occur for the client to server (C2S) to avoid attacks.
     */
    public static DataBindingBuilder<ItemStack> itemStackS2C(String name, Supplier<ItemStack> getter, Consumer<ItemStack> setter) {
        return itemStack(name, getter, setter).c2sStrategy(SyncStrategy.NONE);
    }

    /**
     * Generates an array of {@code IDataBindingHolder<ItemStack>} objects for an {@code Inventory}'s items.
     * Each binding holder is configured for server-to-client (S2C) synchronization only. you have to manage server inventory yourself.
     *
     * @param inv the {@code Inventory} instance from which the item bindings are generated
     * @return an array of {@code IDataBindingHolder<ItemStack>} representing the bindings for the inventory items
     */
    public static DataBindingBuilder<ItemStack>[] inventory(Inventory inv) {
        var holders = new DataBindingBuilder[inv.items.size()];
        for (int i = 0; i < inv.items.size(); i++) {
            int slotIndex = i;
            holders[i] = itemStackS2C("@inventory_%d".formatted(i), () -> inv.items.get(slotIndex), item -> inv.items.set(slotIndex, item));
        }
        return holders;
    }

    public static DataBindingBuilder<Integer> intVal(String name, Supplier<Integer> getter, Consumer<Integer> setter) {
        return create(name, getter, setter).syncType(Integer.class);
    }

    public static DataBindingBuilder<Boolean> bool(String name, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return create(name, getter, setter).syncType(Boolean.class);
    }

    public static DataBindingBuilder<Float> floatVal(String name, Supplier<Float> getter, Consumer<Float> setter) {
        return create(name, getter, setter).syncType(Float.class);
    }

    public static DataBindingBuilder<Double> doubleVal(String name, Supplier<Double> getter, Consumer<Double> setter) {
        return create(name, getter, setter).syncType(Double.class);
    }

    public static DataBindingBuilder<Long> longVal(String name, Supplier<Long> getter, Consumer<Long> setter) {
        return create(name, getter, setter).syncType(Long.class);
    }

    public static DataBindingBuilder<Byte> byteVal(String name, Supplier<Byte> getter, Consumer<Byte> setter) {
        return create(name, getter, setter).syncType(Byte.class);
    }

    public static DataBindingBuilder<Short> shortVal(String name, Supplier<Short> getter, Consumer<Short> setter) {
        return create(name, getter, setter).syncType(Short.class);
    }

    public static DataBindingBuilder<Character> charVal(String name, Supplier<Character> getter, Consumer<Character> setter) {
        return create(name, getter, setter).syncType(Character.class);
    }

    public static <T extends Enum<?>> DataBindingBuilder<T> enumVal(String name, Class<T> clazz, Supplier<T> getter, Consumer<T> setter) {
        return create(name, getter, setter).syncType(clazz);
    }

    public static DataBindingBuilder<String> string(String name, Supplier<String> getter, Consumer<String> setter) {
        return create(name, getter, setter).syncType(String.class);
    }

}
