package com.lowdragmc.lowdraglib2.configurator;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.nbt.CompoundTag;

import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

@Accessors(chain = true)
public class SerializableRecordAction<T extends ValueIOSerializable> implements EditAction {
    public final T serializable;
    @Nullable
    @Setter
    private Consumer<T> onExecute;
    @Nullable
    @Setter
    private Consumer<T> onUndo;
    // runtime
    private CompoundTag snapshot;

    private SerializableRecordAction(T serializable) {
        this.serializable = serializable;
        updateSnapshot();
        try (var reporter = new ProblemReporter.ScopedCollector(LDLib2.LOGGER)) {
            var valueOutput = TagValueOutput.createWithContext(reporter, Platform.getFrozenRegistry());
            serializable.serialize(valueOutput);
            this.snapshot = valueOutput.buildResult();
        }
    }

    public static <T extends ValueIOSerializable> SerializableRecordAction<T> of(T serializable) {
        return new SerializableRecordAction<>(serializable);
    }

    public SerializableRecordAction<T> setOnAction(@Nullable Consumer<T> onAction) {
        setOnExecute(onAction);
        setOnUndo(onAction);
        return this;
    }

    public void updateSnapshot() {
        try (var reporter = new ProblemReporter.ScopedCollector(LDLib2.LOGGER)) {
            var valueOutput = TagValueOutput.createWithContext(reporter, Platform.getFrozenRegistry());
            serializable.serialize(valueOutput);
            this.snapshot = valueOutput.buildResult();
        }
    }


    public void loadSnapshot() {
        try (var reporter = new ProblemReporter.ScopedCollector(LDLib2.LOGGER)) {
            var valueInput = TagValueInput.create(reporter, Platform.getFrozenRegistry(), snapshot);
            serializable.deserialize(valueInput);
        }
    }

    @Override
    public void execute() {
        loadSnapshot();
        if (onExecute != null) {
            onExecute.accept(serializable);
        }
    }

    @Override
    public void undo() {
        loadSnapshot();
        if (onUndo != null) {
            onUndo.accept(serializable);
        }
    }
}
