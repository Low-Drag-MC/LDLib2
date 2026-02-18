package com.lowdragmc.lowdraglib2.nodegraphtookit.model;

import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphElement;
import org.jetbrains.annotations.Nullable;

public interface IGraphElementUIModel {
    default GraphElementModel asModel() {
        return (GraphElementModel) this;
    }

    /**
     * Creates and returns a UI representation of the graph element associated with this model.
     * The specific implementation for the UI representation depends on the subclass that provides
     * this method.
     *
     * @return an instance of {@link GraphElement}, or {@code null} if the graph element cannot
     *         be created or is not applicable for the current context.
     */
    @Nullable GraphElement<?> createElementUI();
}
