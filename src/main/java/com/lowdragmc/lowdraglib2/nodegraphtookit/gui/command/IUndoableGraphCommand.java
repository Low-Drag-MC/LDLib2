package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command;

import com.lowdragmc.lowdraglib2.gui.ui.utils.HistoryStack;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IUndoableGraphCommand extends IGraphCommand {
    @Override
    default void execute(@NotNull GraphView view, @NotNull GraphModel graphModel) {
        execute(view, graphModel, view.getHistoryStack());
    }

    /**
     * Executes a command by applying an edit action and storing it in the history stack.
     *
     * @param view the {@link GraphView} representing the current state of the graph; must not be {@code null}.
     * @param graphModel the {@link GraphModel} representing the structure of the graph; must not be {@code null}.
     * @param historyStack the {@link HistoryStack} to record the command's execution for undo/redo functionality; must not be {@code null}.
     */
    void execute(@NotNull GraphView view, @NotNull GraphModel graphModel, @NotNull HistoryStack historyStack);

    /**
     * Retrieves the name or identifier of the command as a {@link Component}.
     * This name is typically used for display purposes or logging within the
     * context of an undoable or graph-modifying command.
     *
     * @return a {@link Component} representing the name or identifier of the command.
     */
    Component getCommandName();

    /**
     * Retrieves the source object associated with the command, if any.
     * The source object can represent mergeable commands.
     *
     * @return the source object associated with the command, or {@code null} if no source is specified.
     */
    @Nullable
    default Object getSource() {
        return null;
    }
}
