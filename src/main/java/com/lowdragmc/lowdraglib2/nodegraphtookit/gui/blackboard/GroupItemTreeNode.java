package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.blackboard;

import com.lowdragmc.lowdraglib2.gui.util.ITreeNode;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.group.GroupModelBase;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.group.IGroupItemModel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

@EqualsAndHashCode
public class GroupItemTreeNode implements ITreeNode<IGroupItemModel, Void> {
    @Getter @Nullable
    private final GroupItemTreeNode parent;
    @Getter
    private final IGroupItemModel key;
    @Getter
    private int dimension;

    public GroupItemTreeNode(@Nullable GroupItemTreeNode parent, IGroupItemModel key) {
        this.parent = parent;
        this.key = key;
        this.dimension = parent == null ? 0 : parent.getDimension() + 1;
    }

    @Override
    public @Nullable Void getContent() {
        return null;
    }

    @Override
    public @NotNull List<GroupItemTreeNode> getChildren() {
        if (getKey() instanceof GroupModelBase groupModel) {
            return groupModel.getItems().stream().map(item -> new GroupItemTreeNode(this, item)).toList();
        }
        return Collections.emptyList();
    }
}
