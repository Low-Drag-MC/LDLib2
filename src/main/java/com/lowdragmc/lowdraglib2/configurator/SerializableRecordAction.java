package com.lowdragmc.lowdraglib2.configurator;

import com.lowdragmc.lowdraglib2.Platform;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import org.jetbrains.annotations.Nullable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@Accessors(chain = true)
public class SerializableRecordAction<T extends INBTSerializable<?>> implements EditAction {
    public final T serializable;
    private final Function<T, Tag> snapshotter;
    private final BiConsumer<T, Tag> restorer;
    @Nullable
    @Setter
    private Consumer<T> onExecute;
    @Nullable
    @Setter
    private Consumer<T> onUndo;
    // runtime
    private Tag snapshot;

    private SerializableRecordAction(T serializable, Function<T, Tag> snapshotter, BiConsumer<T, Tag> restorer) {
        this.serializable = serializable;
        this.snapshotter = snapshotter;
        this.restorer = restorer;
        this.snapshot = snapshotter.apply(serializable);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T extends INBTSerializable<?>> SerializableRecordAction<T> of(T serializable) {
        return new SerializableRecordAction<>(serializable,
                value -> value.serializeNBT(Platform.getFrozenRegistry()),
                (value, tag) -> ((INBTSerializable) value).deserializeNBT(Platform.getFrozenRegistry(), tag));
    }

    /**
     * Records {@code serializable} through a custom snapshot / restore pair instead of its full NBT.
     */
    public static <T extends INBTSerializable<?>> SerializableRecordAction<T> of(T serializable, Function<T, Tag> snapshotter, BiConsumer<T, Tag> restorer) {
        return new SerializableRecordAction<>(serializable, snapshotter, restorer);
    }

    public SerializableRecordAction<T> setOnAction(@Nullable Consumer<T> onAction) {
        setOnExecute(onAction);
        setOnUndo(onAction);
        return this;
    }

    public void updateSnapshot() {
        snapshot = snapshotter.apply(serializable);
    }

    @Override
    public void execute() {
        restorer.accept(serializable, snapshot);
        if (onExecute != null) {
            onExecute.accept(serializable);
        }
    }

    @Override
    public void undo() {
        restorer.accept(serializable, snapshot);
        if (onUndo != null) {
            onUndo.accept(serializable);
        }
    }
}
