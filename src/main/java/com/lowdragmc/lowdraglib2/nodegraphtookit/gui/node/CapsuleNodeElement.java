package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.Model;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.*;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDirection;
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
        addClass("__capsule-node__");
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
        // ABSOLUTE positioning is data-driven (node coordinates) — pin via IMPORTANT.
        Style.importantPipeline(getLayout(), l -> l.positionType(TaffyPosition.ABSOLUTE));
        Style.defaultPipeline(getLayout(), l -> l.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER)
                .gapAll(2)
                .paddingAll(2));
        Style.defaultPipeline(getStyle(), s -> s.background(Sprites.RECT_SOLID));
        if (nodeTittle != null) {
            // Capsule design says the title has no background and zero padding — DEFAULT override of the title's own design.
            Style.defaultPipeline(nodeTittle.getStyle(), s -> s.background(IGuiTexture.EMPTY));
            Style.defaultPipeline(nodeTittle.getLayout(), l -> l.flexGrow(1).paddingVertical(0).paddingHorizontal(0));
        }
        addChildren(nodeTittle, constant);
    }

    @Override
    public boolean hasModelDependenciesChanged() {
        return getModel() instanceof VariableNodeModel
                || getModel() instanceof WirePortalModel;
    }

    @Override
    public void addModelDependencies() {
        if (getModel() instanceof VariableNodeModel variableNodeModel) {
            getDependencies().addModelDependency(variableNodeModel.getVariableDeclarationModel());
        } else if (getModel() instanceof WirePortalModel portalNode
                && portalNode.getDeclarationModel() != null) {
            // Portal title is read from the shared declaration; without this, renaming the
            // declaration leaves the portal's title stale.
            getDependencies().addModelDependency(portalNode.getDeclarationModel());
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
            Style.defaultPipeline(inputPortContainer.getPortContainer().getStyle(), s -> s.background(IGuiTexture.EMPTY));
            Style.defaultPipeline(inputPortContainer.getPortContainer().getLayout(), l -> l.paddingAll(0));
            addChild(inputPortContainer);
        } else if (inputPort == null && inputPortContainer != null) {
            parts.remove(inputPortContainer);
            inputPortContainer.setGraphView(null);
            inputPortContainer.removeSelf();
            inputPortContainer = null;
        }

        var outputPort = extractOutputPortModel(model);
        if (outputPort != null && outputPortContainer == null) {
            outputPortContainer = new SinglePortContainerElement(outputPort);
            parts.add(outputPortContainer);
            outputPortContainer.setGraphView(getGraphView());
            Style.defaultPipeline(outputPortContainer.getPortContainer().getStyle(), s -> s.background(IGuiTexture.EMPTY));
            Style.defaultPipeline(outputPortContainer.getPortContainer().getLayout(), l -> l.paddingAll(0));
            addChild(outputPortContainer);
        } else if (outputPort == null && outputPortContainer != null) {
            parts.remove(outputPortContainer);
            outputPortContainer.setGraphView(null);
            outputPortContainer.removeSelf();
            outputPortContainer = null;
        }

        // direction depends on which side the port is on — data-driven.
        Style.importantPipeline(getLayout(), l -> l.direction(outputPort != null ? TaffyDirection.LTR : TaffyDirection.RTL));
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
