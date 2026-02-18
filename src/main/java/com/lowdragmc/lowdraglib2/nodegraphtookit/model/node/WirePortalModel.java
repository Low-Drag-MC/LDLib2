package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortType;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandleHelpers;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.CapsuleNodeElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.Capabilities;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.DeclarationModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.PlaceholderModelHelper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class WirePortalModel extends NodeModel implements IHasDeclarationModel {
    @Getter @Setter(AccessLevel.PROTECTED)
    private int evaluationOrder;
    @Nullable
    private DeclarationModel declarationModel;
    private UUID modelUid;
    private TypeHandle typeHandle;

    protected WirePortalModel() {
        capabilities.add(Capabilities.RENAMABLE);
        capabilities.remove(Capabilities.COLLAPSIBLE);
        capabilities.remove(Capabilities.COLORABLE);
    }

    public DeclarationModel getDeclarationModel() {
        if (declarationModel == null) {
            var placeholder = PlaceholderModelHelper.getPlaceholderGraphElementModel(graphModel, modelUid);
            if (placeholder != null) {
                PlaceholderModelHelper.setPlaceholderCapabilities(this);
                return (DeclarationModel) placeholder;
            }
        }
        return declarationModel;
    }

    public void setDeclarationModel(DeclarationModel value) {
        if (this.declarationModel == value) return;
        this.declarationModel = value;
        this.modelUid = value.getUid();
        if (graphModel != null) {
            graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
        }
    }

    public TypeHandle getPortDataTypeHandle() {
        if (declarationModel == null && modelUid != null && graphModel.getModel(modelUid) instanceof PortalDeclarationPlaceholder) {
            return TypeHandles.MISSING_PORT;
        }

        // Type's identification of portals' ports are empty strings in the compatibility tests.
        if (typeHandle.getIdentification() == null)
            typeHandle = TypeHandleHelpers.customType("", null);
        return typeHandle;
    }

    public void setPortDataTypeHandle(TypeHandle typeHandle) {
        if (this.typeHandle == typeHandle) return;
        this.typeHandle = typeHandle;
        if (graphModel != null) {
            graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
        }
    }

    public PortType getPortType() {
        return PortType.DEFAULT;
    }

    @Override
    public String getName() {
        return declarationModel == null ? "" : declarationModel.getName();
    }

    /**
     * Indicates whether there can be one portal that has the same declaration and direction.
     */
    public boolean canHaveAnotherPortalWithSameDirectionAndDeclaration() {
        return true;
    }

    /**
     * Indicates whether we can create an opposite portal for this portal.
     */
    public boolean canCreateOppositePortal() {
        return true;
    }

    /**
     * Indicates whether we can revert this portal to a wire.
     */
    public boolean canRevertToWire() {
        // To be able to create a wire, the portal and the opposite portals need to be connected to another node.
        if (getConnectedWires().isEmpty())
            return false;

        if (this instanceof ISingleInputPortNodeModel) {
            for (var portal : graphModel.getExitPortals(getDeclarationModel())) {
                if (!portal.getConnectedWires().isEmpty()) return true;
            }

        } else {
            for (var portal : graphModel.getExitPortals(getDeclarationModel())) {
                if (!portal.getConnectedWires().isEmpty()) return true;
            }
        }

        return false;
    }

    @Override
    public @Nullable GraphElement<?> createElementUI() {
        return new CapsuleNodeElement(this);
    }
}
