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
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaGutter;

import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryView extends View implements IHistoryStack {
    public static final int MAX_HISTORY_COUNT = 20;

    public final ScrollerView scrollerView = new ScrollerView();
    public final Editor editor;

    @Getter
    @Setter
    private int maxHistoryCount = MAX_HISTORY_COUNT;
    // runtime
    private final ObjectArrayList<HistoryItem> undoStack = new ObjectArrayList<>();
    private final ObjectArrayList<HistoryItem> redoStack = new ObjectArrayList<>();
    @Nullable
    @Getter
    private HistoryItem currentHistory;
    private final Map<HistoryItem, UIElement> historyUIs = new HashMap<>();

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

    private void checkStackSize(ObjectArrayList<HistoryItem> stack) {
        if (stack.size() > maxHistoryCount) {
            for (int i = 0, removeCount = stack.size() - maxHistoryCount; i < removeCount; i++) {
                var historyItem = stack.get(i);
                var ui = historyUIs.get(historyItem);
                if (ui != null) {
                    scrollerView.removeScrollViewChild(ui);
                }
                historyUIs.remove(historyItem);
            }
            stack.removeElements(0, stack.size() - maxHistoryCount);
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
            newHistory = peek(undoStack);
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
        historyUIs.put(newHistory, ui);
        scrollerView.addScrollViewChild(ui);
        checkStackSize();
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
        if (currentHistory != null) {
            var ui = historyUIs.get(currentHistory);
            if (ui != null) {
                ui.style(style -> style.overlayTexture(IGuiTexture.EMPTY));
            }
        }
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
        if (currentHistory != null) {
            var ui = historyUIs.get(currentHistory);
            if (ui != null) {
                ui.style(style -> style.overlayTexture(ColorPattern.T_BLUE.rectTexture()));
            }
        }
        checkStackSize();
    }
}
