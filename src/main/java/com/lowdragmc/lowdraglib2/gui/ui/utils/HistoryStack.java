package com.lowdragmc.lowdraglib2.gui.ui.utils;

import com.lowdragmc.lowdraglib2.configurator.EditAction;
import com.lowdragmc.lowdraglib2.configurator.SerializableRecordAction;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;
import java.util.List;

public class HistoryStack implements IHistoryStack {
    public static final int MAX_HISTORY_COUNT = 20;

    @Getter
    @Setter
    private int maxHistoryCount = MAX_HISTORY_COUNT;
    // runtime
    private final ObjectArrayList<HistoryItem> undoStack = new ObjectArrayList<>();
    private final ObjectArrayList<HistoryItem> redoStack = new ObjectArrayList<>();
    @Nullable
    @Getter
    private HistoryItem currentHistory;

    public List<HistoryItem> getUndoStack() {
        return undoStack;
    }

    public List<HistoryItem> getRedoStack() {
        return redoStack;
    }

    private static <T> void push(ObjectArrayList<T> stack, T value) {
        stack.add(value);
    }

    private static <T> T pop(ObjectArrayList<T> stack) {
        return stack.remove(stack.size() - 1);
    }

    private static <T> T peek(ObjectArrayList<T> stack) {
        return stack.get(stack.size() - 1);
    }

    public void pushHistory(Component name, EditAction action, @Nullable Object source, boolean execute) {
        if (execute) {
            action.execute();
        }
        boolean reuse = false;
        if (currentHistory != null) {
            if (!undoStack.isEmpty()) {
                var popped = pop(undoStack);
                if (popped.source() != null && popped.source().equals(source) && popped.name().equals(name)) {
                    // merge action here
                    if (popped.action() instanceof SerializableRecordAction<?> serializableRecord) {
                        serializableRecord.updateSnapshot();
                    } else {
                        popped = new HistoryItem(name, action.mergeExecuteAfter(popped.action()), source);
                    }
                    reuse = true;
                }
                push(undoStack, popped);
            }
            redoStack.clear();
        }
        HistoryItem newHistory;
        if (reuse) {
            newHistory = peek(undoStack);
            currentHistory = newHistory;
        } else {
            newHistory = new HistoryItem(name, action, source);
            currentHistory = newHistory;
            push(undoStack, currentHistory);
        }
        // update ui
        var ui = new Label().setText(name).textStyle(style -> {
            style.textAlignVertical(Vertical.CENTER);
            style.textWrap(TextWrap.HOVER_ROLL);
        }).layout(layout -> {
            layout.widthPercent(100);
        }).style(style -> {
            style.overlayTexture(ColorPattern.T_BLUE.rectTexture());
        }).addEventListener(UIEvents.MOUSE_DOWN, e -> {
            jumpToHistory(newHistory);
        });
        checkStackSize();
    }

    private void checkStackSize() {
        checkStackSize(undoStack);
        checkStackSize(redoStack);
    }

    private void checkStackSize(ObjectArrayList<HistoryItem> stack) {
        if (stack.size() > maxHistoryCount) {
            stack.removeElements(0, stack.size() - maxHistoryCount);
        }
    }

    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
        currentHistory = null;
    }

    public void undo() {
        if (undoStack.isEmpty()) return;
        var top = pop(undoStack);
        if (undoStack.isEmpty()) {
            push(undoStack, top);
            return;
        }
        var historyItem = peek(undoStack);
        push(undoStack, top);
        jumpToHistory(historyItem);
    }

    public void redo() {
        if (redoStack.isEmpty()) return;
        var historyItem = peek(redoStack);
        jumpToHistory(historyItem);
    }

    public void jumpToHistory(HistoryItem historyItem) {
        if (currentHistory == historyItem) return;
        if (undoStack.contains(historyItem)) {
            while (peek(undoStack) != historyItem) {
                var popped = pop(undoStack);
                popped.action().undo();
                push(redoStack, popped);
            }
            currentHistory = peek(undoStack);
            if (currentHistory.action() instanceof SerializableRecordAction<?> serializableRecord) {
                serializableRecord.execute();
            }
        } else if (redoStack.contains(historyItem)) {
            while (peek(redoStack) != historyItem) {
                var popped = pop(redoStack);
                popped.action().execute();
                push(undoStack, popped);
            }
            currentHistory = pop(redoStack);
            currentHistory.action().execute();
            push(undoStack, currentHistory);
        }
        checkStackSize();
    }
}
