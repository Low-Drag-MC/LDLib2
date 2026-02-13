package com.lowdragmc.lowdraglib2.nodegraphtookit.model;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;

/**
 * A model that represents a declaration (e.g. a variable) in a graph.
 */
public class DeclarationModel extends GraphElementModel implements IHasTitle {
    @Getter @Setter
    public Component title = Component.empty();
}
