package com.lowdragmc.lowdraglib2.nodegraphtookit.model;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A model that represents a declaration (e.g. a variable) in a graph.
 */
public class DeclarationModel extends GraphElementModel implements IHasName {
    @Persisted @Getter
    public String name = "";

    public DeclarationModel() {
        capabilities.addAll(List.of(
                        Capabilities.DELETABLE,
                        Capabilities.DROPPABLE,
                        Capabilities.COPIABLE,
                        Capabilities.SELECTABLE,
                        Capabilities.RENAMABLE
//                Capabilities.EDITABLE,
                )
        );
    }

    @Override
    public void setName(String name) {
        if (this.name.equals(name)) return;
        this.name = name;
        if (graphModel != null) graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
    }
}
