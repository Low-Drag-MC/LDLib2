package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.group.GroupModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.group.GroupModelBase;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.group.IGroupItemModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.*;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;


import java.util.List;
import java.util.UUID;

public final class VariableDeclarationCommands {
    /**
     * Command to create a variable.
     */
    public static class CreateGraphVariableDeclarationCommand extends UndoableGraphCommand {
        public static final Component NAME = Component.translatable("graph.commands.create_variable");

        /**
         * The name of the variable to create.
         */
        public String variableName;

        /**
         * The scope of the variable.
         */
        public VariableScope scope;

        /**
         * The type of variable to create.
         */
        public @Nullable Class<? extends VariableDeclarationModel> variableType;

        /**
         * The type of the variable to create.
         */
        public TypeHandle typeHandle;

        /**
         * The uid to assign to the newly created variable.
         */
        public @Nullable UUID uid;

        /**
         * The modifiers to apply to the newly created variable.
         */
        public ModifierFlags modifierFlags;

        /**
         * The group to insert the variable in.
         */
        public @Nullable GroupModel group;

        /**
         * The index in the group where the variable will be inserted.
         */
        public int indexInGroup;

        public CreateGraphVariableDeclarationCommand(String name,
                                                     VariableScope scope,
                                                     @Nullable Class<? extends VariableDeclarationModel> variableType,
                                                     TypeHandle typeHandle,
                                                     @Nullable GroupModel group,
                                                     int indexInGroup,
                                                     @Nullable ModifierFlags modifierFlags,
                                                     @Nullable UUID uid) {
            this.variableName = name;
            this.scope = scope;
            this.variableType = variableType;
            this.typeHandle = typeHandle;
            this.group = group;
            this.indexInGroup = indexInGroup;
            this.modifierFlags = modifierFlags == null ? ModifierFlags.NONE : modifierFlags;
            this.uid = uid;
        }

        @Override
        public void execute() {
            VariableDeclarationModelBase newVariableDeclaration;
            if (variableType != null) {
                newVariableDeclaration = graphModel.createGraphVariableDeclaration(variableType, typeHandle, variableName,
                        modifierFlags, scope, group, indexInGroup, null, uid, null, null);
            } else {
                newVariableDeclaration = graphModel.createGraphVariableDeclaration(typeHandle, variableName,
                        modifierFlags, scope, group, indexInGroup, null, uid, null);
            }

            if (newVariableDeclaration == null) return;

            view.blackboard.setLastVariableInfos(new VariableCreationInfos()
                    .setName(variableName)
                    .setGroup(group)
                    .setIndexInGroup(indexInGroup)
                    .setModifiers(newVariableDeclaration.getModifiers())
                    .setScope(scope)
                    .setTypeHandle(typeHandle)
                    .setVariableType(variableType)
            );

            graphModel.updateSubGraphs();

            var current = newVariableDeclaration.getParentGroup();

            while (current != null) {
                view.blackboard.setGroupModelExpanded(current, true);
                current = current.getParentGroup();
            }
        }

        @Override
        public Component getCommandName() {
            return NAME;
        }
    }

    /**
     * Moves a Blackboard item (variable or group) into a target group at a given index. Handles
     * both reorder-within-group and move-between-groups in one call — {@link GroupModel#insertItem}
     * removes from the previous parent automatically.
     */
    public static class MoveGroupItemCommand extends UndoableGraphCommand {
        public static final Component NAME = Component.translatable("graph.commands.move_group_item");
        private final IGroupItemModel item;
        private final GroupModelBase targetGroup;
        private final int index;

        public MoveGroupItemCommand(IGroupItemModel item, GroupModelBase targetGroup, int index) {
            this.item = item;
            this.targetGroup = targetGroup;
            this.index = index;
        }

        @Override
        public void execute() {
            if (targetGroup instanceof GroupModel g) {
                g.insertItem(item, index);
            }
        }

        @Override
        public Component getCommandName() {
            return NAME;
        }
    }

    public static class ChangeVariableModifiersCommand extends UndoableGraphCommand {
        public static final Component NAME = Component.translatable("graph.commands.change_variable_modifiers");

        public List<? extends VariableDeclarationModelBase> variableDeclarationModels;
        public ModifierFlags modifierFlags;

        public ChangeVariableModifiersCommand( List<? extends VariableDeclarationModelBase> variableDeclarationModels, ModifierFlags modifierFlags) {
            this.variableDeclarationModels = variableDeclarationModels;
            this.modifierFlags = modifierFlags;
        }

        @Override
        public void execute() {
            for (var variable : variableDeclarationModels) {
                variable.setModifiers(modifierFlags);
            }

            graphModel.updateSubGraphs();
        }

        @Override
        public Component getCommandName() {
            return NAME;
        }
    }
}
