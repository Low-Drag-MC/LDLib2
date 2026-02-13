package com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.IGraphElementContainer;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireModel;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ElementsByType {
//    public HashSet<StickyNoteModel> StickyNoteModels;
//    public HashSet<PlacematModel> PlacematModels;
//    public HashSet<VariableDeclarationModelBase> VariableDeclarationsModels;
//    public HashSet<GroupModel> GroupModels;
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
//                case StickyNoteModel stickyNoteModel:
//                    StickyNoteModels.Add(stickyNoteModel);
//                    break;
//                case PlacematModel placematModel:
//                    PlacematModels.Add(placematModel);
//                    break;
//                case VariableDeclarationModelBase variableDeclarationModel:
//                    VariableDeclarationsModels.Add(variableDeclarationModel);
//                    break;
//                case GroupModel groupModel:
//                    GroupModels.Add(groupModel);
//                    break;
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
