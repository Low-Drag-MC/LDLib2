package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.nodegraphtookit.editor.GraphEditorView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.SubgraphNodeModel;

public class SubgraphNodeElement extends CollapsibleInOutNodeElement {

    public SubgraphNodeElement(SubgraphNodeModel nodeModel) {
        super(nodeModel);
        addClass("__subgraph-node__");

        // Double-click on a subgraph node enters its inner graph. View-level navigation —
        // intentionally not routed through the command/history system so per-level history is kept
        // independent and clean.
        addEventListener(UIEvents.DOUBLE_CLICK, event -> {
            var editorView = getFirstAncestorOfType(GraphEditorView.class);
            if (editorView != null) {
                editorView.enterSubgraph(getModel());
                event.stopPropagation();
            }
        });
    }

    public SubgraphNodeModel getModel() {
        return (SubgraphNodeModel) super.getModel();
    }

    @Override
    protected void buildUI() {
        super.buildUI();
        if (nodeTittle != null) {
            Style.defaultPipeline(nodeTittle.getStyle(), s -> s.background(Sprites.TAB_WHITE));
        }
    }
}
