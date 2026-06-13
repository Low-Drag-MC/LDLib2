package com.lowdragmc.lowdraglib2.editor.ui.view;

import com.lowdragmc.lowdraglib2.configurator.EditAction;
import com.lowdragmc.lowdraglib2.configurator.SerializableRecordAction;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.CommandEvents;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.utils.IHistoryStack;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class HistoryView extends View implements IHistoryStack {
    public static final int MAX_HISTORY_COUNT = 20;

    public final ScrollerView scrollerView = new ScrollerView();
    public final Editor editor;

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
    private final Map<HistoryItem, UIElement> historyUIs = new HashMap<>();

    public HistoryView(Editor editor) {
        super("editor.view.history", Icons.HISTORY);
        this.editor = editor;
        scrollerView.layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
        });
        scrollerView.viewPort.layout(layout -> {
            layout.paddingAll(1);
        }).style(style -> style.backgroundTexture(IGuiTexture.EMPTY));;
        scrollerView.viewContainer.layout(layout -> {
            layout.gapAll(1);
        });
        addChild(scrollerView);

        // history may be invisible
        editor.addEventListener(UIEvents.VALIDATE_COMMAND, this::onValidateCommand);
        editor.addEventListener(UIEvents.EXECUTE_COMMAND, this::onExecuteCommand);
    }

    protected void onValidateCommand(UIEvent event) {
        if (CommandEvents.REDO.equals(event.command) && !redoStack.isEmpty()) {
            event.stopPropagation();
        }
        if (CommandEvents.UNDO.equals(event.command) && !undoStack.isEmpty()) {
            event.stopPropagation();
        }
    }

    protected void onExecuteCommand(UIEvent event) {
        if (CommandEvents.REDO.equals(event.command) && !redoStack.isEmpty()) {
            redo();
        }
        if (CommandEvents.UNDO.equals(event.command) && !undoStack.isEmpty()) {
            undo();
        }
    }

    private void checkStackSize() {
        checkStackSize(undoStack);
        checkStackSize(redoStack);
    }

    private void checkStackSize(Stack<HistoryItem> stack) {
        if (stack.size() > maxHistoryCount) {
            // Remove only the excess items
            var toRemove = stack.subList(0, stack.size() - maxHistoryCount);
            for (HistoryItem historyItem : toRemove) {
                var ui = historyUIs.get(historyItem);
                if (ui != null) {
                    scrollerView.removeScrollViewChild(ui);
                }
                historyUIs.remove(historyItem);
            }
            toRemove.clear();
        }
    }

    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
        currentHistory = null;
        scrollerView.clearAllScrollViewChildren();
        historyUIs.clear();
    }

    public void pushHistory(Component name, EditAction action) {
        pushHistory(name, action, null, true);
    }

    public void pushHistory(Component name, EditAction action, boolean execute) {
        pushHistory(name, action, null, execute);
    }

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
            for (HistoryItem historyItem : redoStack) {
                var ui = historyUIs.get(historyItem);
                if (ui != null) {
                    scrollerView.viewContainer.removeChild(ui);
                }
                historyUIs.remove(historyItem);
            }
            redoStack.clear();
        }
        HistoryItem newHistory;
        if (reuse) {
            var ui = historyUIs.remove(currentHistory);
            if (ui != null) {
                scrollerView.viewContainer.removeChild(ui);
            }
            newHistory = undoStack.peek();
            currentHistory = newHistory;
        } else {
            if (currentHistory != null) {
                var ui = historyUIs.get(currentHistory);
                if (ui != null) {
                    ui.style(style -> style.overlayTexture(IGuiTexture.EMPTY));
                }
            }
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
        historyUIs.put(newHistory, ui);
        scrollerView.addScrollViewChild(ui);
        checkStackSize();
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
        if (currentHistory != null) {
            var ui = historyUIs.get(currentHistory);
            if (ui != null) {
                ui.style(style -> style.overlayTexture(IGuiTexture.EMPTY));
            }
        }
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
        if (currentHistory != null) {
            var ui = historyUIs.get(currentHistory);
            if (ui != null) {
                ui.style(style -> style.overlayTexture(ColorPattern.T_BLUE.rectTexture()));
            }
        }
        checkStackSize();
    }
}
