package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.IResizeWidth;
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

/**
 * Base class for a model that represents a node in a graph.
 */
public abstract class AbstractNodeModel extends GraphElementModel implements IHasName, IHasDisplayName, IMovable,
        IHasElementColor, IHasContextualMenuItems, IGraphElementUIModel, IResizeWidth {
    @Persisted
    private Vector2f position = new Vector2f(0);
    @Persisted @Getter
    protected String name = "";
    @Persisted @Nullable
    protected Component title;
    @Nullable
    protected Component tooltip;
    /** User-chosen color; meaningful only when {@link #userColor} is true. */
    @Persisted
    protected int elementColor = 0xFFFFFFFF;
    /** Whether the user has explicitly chosen a color via setColor(). */
    @Persisted
    protected boolean userColor = false;

    private SpawnFlags spawnFlags = SpawnFlags.DEFAULT;

    private NodePreviewModel nodePreviewModel;
    /**
     * Persisted source of truth for whether the preview panel is expanded. Mirrored onto the runtime
     * {@link NodePreviewModel} (which has no independent serialization) whenever it is (re)created.
     */
    @Persisted
    private boolean previewExpanded = true;

    @Persisted
    private ModelState state;

    /**
     * Floor for the auto-computed width. {@code 0} (default) means no floor — width is whatever
     * children compute. Edited via the inspector when {@link Capabilities#RESIZABLE} is on.
     */
    @Persisted
    protected float minWidth = 0f;

    /**
     * Whether the node UI is collapsed to title-only. Element subclasses decide which parts to
     * hide; wires connected to hidden ports re-route to the title bar (see {@link com.lowdragmc.lowdraglib2.nodegraphtookit.gui.WireElement}).
     */
    @Persisted @Getter
    protected boolean collapsed = false;

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
                Capabilities.DISABLEABLE,
                Capabilities.RESIZABLE
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

    @Override
    public int getElementColor() {
        return userColor ? elementColor : getDefaultColor();
    }

    @Override
    public void setColor(int color) {
        if (userColor && elementColor == color) return;
        elementColor = color;
        userColor = true;
        GraphModel gm = getGraphModel();
        if (gm != null) gm.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.STYLE);
    }

    @Override
    public int getDefaultColor() {
        return 0;
    }

    @Override
    public boolean hasUserColor() {
        return userColor;
    }

    @Override
    public void resetColor() {
        if (!userColor) return;
        userColor = false;
        elementColor = getDefaultColor();
        GraphModel gm = getGraphModel();
        if (gm != null) gm.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.STYLE);
    }

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

    @Override
    public float getMinWidth() {
        return Math.max(minWidth, getNodeWidth());
    }

    /**
     * Gets the intrinsic minimum width required by this node type. User-edited {@link #minWidth}
     * cannot go below this value.
     */
    public float getNodeWidth() {
        return 0f;
    }

    @Override
    public void setMinWidth(float value) {
        if (!isResizable()) return;
        value = Math.max(value, Math.max(0, getNodeWidth()));
        if (minWidth == value) return;
        minWidth = value;
        GraphModel gm = getGraphModel();
        if (gm != null) gm.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.LAYOUT);
    }

    public void setCollapsed(boolean value) {
        if (!isCollapsible()) return;
        if (collapsed == value) return;
        collapsed = value;
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

    public boolean isNodePreviewExpandedByDefault() {
        return true;
    }

    public NodePreviewModel getNodePreviewModel() {
        return hasNodePreview() ? nodePreviewModel : null;
    }

    /** Whether the preview panel is expanded (persisted; meaningful only when {@link #hasNodePreview()}). */
    public boolean isPreviewExpanded() {
        return previewExpanded;
    }

    /**
     * Sets the preview panel's expanded state, mirrors it onto the live preview model, and fires a
     * {@link ChangeHint#DATA} change so the preview UI toggles.
     */
    public void setPreviewExpanded(boolean expanded) {
        if (previewExpanded == expanded) return;
        previewExpanded = expanded;
        if (nodePreviewModel != null) {
            nodePreviewModel.setExpanded(expanded);
            GraphModel gm = getGraphModel();
            if (gm != null) {
                gm.getCurrentGraphChangeDescription().addChangedModel(nodePreviewModel, ChangeHint.DATA);
            }
        }
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
            previewExpanded = isNodePreviewExpandedByDefault();
            addNodePreview();
        }
    }

    public void onDuplicateNode(AbstractNodeModel sourceNode) {
        if (sourceNode == null) return;

        setName(sourceNode.getName());
        if (sourceNode.hasNodePreview() && sourceNode.getNodePreviewModel() != null) {
            this.previewExpanded = sourceNode.previewExpanded;
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

    /**
     * Reconciles the preview model with {@link #hasNodePreview()}: creates the preview model when the
     * node should have one but doesn't (e.g. after deserialize, where the lifecycle hooks that create
     * it on fresh spawn don't run), or drops it when it shouldn't. Safe to call repeatedly.
     */
    public void syncNodePreview() {
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
        // Mirror the persisted expanded state onto the freshly-created (unserialized) preview model.
        m.setExpanded(previewExpanded);

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
