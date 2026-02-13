package com.lowdragmc.lowdraglib2.nodegraphtookit.model;

import java.util.List;

/**
 * Interface for models that provide contextual menu items.
 */
public interface IHasContextualMenuItems {
    /**
     * Gets the contextual menu items for this model.
     *
     * @return the list of menu items
     */
    List<ContextualMenuItem> getContextualMenuItems();
}
