package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.NodeElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.*;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireModel;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import org.joml.Vector2f;

import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.stream.Stream;
public abstract class AbstractNodeModel extends GraphElementModel implements IHasName, IHasDisplayName, IMovable,
        IHasElementColor, IHasContextualMenuItems, IGraphElementUIModel {
    @Persisted
    private Vector2f position = new Vector2f(0);
    @Persisted @Getter
    protected String name = "";
    @Persisted @Nullable
    protected Component title;
    @Nullable
    protected Component tooltip;

    private SpawnFlags spawnFlags = SpawnFlags.DEFAULT;

    private NodePreviewModel nodePreviewModel;

    @Persisted
    private ModelState state;

    protected AbstractNodeModel() {
        capabilities.addAll(List.of(
                Capabilities.DELETABLE,
                Capabilities.DROPPABLE,
                Capabilities.COPIABLE,
                Capabilities.SELECTABLE,
                Capabilities.MOVABLE,
                Capabilities.COLLAPSIBLE,
                Capabilities.COLORABLE,
                Capabilities.ASCENDABLE,
                Capabilities.DISABLEABLE
        ));
    }

    /**
     * Determines whether the node model allows connections to itself.
     *
     * @return {@code true} if the node can connect to itself, {@code false} otherwise.
     */
    public abstract boolean isAllowSelfConnect();

    /**
     * Gets the icon of the node.
     */
    public abstract IGuiTexture getNodeIcon();

    public abstract int getElementColor();

    public abstract void setColor(int color);

    public abstract int getDefaultColor();

    public abstract boolean hasUserColor();

    /**
     * Gets the state of the node. Indicates whether the node is enabled or disabled.
     */
    public ModelState getState() {
        return state;
    }

    public void setState(ModelState value) {
        if (Objects.equals(state, value)) return;
        if (!isDisableable()) return;
        state = value;
        GraphModel gm = getGraphModel();
        if (gm != null) gm.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
    }

    public void setName(String value) {
        if (Objects.equals(name, value)) return;
        name = value;
        GraphModel gm = getGraphModel();
        if (gm != null) gm.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
    }

    public void setTitle(Component value) {
        if (Objects.equals(title, value)) return;
        title = value;
        GraphModel gm = getGraphModel();
        if (gm != null) gm.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.STYLE);
    }

    public Component getTitle() {
        return title == null ? Component.translatable(getName()) : title;
    }

    public Component getTooltip() {
        return tooltip == null ? getTitle() : tooltip;
    }

    public void setTooltip(Component value) {
        if (Objects.equals(tooltip, value)) return;
        tooltip = value;
        GraphModel gm = getGraphModel();
        if (gm != null) gm.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.STYLE);
    }

    public Vector2f getPosition() {
        return position;
    }

    public void setPosition(Vector2f value) {
        if (!isMovable()) return;
        if (Objects.equals(position, value)) return;
        position = value;
        GraphModel gm = getGraphModel();
        if (gm != null) gm.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.LAYOUT);
    }

    public SpawnFlags getSpawnFlags() {
        return spawnFlags;
    }

    public void setSpawnFlags(SpawnFlags value) {
        spawnFlags = value;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    private boolean destroyed;

    public abstract boolean hasNodePreview();

    public NodePreviewModel getNodePreviewModel() {
        return hasNodePreview() ? nodePreviewModel : null;
    }

    @Override
    public Stream<GraphElementModel> getDependentModels() {
        return Stream.concat(super.getDependentModels(), nodePreviewModel == null ? Stream.empty() : Stream.of(nodePreviewModel));
    }

    public void onDeleteNode() {
        destroyed = true;
        if (nodePreviewModel != null) {
            getGraphModel().getCurrentGraphChangeDescription().addDeletedModel(nodePreviewModel);
        }
    }

    public abstract List<WireModel> getConnectedWires();

    public void onCreateNode() {
        if (hasNodePreview() && spawnFlags != SpawnFlags.ORPHAN) {
            addNodePreview();
        }
    }

    public void onDuplicateNode(AbstractNodeModel sourceNode) {
        if (sourceNode == null) return;

        setName(sourceNode.getName());
        if (sourceNode.hasNodePreview() && sourceNode.getNodePreviewModel() != null) {
            NodePreviewModel preview = addNodePreview();
            preview.onDuplicateNodePreview(sourceNode.getNodePreviewModel());
        }
    }

    @Override
    public void move(Vector2f delta) {
        if (!isMovable()) return;
        setPosition(getPosition().add(delta, new Vector2f()));
    }

    protected NodePreviewModel createNodePreview() {
        return new NodePreviewModel();
    }

    void syncNodePreview() {
        if (hasNodePreview() && nodePreviewModel == null) {
            addNodePreview();
        } else if (!hasNodePreview() && nodePreviewModel != null) {
            nodePreviewModel = null;
        }
    }

    protected NodePreviewModel addNodePreview() {
        NodePreviewModel m = createNodePreview();
        nodePreviewModel = m;
        m.onCreateNodePreview(this);

        GraphModel gm = getGraphModel();
        gm.registerNodePreview(nodePreviewModel);
        gm.getCurrentGraphChangeDescription().addNewModel(nodePreviewModel);
        return m;
    }

    @Override
    public List<ContextualMenuItem> getContextualMenuItems() {
        List<ContextualMenuItem> items = new ArrayList<>(GraphElementModel.COMMON_GRAPH_ELEMENT_MENU_ITEMS);
        items.addAll(CONTEXTUAL_MENU_ITEMS);
        return items;
    }

    @Override
    public @Nullable GraphElement<?> createElementUI() {
        return new NodeElement(this);
    }

    private static final List<ContextualMenuItem> CONTEXTUAL_MENU_ITEMS = List.of(
            ContextualMenuHelpers.deleteAndReconnectItem,
            new ContextualMenuItem(ContextualMenuHelpers.editSubtitleItem, 0),
            new ContextualMenuItem(ContextualMenuHelpers.bypassNodeItem, 1),
            new ContextualMenuItem(ContextualMenuHelpers.disableNodeItem, 2),
            new ContextualMenuItem(ContextualMenuHelpers.disconnectAllWiresItem, 3),
            new ContextualMenuItem(ContextualMenuHelpers.toggleCollapseItem, 5)
    );
}
