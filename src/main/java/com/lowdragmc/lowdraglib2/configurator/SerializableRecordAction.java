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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@Accessors(chain = true)
public class SerializableRecordAction<T extends ValueIOSerializable> implements EditAction {
    public final T serializable;
    private final Function<T, CompoundTag> snapshotter;
    private final BiConsumer<T, CompoundTag> restorer;
    @Nullable
    @Setter
    private Consumer<T> onExecute;
    @Nullable
    @Setter
    private Consumer<T> onUndo;
    // runtime
    private CompoundTag snapshot;

    private SerializableRecordAction(T serializable, Function<T, CompoundTag> snapshotter, BiConsumer<T, CompoundTag> restorer) {
        this.serializable = serializable;
        this.snapshotter = snapshotter;
        this.restorer = restorer;
        this.snapshot = snapshotter.apply(serializable);
    }

    public static <T extends ValueIOSerializable> SerializableRecordAction<T> of(T serializable) {
        return new SerializableRecordAction<>(serializable, SerializableRecordAction::save, SerializableRecordAction::load);
    }

    /**
     * Records {@code serializable} through a custom snapshot / restore pair instead of its full serialized state.
     */
    public static <T extends ValueIOSerializable> SerializableRecordAction<T> of(T serializable, Function<T, CompoundTag> snapshotter, BiConsumer<T, CompoundTag> restorer) {
        return new SerializableRecordAction<>(serializable, snapshotter, restorer);
    }

    private static CompoundTag save(ValueIOSerializable serializable) {
        try (var reporter = new ProblemReporter.ScopedCollector(LDLib2.LOGGER)) {
            var valueOutput = TagValueOutput.createWithContext(reporter, Platform.getFrozenRegistry());
            serializable.serialize(valueOutput);
            return valueOutput.buildResult();
        }
    }

    private static void load(ValueIOSerializable serializable, CompoundTag snapshot) {
        try (var reporter = new ProblemReporter.ScopedCollector(LDLib2.LOGGER)) {
            serializable.deserialize(TagValueInput.create(reporter, Platform.getFrozenRegistry(), snapshot));
        }
    }

    public SerializableRecordAction<T> setOnAction(@Nullable Consumer<T> onAction) {
        setOnExecute(onAction);
        setOnUndo(onAction);
        return this;
    }

    public void updateSnapshot() {
        snapshot = snapshotter.apply(serializable);
    }

    public void loadSnapshot() {
        restorer.accept(serializable, snapshot);
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
