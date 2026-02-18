package com.lowdragmc.lowdraglib2.gui.ui.utils;

import com.lowdragmc.lowdraglib2.configurator.EditAction;
import com.lowdragmc.lowdraglib2.configurator.SerializableRecordAction;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.util.INBTSerializable;

import org.jetbrains.annotations.Nullable;

public interface IHistoryStack {
    record HistoryItem(Component name, EditAction action, @Nullable Object source) { }

    default <T extends INBTSerializable<?>> SerializableRecordAction<T> recordSerializableObject(Component name, T object) {
        return recordSerializableObject(name, object, null);
    }

    default <T extends INBTSerializable<?>> SerializableRecordAction<T> recordSerializableObject(Component name, T object, @Nullable Object source) {
        var recordAction = SerializableRecordAction.of(object);
        pushHistory(name, recordAction, source, false);
        return recordAction;
    }

    default void pushHistory(Component name, EditAction action) {
        pushHistory(name, action, null, true);
    }

    default void pushHistory(Component name, EditAction action, boolean execute) {
        pushHistory(name, action, null, execute);
    }

    void pushHistory(Component name, EditAction action, @Nullable Object source, boolean execute);

    void undo();

    void redo();

    void jumpToHistory(HistoryItem historyItem);
}
