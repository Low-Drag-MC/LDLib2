package com.lowdragmc.lowdraglib2.configurator;

import com.lowdragmc.lowdraglib2.Platform;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

@Accessors(chain = true)
public class SerializableRecordAction<T extends INBTSerializable<?>> implements EditAction {
    public final T serializable;
    @Nullable
    @Setter
    private Consumer<T> onExecute;
    @Nullable
    @Setter
    private Consumer<T> onUndo;
    // runtime
    private Tag snapshot;

    private SerializableRecordAction(T serializable) {
        this.serializable = serializable;
        this.snapshot = serializable.serializeNBT(Platform.getFrozenRegistry());
    }

    public static <T extends INBTSerializable<?>> SerializableRecordAction<T> of(T serializable) {
        return new SerializableRecordAction<>(serializable);
    }

    public SerializableRecordAction<T> setOnAction(@Nullable Consumer<T> onAction) {
        setOnExecute(onAction);
        setOnUndo(onAction);
        return this;
    }

    public void updateSnapshot() {
        snapshot = serializable.serializeNBT(Platform.getFrozenRegistry());
    }

    @Override
    public void execute() {
        ((INBTSerializable)serializable).deserializeNBT(Platform.getFrozenRegistry(), snapshot);
        if (onExecute != null) {
            onExecute.accept(serializable);
        }
    }

    @Override
    public void undo() {
        ((INBTSerializable)serializable).deserializeNBT(Platform.getFrozenRegistry(), snapshot);
        if (onUndo != null) {
            onUndo.accept(serializable);
        }
    }
}
