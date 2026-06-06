package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command;

import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;

/**
 * Observer notified after an {@link IGraphCommand} has executed on a {@link GraphView}. Register via
 * {@link GraphView#addCommandListener(GraphCommandListener)}. Use it to react to applied edits —
 * custom side effects, analytics, extra dirty-tracking, etc. Listeners run only when a command was
 * actually executed (not when it was vetoed).
 */
@FunctionalInterface
public interface GraphCommandListener {
    void onCommandExecuted(IGraphCommand command, GraphView view, GraphModel graphModel);
}
