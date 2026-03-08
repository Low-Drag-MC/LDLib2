package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortDirection;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.VariableNodeElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.Capabilities;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.DeclarationModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.PlaceholderModelHelper;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.NodeDefinitionScope;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.ModifierFlags;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableDeclarationModelBase;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class VariableNodeModel extends NodeModel implements ISingleInputPortNodeModel, ISingleOutputPortNodeModel, IHasDeclarationModel {
    public static final String MAIN_PORT_ID = "main";
    @Nullable
    private VariableDeclarationModelBase declarationModel;
    @Persisted @Nullable
    private UUID declarationModelUid;
    @Nullable
    protected PortModel mainPortModel;

    public @Nullable UUID getDeclarationModelUid() {
        return declarationModelUid;
    }

    public VariableNodeModel() {
        setCapability(Capabilities.COLORABLE, false);
    }

    public void updateTypeFromDeclaration() {
        if (!Objects.equals(
                mainPortModel == null ? null : mainPortModel.getDataTypeHandle(),
                getVariableDeclarationModel().getDataTypeHandle()
        )) {
            defineNode();
            if (graphModel != null) {
                graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
            }
            // update connected nodes' ports colors/types
            if (mainPortModel != null) {
                for (var connectedPort : mainPortModel.getConnectedPorts()) {
                    connectedPort.getNodeModel().onConnection(connectedPort, mainPortModel);
                }
            }
        }
    }

    @Override
    public Component getTitle() {
        return Component.literal(getDeclarationModel() == null ? "" : getDeclarationModel().getName());
    }

    @Override
    protected void onDefineNode(NodeDefinitionScope<? extends NodeModel> scope) {
        if (declarationModel != null && declarationModel.getModifiers().hasFlag(ModifierFlags.WRITE)) {
            mainPortModel = scope.nodeModel.addInputPort(MAIN_PORT_ID, getDataType(), null, null, null, null, null);
        } else {
            mainPortModel = scope.nodeModel.addOutputPort(MAIN_PORT_ID, declarationModel == null ? TypeHandles.MISSING_PORT : getDataType(), null, null, null);
        }
        mainPortModel.setTitle(Component.empty());
    }

    public TypeHandle getDataType() {
        var variableDeclarationModel = getVariableDeclarationModel();
        if (variableDeclarationModel == null) return TypeHandles.UNKNOWN;
        return variableDeclarationModel.getDataTypeHandle();
    }

    public DeclarationModel getDeclarationModel() {
        if (declarationModel == null) {
            var result = PlaceholderModelHelper.tryGetPlaceholderGraphElementModel(graphModel, declarationModelUid);
            if (result.isSuccess()) {
                PlaceholderModelHelper.setPlaceholderCapabilities(this);
                return (DeclarationModel) result.getOrThrow();
            }
        }
        return declarationModel;
    }

    @Override
    public void setDeclarationModel(DeclarationModel declarationModel) {
        if (!(declarationModel instanceof VariableDeclarationModelBase)) return;
        if (this.declarationModel == declarationModel) return;
        this.declarationModel = (VariableDeclarationModelBase) declarationModel;
        this.declarationModelUid = declarationModel.getUid();
        if (graphModel != null) {
            graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
        }
        if (mainPortModel != null) {
            // We need to update the port type if the declaration model changes,
            // but only if the port has been created. This prevent a double call to OnDefineNode on creation.
            defineNode();
        }
    }

    public VariableDeclarationModelBase getVariableDeclarationModel() {
        return (VariableDeclarationModelBase) getDeclarationModel();
    }

    public void setVariableDeclarationModel(VariableDeclarationModelBase declarationModel) {
        setDeclarationModel(declarationModel);
    }

    @Override
    public PortModel getInputPort() {
        return mainPortModel == null ? null : mainPortModel.getDirection() == PortDirection.INPUT ? mainPortModel : null;
    }

    @Override
    public PortModel getOutputPort() {
        return mainPortModel == null ? null : mainPortModel.getDirection() == PortDirection.OUTPUT ? mainPortModel : null;
    }

    @Override
    public @Nullable GraphElement<?> createElementUI() {
        return new VariableNodeElement(this);
    }
}
