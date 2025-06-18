package com.lowdragmc.lowdraglib2.configurator;

import com.lowdragmc.lowdraglib2.Platform;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

public class SerializableRecordAction<T extends Tag> implements EditAction {
    private final INBTSerializable<T> serializable;
    private T snapshot;

    private SerializableRecordAction(INBTSerializable<T> serializable) {
        this.serializable = serializable;
        this.snapshot = serializable.serializeNBT(Platform.getFrozenRegistry());
    }

    public static <T extends Tag> SerializableRecordAction<T> of(INBTSerializable<T> serializable) {
        return new SerializableRecordAction<>(serializable);
    }

    @Override
    public void execute() {
        var currentSnapshot = serializable.serializeNBT(Platform.getFrozenRegistry());
        serializable.deserializeNBT(Platform.getFrozenRegistry(), snapshot);
        snapshot = currentSnapshot;
    }

    @Override
    public void undo() {
        var currentSnapshot = serializable.serializeNBT(Platform.getFrozenRegistry());
        serializable.deserializeNBT(Platform.getFrozenRegistry(), snapshot);
        snapshot = currentSnapshot;
    }
}
