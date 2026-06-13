package com.lowdragmc.lowdraglib2.gui.ui.utils;

import com.lowdragmc.lowdraglib2.configurator.EditAction;
import com.lowdragmc.lowdraglib2.configurator.SerializableRecordAction;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;
import java.util.Stack;

public class HistoryStack implements IHistoryStack {
    public static final int MAX_HISTORY_COUNT = 20;

    @Getter
    @Setter
    private int maxHistoryCount = MAX_HISTORY_COUNT;
    // runtime
    @Getter
    private final Stack<HistoryItem> undoStack = new Stack<>();
    @Getter
    private final Stack<HistoryItem> redoStack = new Stack<>();
    @Nullable
    @Getter
    private HistoryItem currentHistory;

    public void pushHistory(Component name, EditAction action, @Nullable Object source, boolean execute) {
        if (execute) {
            action.execute();
        }
        boolean reuse = false;
        if (currentHistory != null) {
            if (!undoStack.isEmpty()) {
                var popped = undoStack.pop();
                if (popped.source() != null && popped.source().equals(source) && popped.name().equals(name)) {
                    // merge action here
                    if (popped.action() instanceof SerializableRecordAction<?> serializableRecord) {
                        serializableRecord.updateSnapshot();
                    } else {
                        popped = new HistoryItem(name, action.mergeExecuteAfter(popped.action()), source);
                    }
                    reuse = true;
                }
                undoStack.push(popped);
            }
            redoStack.clear();
        }
        HistoryItem newHistory;
        if (reuse) {
            newHistory = undoStack.peek();
            currentHistory = newHistory;
        } else {
            newHistory = new HistoryItem(name, action, source);
            currentHistory = newHistory;
            undoStack.push(currentHistory);
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

    private void checkStackSize(Stack<HistoryItem> stack) {
        if (stack.size() > maxHistoryCount) {
            // Remove only the excess items
            var toRemove = stack.subList(0, stack.size() - maxHistoryCount);
            toRemove.clear();
        }
    }

    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
        currentHistory = null;
    }

    public void undo() {
        if (undoStack.isEmpty()) return;
        var top = undoStack.pop();
        if (undoStack.isEmpty()) {
            undoStack.push(top);
            return;
        }
        var historyItem = undoStack.peek();
        undoStack.push(top);
        jumpToHistory(historyItem);
    }

    public void redo() {
        if (redoStack.isEmpty()) return;
        var historyItem = redoStack.peek();
        jumpToHistory(historyItem);
    }

    public void jumpToHistory(HistoryItem historyItem) {
        if (currentHistory == historyItem) return;
        if (undoStack.contains(historyItem)) {
            while(undoStack.peek() != historyItem) {
                var popped = undoStack.pop();
                popped.action().undo();
                redoStack.push(popped);
            }
            currentHistory = undoStack.peek();
            if (currentHistory.action() instanceof SerializableRecordAction<?> serializableRecord) {
                serializableRecord.execute();
            }
        } else if (redoStack.contains(historyItem)) {
            while (redoStack.peek() != historyItem) {
                var popped = redoStack.pop();
                popped.action().execute();
                undoStack.push(popped);
            }
            currentHistory = redoStack.pop();
            currentHistory.action().execute();
            undoStack.push(currentHistory);
        }
        checkStackSize();
    }
}
