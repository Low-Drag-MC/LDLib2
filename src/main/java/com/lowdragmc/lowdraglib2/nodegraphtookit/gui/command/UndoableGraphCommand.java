package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.EditAction;
import com.lowdragmc.lowdraglib2.gui.ui.utils.HistoryStack;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jetbrains.annotations.NotNull;

public abstract class UndoableGraphCommand implements IUndoableGraphCommand {
    // runtime
    protected GraphView view;
    protected GraphModel graphModel;

    @Override
    public void execute(@NotNull GraphView view, @NotNull GraphModel graphModel, @NotNull HistoryStack historyStack) {
        this.view = view;
        this.graphModel = graphModel;
        generalActionData();

        var provider = Platform.getFrozenRegistry();
        var beforeTag = serializeGraph(graphModel);

        execute();

        var afterTag = serializeGraph(graphModel);

        historyStack.pushHistory(getCommandName(), EditAction.of(
                () -> { deserializeGraph(graphModel, afterTag); view.rebuildGraphUI(); },
                () -> { deserializeGraph(graphModel, beforeTag); view.rebuildGraphUI(); }
        ), getSource(), false);
    }

    /**
     * Executes the forward action of this command.
     * Undo is handled by the snapshot mechanism.
     */
    public abstract void execute();

    protected void generalActionData() {

    }

    private static CompoundTag serializeGraph(GraphModel graphModel) {
        var provider = Platform.getFrozenRegistry();
        var output = TagValueOutput.createWithContext(ProblemReporter.Collector.DISCARDING, provider);
        graphModel.serialize(output);
        return output.buildResult();
    }

    private static void deserializeGraph(GraphModel graphModel, CompoundTag tag) {
        var provider = Platform.getFrozenRegistry();
        graphModel.deserialize(TagValueInput.create(ProblemReporter.Collector.DISCARDING, provider, tag));
    }
}
