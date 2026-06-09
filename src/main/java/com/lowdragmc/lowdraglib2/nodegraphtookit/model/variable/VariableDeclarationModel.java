package com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable;

import com.lowdragmc.lowdraglib2.gui.ui.data.Tooltips;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.TypeConstant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.VariableNodeModel;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

import java.awt.*;
import java.util.Objects;

public class VariableDeclarationModel extends VariableDeclarationModelBase {
    @Getter
    protected TypeHandle dataTypeHandle;
    @Getter
    protected Constant initializationModel;
    @Persisted @Getter
    protected VariableScope scope = VariableScope.UNKNOWN;
    @Persisted @Getter
    protected boolean isShowOnInspectorOnly = false;
    protected Tooltips tooltips = Tooltips.empty();
    @Persisted @Getter
    protected ModifierFlags modifiers = ModifierFlags.NONE;
    @Persisted @Getter
    protected VariableFlags variableFlags = VariableFlags.NONE;

    public void createInitializationValue() {
        if (graphModel != null && graphModel.getConstantType(getDataTypeHandle()) != null){
            setInitializationModel(graphModel.createConstantValue(getDataTypeHandle()));
            graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
        }
    }

    @Override
    public void setVariableFlags(VariableFlags flags) {
        if (variableFlags == flags) return;
        variableFlags = flags;
        if (graphModel != null) {
            graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
        }
    }

    @Override
    public void setModifiers(ModifierFlags flags) {
        if (graphModel != null) {
            flags = graphModel.sanitizeSubgraphVariableModifiers(flags);
        }
        if (modifiers == flags) return;
        modifiers = flags;
        if (graphModel != null) {
            for (var variableNodeModel : graphModel.findReferencesInGraph(VariableNodeModel.class, this)) {
                if (variableNodeModel.getDeclarationModel() == this) {
                    variableNodeModel.defineNode();
                }
            }
            graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
            // outer subgraph nodes that reference this graph must mirror the modifier change as port direction
            graphModel.redefineSubgraphNodeModels();
        }
    }

    @Override
    public void setScope(VariableScope scope) {
        if (this.scope == scope) return;
        this.scope = scope;
        if (graphModel != null) {
            graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
        }
    }

    @Override
    public void setShowOnInspectorOnly(boolean show) {
        if (isShowOnInspectorOnly == show) return;
        isShowOnInspectorOnly = show;
        if (graphModel != null) {
            graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
        }
    }

    @Override
    public void setDataTypeHandle(TypeHandle dataType) {
        if (Objects.equals(dataTypeHandle, dataType)) return;
        dataTypeHandle = dataType;
        initializationModel = null;
        if (graphModel != null) {
            if (graphModel.variableDeclarationRequiresInitialization(this)) {
                createInitializationValue();
            }
            graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
            var variableRefs = graphModel.findReferencesInGraph(VariableNodeModel.class, this);
            for (var usage : variableRefs) {
                usage.updateTypeFromDeclaration();
            }
            // type change on an exposed variable changes the outer subgraph node's port type
            if (modifiers != null && modifiers != ModifierFlags.NONE) {
                graphModel.redefineSubgraphNodeModels();
            }
        }
    }

    @Override
    public void setName(String name) {
        if (getName().equals(name)) return;
        super.setName(name);
        // port title on the outer subgraph node mirrors the variable name
        if (graphModel != null && modifiers != null && modifiers != ModifierFlags.NONE) {
            graphModel.redefineSubgraphNodeModels();
        }
    }

    @Override
    public void setInitializationModel(Constant constant) {
        if (initializationModel == constant) return;
        // Unregister ourselves as the owner of the old constant.
        if (initializationModel != null)
            initializationModel.setOwner(null);
        initializationModel = constant;
        if (initializationModel != null)
            initializationModel.setOwner(this);
        if (graphModel != null) {
            graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
        }
    }


    @Override
    public Tag serializeAdditionalNBT(HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        if (dataTypeHandle != null) {
            tag.putString("dataTypeId", dataTypeHandle.getIdentification());
        }
        if (initializationModel != null) {
            tag.put("initializationModel", TypeConstant.serializeConstant(initializationModel, provider));
        }
        return tag;
    }

    @Override
    public void deserializeAdditionalNBT(Tag tag, HolderLookup.Provider provider) {
        if (tag instanceof CompoundTag compound) {
            if (compound.contains("dataTypeId")) {
                dataTypeHandle = TypeHandle.create(compound.getString("dataTypeId"));
            }
            if (compound.contains("initializationModel")) {
                initializationModel = TypeConstant.deserializeConstant(compound.getCompound("initializationModel"), provider);
                if (initializationModel != null) {
                    initializationModel.setOwner(this);
                }
            }
        }
    }

    @Override
    public Tooltips getTooltips() {
        if (tooltips != null) return tooltips;
        var typeName = getDataTypeHandle().getFriendlyName();
        var name = Component.literal("Variable");
        if (!typeName.isEmpty()) name = name.append(Component.literal(" of type ").append(typeName));
        return Tooltips.of(name);
    }

    @Override
    public void setTooltips(Tooltips tooltips) {
        if (this.tooltips.equals(tooltips)) return;
        this.tooltips = tooltips;
        if (graphModel != null) {
            graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.STYLE);
            for (var ref : graphModel.findReferencesInGraph(VariableNodeModel.class, this)) {
                graphModel.getCurrentGraphChangeDescription().addChangedModel(ref, ChangeHint.STYLE);
            }
        }
    }

}
