package com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.group.GroupModel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter @Setter @Accessors(chain = true)
public class VariableCreationInfos {
    /**
     * The type of the variable declaration model.
     */
    private Class<? extends VariableDeclarationModel> variableType;
    /**
     * The type of the variable.
     */
    private TypeHandle typeHandle = TypeHandles.FLOAT;
    /**
     * The scope of the variable.
     */
    private VariableScope scope = VariableScope.LOCAL;
    /**
     * The modifiers of the variable.
     */
    private ModifierFlags modifiers = ModifierFlags.NONE;
    /**
     * The name of the variable.
     */
    private String name = "";
    /**
     * The group to insert the variable in.
     */
    private GroupModel group;
    /**
     * The index in the group where the variable will be inserted.
     */
    private int indexInGroup;
}
