package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.ModelElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.BlockNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ContextNodeModel;
import dev.vfyjxf.taffy.style.FlexDirection;

import java.util.ArrayList;
import java.util.List;

/**
 * Vertical container that renders a {@link ContextNodeModel}'s ordered block list. Rebuilds its
 * child {@link BlockNodeElement}s when the parent context emits a topology change (block added,
 * removed, or reordered).
 *
 * <p>This element <em>owns</em> the lifecycle of every {@link BlockNodeElement} it creates:
 * on rebuild it calls {@code setGraphView(null)} on the old elements before clearing them so
 * they unregister cleanly from the graph view's model→element map. Top-level
 * {@code GraphView.createAndAddModelElement} never sees blocks (see
 * {@link BlockNodeModel#createElementUI()} which returns null).</p>
 */
public class BlockListContainerElement extends ModelElement {
    public final ContextNodeModel contextNodeModel;
    /** Block UI elements currently in the tree, paired with their backing models. */
    private final List<BlockNodeElement> blockElements = new ArrayList<>();

    public BlockListContainerElement(ContextNodeModel contextNodeModel) {
        this.contextNodeModel = contextNodeModel;
    }

    @Override
    protected void buildUI() {
        setId("block-list-container");
        getLayout().flexDirection(FlexDirection.COLUMN).gapAll(2).paddingAll(2);
        // Initial population — buildUI runs once the graphView is set.
        rebuildBlocks();
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        if (visitor.hasHint(ChangeHint.GRAPH_TOPOLOGY) || shouldRebuild()) {
            rebuildBlocks();
        }
    }

    private boolean shouldRebuild() {
        var blocks = contextNodeModel.getBlocks();
        if (blocks.size() != blockElements.size()) return true;
        for (int i = 0; i < blocks.size(); i++) {
            if (blockElements.get(i).getModel() != blocks.get(i)) return true;
        }
        return false;
    }

    private void rebuildBlocks() {
        // Tear down the previous BlockNodeElements properly: setGraphView(null) unregisters
        // them from the graph view's modelElements map (otherwise selection/hit-testing would
        // still see stale elements). clearAllChildren only detaches them from the UI tree.
        for (var old : blockElements) {
            old.setGraphView(null);
        }
        blockElements.clear();
        clearAllChildren();

        // When unmounted (graphView == null, e.g. ContextNodeElement was just removed),
        // skip rebuilding — there's no live tree to attach to and no graphView to register
        // with. Mount-time setGraphView will call buildUI again with a real graphView.
        var graphView = getGraphView();
        if (graphView == null) return;

        for (var blockModel : contextNodeModel.getBlocks()) {
            var blockElement = new BlockNodeElement(blockModel);
            blockElement.setGraphView(graphView);
            addChild(blockElement);
            blockElements.add(blockElement);
            blockElement.doCompleteUpdate();
        }
    }
}
