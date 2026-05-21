package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.ModelElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.BlockCommands;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary.BlockLibraryItem;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.BlockNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ContextNodeModel;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Vertical container that renders a {@link ContextNodeModel}'s ordered block list. Rebuilds its
 * child {@link BlockNodeElement}s when the parent context emits a topology change (block added,
 * removed, or reordered).
 *
 * <p>Existing {@link BlockNodeElement} instances are reused across rebuilds whenever their
 * backing model is still present — including reorders. Only elements whose model has been
 * removed are torn down (setGraphView(null) so they unregister from the graph view); only new
 * models get fresh elements.</p>
 *
 * <p>Top-level {@code GraphView.createAndAddModelElement} never sees blocks (see
 * {@link BlockNodeModel#createElementUI()} which returns null), so this container is the sole
 * owner of every block element's lifecycle.</p>
 */
public class BlockListContainerElement extends ModelElement {
    public final ContextNodeModel contextNodeModel;
    /** Block UI elements currently in the tree, ordered to match {@code contextNodeModel.getBlocks()}. */
    protected final List<BlockNodeElement> blockElements = new ArrayList<>();
    protected UIElement blockContainer;
    protected Button addBlockButton;

    public BlockListContainerElement(ContextNodeModel contextNodeModel) {
        this.contextNodeModel = contextNodeModel;
        addClass("__block-list-container__");
    }

    @Override
    protected void buildUI() {
        Style.defaultPipeline(getLayout(), l -> l.gapAll(1).paddingAll(2).marginAll(4));
        Style.defaultPipeline(getStyle(), s -> s.background(Sprites.RECT_DARK));
        // Initial population — buildUI runs after the graphView is set.
        blockContainer = new UIElement().addClass("__block-list-container_blocks__");
        Style.defaultPipeline(blockContainer.getLayout(), l -> l.gapAll(1));
        addBlockButton = new Button().setText("graph.add_block");
        addBlockButton.addClass("__block-list-container_add-button__");
        Style.defaultPipeline(addBlockButton.getLayout(), l -> l.marginAll(5));
        // Opens the ItemLibrary scoped to compatible blocks; selection dispatches an
        // InsertBlockCommand against this container's parent context.
        addBlockButton.setOnClick(e -> {
            var graphView = getGraphView();
            if (graphView == null) return;
            graphView.itemLibrary.showBlocksForContext(e.x, e.y, contextNodeModel, item -> {
                if (item instanceof BlockLibraryItem blockItem) {
                    graphView.dispatchCommand(new BlockCommands.InsertBlockCommand(
                            contextNodeModel, blockItem.getBlockClass(), -1));
                }
            });
        });
        addChildren(blockContainer, addBlockButton);
        applyAddButtonVisibility();
        rebuildBlocks();
    }

    /** Hide the Add Block button entirely when the context accepts no block types. Data-driven. */
    private void applyAddButtonVisibility() {
        if (addBlockButton == null) return;
        var hasSupport = !contextNodeModel.getSupportBlockClasses().isEmpty();
        Style.importantPipeline(addBlockButton.getLayout(), l -> l.display(hasSupport ? TaffyDisplay.FLEX : TaffyDisplay.NONE));
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
        var graphView = getGraphView();
        var newBlocks = contextNodeModel.getBlocks();

        // Index existing elements by model so we can recognise reuses, including pure reorders.
        var existingByModel = new HashMap<BlockNodeModel, BlockNodeElement>(blockElements.size());
        for (var el : blockElements) existingByModel.put((BlockNodeModel) el.getModel(), el);

        // Tear down only blocks no longer present. setGraphView(null) unregisters them from the
        // graph view's modelElements map — without this, selection/hit-testing would still find
        // stale references after a block is removed.
        for (var el : blockElements) {
            if (!newBlocks.contains(el.getModel())) {
                el.setGraphView(null);
            }
        }

        // Detach all children from the UI tree. Children we still want will be re-attached below;
        // setGraphView is sticky, so reused elements keep their graphView registration.
        blockContainer.clearAllChildren();
        blockElements.clear();

        // When unmounted (e.g. ContextNodeElement was just removed), skip — buildUI will rebuild
        // when the element is remounted to a graphView.
        if (graphView == null) return;

        for (var blockModel : newBlocks) {
            var blockElement = existingByModel.get(blockModel);
            if (blockElement == null) {
                blockElement = new BlockNodeElement(blockModel);
                blockElement.setGraphView(graphView);
                // Wire MOUSE_DOWN selection (otherwise blocks are unselectable — they don't go
                // through GraphView.addElement). Done once at create time; reused elements
                // already have it.
                graphView.wireSelectableElement(blockElement);
                blockElement.doCompleteUpdate();
            }
            blockContainer.addChild(blockElement);
            blockElements.add(blockElement);
        }
    }
}
