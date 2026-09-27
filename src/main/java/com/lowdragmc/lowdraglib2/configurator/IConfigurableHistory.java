package com.lowdragmc.lowdraglib2.configurator;

import com.lowdragmc.lowdraglib2.gui.ui.utils.IHistoryStack;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Strategy for recording {@link IConfigurable} edits into a {@link IHistoryStack}.
 * <p>
 * Returned by {@link IConfigurable#createHistoryRecorder()}; if a configurable returns {@code null}
 * the inspector skips history recording for it.
 */
public interface IConfigurableHistory {

    /**
     * Push a history entry into the stack and return a {@link Handle} whose execute/undo callbacks can be wired.
     *
     * @param stack  target history stack
     * @param name   display name for the history entry
     * @param source optional grouping source (typically the configurator or configurable triggering the change)
     */
    Handle record(IHistoryStack stack, Component name, @Nullable Object source);

    interface Handle {
        Handle setOnExecute(@Nullable Runnable onExecute);

        Handle setOnUndo(@Nullable Runnable onUndo);
    }

    /**
     * Default snapshot-based recorder backed by {@link SerializableRecordAction}.
     */
    static <T extends INBTSerializable<?>> IConfigurableHistory ofSerializable(T serializable) {
        return (stack, name, source) -> handleOf(stack.recordSerializableObject(name, serializable, source));
    }

    /**
     * Same as {@link #ofSerializable}, but snapshots and restores through {@code snapshotter} / {@code restorer}
     * rather than the object's full NBT.
     */
    static <T extends INBTSerializable<?>> IConfigurableHistory ofSnapshot(T serializable, Function<T, Tag> snapshotter, BiConsumer<T, Tag> restorer) {
        return (stack, name, source) -> {
            var action = SerializableRecordAction.of(serializable, snapshotter, restorer);
            stack.pushHistory(name, action, source, false);
            return handleOf(action);
        };
    }

    private static Handle handleOf(SerializableRecordAction<?> action) {
        return new Handle() {
            @Override
            public Handle setOnExecute(@Nullable Runnable onExecute) {
                action.setOnExecute(onExecute == null ? null : value -> onExecute.run());
                return this;
            }

            @Override
            public Handle setOnUndo(@Nullable Runnable onUndo) {
                action.setOnUndo(onUndo == null ? null : value -> onUndo.run());
                return this;
            }
        };
    }
}
