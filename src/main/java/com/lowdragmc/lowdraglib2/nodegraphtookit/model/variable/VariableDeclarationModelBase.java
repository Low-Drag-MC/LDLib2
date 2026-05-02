package com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable;

import com.lowdragmc.lowdraglib2.gui.ui.data.Tooltips;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.IFieldConstantConfigurable;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.IVariable;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.VariableKind;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.DeclarationModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.group.GroupModelBase;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.group.IGroupItemModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.VariableNodeModel;
import com.mojang.serialization.DataResult;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.stream.Stream;


/**
 * Base class for variable declarations.
 */
public abstract class VariableDeclarationModelBase extends DeclarationModel implements IVariable, IGroupItemModel, IFieldConstantConfigurable {
    @Getter @Setter
    protected GroupModelBase parentGroup;

    public VariableDeclarationModelBase() {}

    public abstract VariableFlags getVariableFlags();
    public abstract void setVariableFlags(VariableFlags flags);

    public abstract ModifierFlags getModifiers();
    public abstract void setModifiers(ModifierFlags flags);

    public abstract VariableScope getScope();
    public abstract void setScope(VariableScope scope);

    public abstract boolean isShowOnInspectorOnly();
    public abstract void setShowOnInspectorOnly(boolean show);

    public abstract TypeHandle getDataTypeHandle();
    public abstract void setDataTypeHandle(TypeHandle dataType);

    public abstract Constant getInitializationModel();
    public abstract void setInitializationModel(Constant constant);

    public abstract Tooltips getTooltips();
    public abstract void setTooltips(Tooltips tooltips);

    /**
     * Sets the {@link #getInitializationModel()} to a new {@link Constant} instance.
     */
    public abstract void createInitializationValue();

    @Override
    public void onValueChanged() {
        if (this.graphModel != null) {
            this.graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
        }
    }

    @Override
    public Stream<GraphElementModel> getContainedModels() {
        return Stream.of(this);
    }

    @Override
    public IGroupItemModel getGroupItemInTargetGraph(GraphModel targetModel, Map<VariableDeclarationModelBase, VariableDeclarationModelBase> variableTranslation) {
        return variableTranslation.get(this);
    }

    @Override
    public <T> DataResult<T> tryGetDefaultValue(Type expectedType) {
        var model = getInitializationModel();
        if (model == null) {
            return DataResult.error(() -> "Cannot get default value of variable " + getName() + " as it has no initialization model.");
        }
        return model.tryGetValue(expectedType);
    }

    @Override
    public VariableKind getVariableKind() {
        return switch (getModifiers()) {
            case WRITE -> VariableKind.OUTPUT;
            case READ -> VariableKind.INPUT;
            default -> VariableKind.LOCAL;
        };
    }

    @Override
    public Type getDataType() {
        return getDataTypeHandle().resolve();
    }

    /**
     * Indicates whether it requires initialization.
     */
    public boolean requiresInitialization() {
        var dataType = getDataType();
        return dataType instanceof Class<?> clazz && clazz.isPrimitive() || dataType == String.class;
    }

    public String uniqueId() {
        return getUid().toString();
    }

    public String getSubName() {
        return "";
    }

    public boolean isOutput() {
        return getModifiers() == ModifierFlags.WRITE;
    }

    public boolean isInput() {
        return getModifiers() == ModifierFlags.READ;
    }

    public boolean isInputOrOutput() {
        return isInput() || isOutput();
    }

    /**
     * Returns if this variable is used in the graph, it won't be selected when select unused is dispatched.
     */
    public boolean isUsed() {
        for (var nodeModel : graphModel.getNodeModels()) {
            if (nodeModel instanceof VariableNodeModel node) {
                if (node.getVariableDeclarationModel() == this && node.getPorts().stream().anyMatch(PortModel::isConnected)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public @Nullable Constant getConfigurableConstant() {
        return getInitializationModel();
    }
}
