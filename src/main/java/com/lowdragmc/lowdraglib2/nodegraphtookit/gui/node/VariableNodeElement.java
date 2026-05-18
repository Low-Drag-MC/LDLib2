package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.VariableNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.ModifierFlags;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

public class VariableNodeElement extends CapsuleNodeElement {
    // runtime
    @Getter @Nullable
    private UIElement scopeImage;

    public VariableNodeElement(VariableNodeModel variableNodeModel) {
        super(variableNodeModel);
    }

    @Override
    public VariableNodeModel getModel() {
        return (VariableNodeModel) super.getModel();
    }

    @Override
    protected void buildUI() {
        super.buildUI();

        scopeImage = new UIElement();
        scopeImage.getLayout().width(2).height(12);
        addChildAt(scopeImage, 0);
        internalSetup();
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        super.updateUIFromModel(visitor);

        var variableDeclarationModel = getModel().getVariableDeclarationModel();
        if (variableDeclarationModel == null) return;
        var portContainer = getParts().stream()
                .filter(SinglePortContainerElement.class::isInstance)
                .map(SinglePortContainerElement.class::cast).findFirst().orElse(null);
        if (scopeImage != null) {
            if (portContainer != null) {
                scopeImage.getStyle().background(new ColorRectTexture(portContainer.portModel
                        .getDataTypeHandle()
                        .getTypeColor()));
            }
            scopeImage.setDisplay(variableDeclarationModel.getModifiers() != ModifierFlags.NONE);
        }
    }
}
