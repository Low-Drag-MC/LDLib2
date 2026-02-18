package com.lowdragmc.lowdraglib2.nodegraphtookit.model;

import net.minecraft.network.chat.Component;

public interface IHasDisplayName {
    /**
     * Get the title of the object.
     */
    Component getTitle();

    /**
     * Set the title of the object.
     */
    void setTitle(Component title);
}
