package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;

public class CapsuleNodeElement extends NodeElement {
    public CapsuleNodeElement(AbstractNodeModel nodeModel) {
        super(nodeModel);
        nodeOptionContainer.removeSelf();
        this.nodeInputPortContainer.getStyle().background(IGuiTexture.EMPTY);
        this.nodeOutputPortContainer.getStyle().background(IGuiTexture.EMPTY);
        nodeTitleBar.addChildAt(nodeInputPortContainer, 0);
        nodeTitleBar.addChild(nodeOutputPortContainer);
    }

    @Override
    public boolean hasModelDependenciesChanged() {
        // todo variable node
//        return model instanceof VariableNodeModel;
        return false;
    }

    @Override
    public void addModelDependencies() {
        // todo variable node
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        super.updateUIFromModel(visitor);
    }
}
