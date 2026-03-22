package com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.IGraphElementContainer;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.group.GroupModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableDeclarationModelBase;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wiget.PlacematModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wiget.StickyNoteModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireModel;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ElementsByType {
    public final Set<StickyNoteModel> stickyNoteModels = new HashSet<>();
    public final Set<PlacematModel> placematModels = new HashSet<>();
    public final Set<VariableDeclarationModelBase> variableDeclarationsModels = new HashSet<>();
    public final Set<GroupModel> groupModels = new HashSet<>();
    public final Set<WireModel> wireModels = new HashSet<>();
    public final Set<AbstractNodeModel> nodeModels = new HashSet<>();

    public ElementsByType(Collection<? extends GraphElementModel> elements) {
        recursiveSortElements(elements);
    }

    void recursiveSortElements(Collection<? extends GraphElementModel> graphElementModels) {
        for (var element : graphElementModels) {
            if (element instanceof IGraphElementContainer container)
                recursiveSortElements(container.getGraphElementModels());
            switch (element) {
                case StickyNoteModel stickyNoteModel:
                    stickyNoteModels.add(stickyNoteModel);
                    break;
                case PlacematModel placematModel:
                    placematModels.add(placematModel);
                    break;
                case VariableDeclarationModelBase variableDeclarationModel:
                    variableDeclarationsModels.add(variableDeclarationModel);
                    break;
                case GroupModel groupModel:
                    groupModels.add(groupModel);
                    break;
                case WireModel wireModel:
                    wireModels.add(wireModel);
                    break;
                case AbstractNodeModel nodeModel:
                    nodeModels.add(nodeModel);
                    break;
                default:
                    break;
            }
        }
    }
}
