package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.util;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ColorConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.ElementRenameColorCommands;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.IHasElementColor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.IHasName;

/**
 * Builds the rename + color configurator group for a graph element model. Used by the various
 * GraphElement subclasses to populate the {@code GraphInspector} when a single element is
 * selected. Both fields are gated on the relevant capability + interface and dispatch through
 * undoable commands.
 */
public final class RenameColorConfigurableHelper {
    private RenameColorConfigurableHelper() {}

    public static IConfigurable build(GraphElementModel model, GraphView view) {
        return IConfigurable.create(group -> {
            if (model.isRenamable() && model instanceof IHasName named) {
                group.addConfigurator(new StringConfigurator(
                        "graph.name",
                        named::getName,
                        newName -> {
                            if (view != null) {
                                view.dispatchCommand(new ElementRenameColorCommands.RenameElementCommand(model, newName));
                            } else {
                                named.setName(newName);
                            }
                        },
                        named.getName(),
                        true));
            }
            if (model.isColorable() && model instanceof IHasElementColor colored) {
                group.addConfigurator(new ColorConfigurator(
                        "graph.color",
                        colored::getElementColor,
                        newColor -> {
                            if (view != null) {
                                view.dispatchCommand(new ElementRenameColorCommands.SetElementColorCommand(model, newColor));
                            } else {
                                colored.setColor(newColor);
                            }
                        },
                        colored.getDefaultColor(),
                        true));
            }
        });
    }
}
