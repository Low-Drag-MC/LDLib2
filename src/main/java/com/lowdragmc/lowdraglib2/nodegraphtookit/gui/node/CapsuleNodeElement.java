package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.Model;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.*;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

public class CapsuleNodeElement extends NodeElement {
    @Getter
    @Nullable
    protected ConstantNodeEditorElement constant;
    @Getter
    @Nullable
    protected SinglePortContainerElement inputPortContainer;
    @Getter
    @Nullable
    protected SinglePortContainerElement outputPortContainer;

    public CapsuleNodeElement(AbstractNodeModel nodeModel) {
        super(nodeModel);
    }

    @Override
    protected void buildPartList() {
        parts.add(this.nodeTittle = new NodeTitleElement(getModel()));
        if (getModel() instanceof ConstantNodeModel constantNodeModel) {
            parts.add(this.constant = new ConstantNodeEditorElement(constantNodeModel));
        }
    }

    @Override
    protected void buildUI() {
        getLayout().positionType(TaffyPosition.ABSOLUTE).flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER)
                .paddingAll(2);
        getStyle().background(Sprites.RECT_SOLID);
        if (nodeTittle != null) {
            nodeTittle.getStyle().background(IGuiTexture.EMPTY);
            nodeTittle.getLayout().flexGrow(1).paddingVertical(0).paddingHorizontal(0);;
        }
        addChildren(nodeTittle, constant);
        internalSetup();
    }

    @Override
    public boolean hasModelDependenciesChanged() {
        return getModel() instanceof VariableNodeModel;
    }

    @Override
    public void addModelDependencies() {
        if (getModel() instanceof VariableNodeModel variableNodeModel) {
            getDependencies().addModelDependency(variableNodeModel.getVariableDeclarationModel());
        }
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        super.updateUIFromModel(visitor);

        var model = getModel();
        var inputPort = extractInputPortModel(model);
        if (inputPort != null && inputPortContainer == null) {
            inputPortContainer = new SinglePortContainerElement(inputPort);
            parts.add(inputPortContainer);
            inputPortContainer.setGraphView(getGraphView());
            inputPortContainer.getPortContainer().getStyle().background(IGuiTexture.EMPTY);
            inputPortContainer.getPortContainer().getLayout().paddingAll(2);
            addChild(inputPortContainer);
        } else if (inputPort == null && inputPortContainer != null) {
            parts.remove(inputPortContainer);
            inputPortContainer.setGraphView(null);
            inputPortContainer.removeSelf();
        }

        var outputPort = extractOutputPortModel(model);
        if (outputPort != null && outputPortContainer == null) {
            outputPortContainer = new SinglePortContainerElement(outputPort);
            parts.add(outputPortContainer);
            outputPortContainer.setGraphView(getGraphView());
            outputPortContainer.getPortContainer().getStyle().background(IGuiTexture.EMPTY);
            outputPortContainer.getPortContainer().getLayout().paddingAll(2);
            addChild(outputPortContainer);
        } else if (outputPort == null && outputPortContainer != null) {
            parts.remove(outputPortContainer);
            outputPortContainer.setGraphView(null);
            outputPortContainer.removeSelf();
        }
    }

    protected static PortModel extractInputPortModel(Model model) {
        if (model instanceof ISingleInputPortNodeModel inputPortHolder && inputPortHolder.getInputPort() != null) {
            return inputPortHolder.getInputPort();
        }
        return null;
    }

    protected static PortModel extractOutputPortModel(Model model) {
        if (model instanceof ISingleOutputPortNodeModel outputPortHolder && outputPortHolder.getOutputPort() != null) {
            return outputPortHolder.getOutputPort();
        }
        return null;
    }
}
