package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;

public abstract class GraphElement<T extends GraphElementModel> extends ModelElement {

    public GraphElement(T model) {
        setModel(model);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getModel() {
        return (T) super.getModel();
    }

    @Override
    public boolean isSelectable() {
        return getModel().isSelectable();
    }
}
