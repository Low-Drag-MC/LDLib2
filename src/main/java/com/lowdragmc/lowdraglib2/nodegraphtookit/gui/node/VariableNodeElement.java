package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphInspector;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.util.RenameColorConfigurableHelper;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.util.VariableDeclarationConfigurableHelper;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.VariableNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.ModifierFlags;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

public class VariableNodeElement extends CapsuleNodeElement {
    // runtime
    @Getter @Nullable
    private UIElement scopeImage;

    public VariableNodeElement(VariableNodeModel variableNodeModel) {
        super(variableNodeModel);
        addClass("__variable-node__");
    }

    @Override
    public VariableNodeModel getModel() {
        return (VariableNodeModel) super.getModel();
    }

    @Override
    protected void buildUI() {
        super.buildUI();

        scopeImage = new UIElement().addClass("__variable-node_scope-image__");
        Style.defaultPipeline(scopeImage.getLayout(), l -> l.width(2).height(12));
        addChildAt(scopeImage, 0);
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
                // Scope color is the typed-port color — model data, pin via IMPORTANT.
                Style.importantPipeline(scopeImage.getStyle(), s -> s.background(new ColorRectTexture(portContainer.portModel
                        .getDataTypeHandle()
                        .getTypeColor())));
            }
            // Visibility depends on whether the variable carries a modifier — model data.
            var hasModifier = variableDeclarationModel.getModifiers() != ModifierFlags.NONE;
            Style.importantPipeline(scopeImage.getLayout(), l -> l.display(hasModifier ? TaffyDisplay.FLEX : TaffyDisplay.NONE));
        }
    }

    @Override
    protected void onSelectionInspect(GraphInspector inspector) {
        if (graphView != null) inspector.setHistoryStack(graphView.getHistoryStack());
        var variableModel = getModel().getVariableDeclarationModel();
        // Inspector#inspect clears+replaces, so combine the node's own config (min-width) and the
        // variable declaration config (name / default value / internal-external) into one configurable
        // — mirrors what the Blackboard shows when the variable itself is selected.
        inspector.inspect(IConfigurable.create(group -> {
            RenameColorConfigurableHelper.build(getModel(), graphView).buildConfigurator(group);
            if (variableModel != null) {
                VariableDeclarationConfigurableHelper.build(variableModel, graphView).buildConfigurator(group);
            }
        }));
    }
}
