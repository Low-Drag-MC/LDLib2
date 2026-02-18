package com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable;

import com.lowdragmc.lowdraglib2.gui.ui.data.Tooltips;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.IPlaceHolder;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.PlaceholderModelHelper;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import lombok.Getter;
import lombok.Setter;

/**
 * A model that represents the placeholder of a variable declaration.
 */
public class VariableDeclarationPlaceholder extends VariableDeclarationModelBase implements IPlaceHolder {
    @Getter @Setter
    private VariableFlags variableFlags = VariableFlags.NONE;
    @Getter @Setter
    private ModifierFlags modifiers = ModifierFlags.NONE;
    @Getter @Setter
    private VariableScope scope = VariableScope.UNKNOWN;
    @Getter @Setter
    private boolean isShowOnInspectorOnly = false;
    @Getter @Setter
    private TypeHandle dataTypeHandle = TypeHandles.UNKNOWN;
    @Getter @Setter
    private Constant initializationModel;
    @Getter @Setter
    private Tooltips tooltips = Tooltips.empty();

    public VariableDeclarationPlaceholder() {
        PlaceholderModelHelper.setPlaceholderCapabilities(this);
    }

    @Override
    public void createInitializationValue() {

    }
}
