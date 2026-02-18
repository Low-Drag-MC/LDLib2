package com.lowdragmc.lowdraglib2.nodegraphtookit.model;

import java.util.Collection;
import java.util.List;

/**
 * A container for graph elements.
 */
public interface IGraphElementContainer {
    /**
     * Gets the contained graph element models.
     *
     * @return the graph element models contained in this container
     */
    List<GraphElementModel> getGraphElementModels();

    /**
     * Removes the given graph element models from the container.
     * @param elementsToRemove The graph element models to remove.
     */
    void removeContainerElements(Collection<? extends GraphElementModel> elementsToRemove);

    /**
     * Repairs the container by removing invalid or null references.
     *
     * @return {@code true} if the container was modified during the repair;
     *         {@code false} otherwise
     */
    boolean repair();
}
