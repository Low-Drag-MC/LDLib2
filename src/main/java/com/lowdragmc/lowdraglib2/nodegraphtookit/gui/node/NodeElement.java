package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElementRendererRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.elements.GraphViewLod;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.DelegatingUIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphInspector;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.util.NodeColors;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.util.RenameColorConfigurableHelper;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.Model;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.*;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class NodeElement extends GraphElement<AbstractNodeModel> {
    public final static String NODE_LAYER = "Node";

    @Configurable(name = "NodeStyle")
    public class NodeStyle extends Style {
        private static final Property<?>[] PROPERTIES = new Property[] {
                PropertyRegistry.FOCUS_OVERLAY,
        };

        protected NodeStyle() {
            super(NodeElement.this);
            setDefault(PropertyRegistry.FOCUS_OVERLAY, ColorPattern.BLUE.borderTexture(1));
        }

        @Override
        protected Property<?>[] getProperties() {
            return PROPERTIES;
        }

        public IGuiTexture focusOverlay() {
            return getValueSave(PropertyRegistry.FOCUS_OVERLAY);
        }

        public NodeStyle focusOverlay(IGuiTexture texture) {
            set(PropertyRegistry.FOCUS_OVERLAY, texture);
            return this;
        }
    }

    @Getter
    protected @Nullable NodeTitleElement nodeTittle;
    @Getter
    protected @Nullable NodeOptionsInspector nodeOptionContainer;
    @Getter
    protected @Nullable PortContainerElement portContainerElement;
    /** Preview panel rendered at the bottom of the node; present only when the model has a preview. */
    @Getter
    protected @Nullable GraphElement<?> nodePreviewElement;

    private static final int HIGHLIGHT_TINT = 0xddffaf00;
    private static final int HOVER_TINT = 0xaaffffff;

    @Getter
    private final NodeStyle nodeStyle = new NodeStyle();
    /** Source the cached tints were built from; see {@link #tintedOverlay}. */
    @Nullable
    private IGuiTexture tintedOverlaySource;
    private final IGuiTexture[] tintedOverlays = new IGuiTexture[2];

    public NodeElement(AbstractNodeModel nodeModel) {
        super(nodeModel);
        addClass("__node-element__");
    }

    @Override
    public String getLayerName() {
        return NODE_LAYER;
    }

    // region build ui

    @Override
    protected void buildPartList() {
        parts.add(this.nodeTittle = new NodeTitleElement(getModel()));
        if (getModel() instanceof NodeModel nodeModel) {
            parts.add(this.nodeOptionContainer = new NodeOptionsInspector(nodeModel));
        }
        if (getModel() instanceof PortNodeModel portNodeNode) {
            parts.add(this.portContainerElement = new PortContainerElement(portNodeNode, PortContainerElement.HORIZONTAL_PORT_FILTER));
        }
        buildPreviewPart();
    }

    /**
     * Creates the preview panel part from the model's {@code NodePreviewModel} (the model decides the
     * concrete element via {@link com.lowdragmc.lowdraglib2.nodegraphtookit.model.IGraphElementUIModel#createElementUI()}).
     * Called last in {@link #buildPartList()} so subclasses can rely on the other parts existing.
     */
    protected void buildPreviewPart() {
        var previewModel = getModel().getNodePreviewModel();
        if (previewModel != null) {
            var element = previewModel.createElementUI();
            if (element != null) {
                parts.add(this.nodePreviewElement = element);
            }
        }
    }

    @Override
    protected void buildUI() {
        // Node uses ABSOLUTE positioning so position can be driven by model coordinates — pin via IMPORTANT.
        Style.importantPipeline(getLayout(), l -> l.positionType(TaffyPosition.ABSOLUTE));
        Style.defaultPipeline(getStyle(), s -> s.background(Sprites.RECT_SOLID));

        // Preview panel always sits at the very bottom of the node.
        addChildren(nodeTittle, nodeOptionContainer, portContainerElement, nodePreviewElement);
    }

    // endregion

    @Override
    public boolean hasModelDependenciesChanged() {
        return (getModel() instanceof InputOutputPortsNodeModel ioNode && !ioNode.getNodeOptions().isEmpty())
                || getModel().getNodePreviewModel() != null;
    }

    @Override
    public void addModelDependencies() {
        super.addModelDependencies();
        if (getModel() instanceof InputOutputPortsNodeModel ioNode) {
            for (var nodeOption : ioNode.getNodeOptions()) {
                getDependencies().addModelDependency(nodeOption.getPortModel());
            }
        }
        // Depend on the preview model so preview expand/collapse and data changes refresh the node.
        var previewModel = getModel().getNodePreviewModel();
        if (previewModel != null) {
            getDependencies().addModelDependency(previewModel);
        }
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        var model = getModel();
        // update layout — node position is model data, so write at IMPORTANT.
        if (visitor.hasHint(ChangeHint.LAYOUT)) {
            Style.importantPipeline(getLayout(), l -> l.left(model.getPosition().x).top(model.getPosition().y));
            // Per-instance min-width floor — only applied when the model opts into resizing.
            if (model.isResizable()) {
                Style.importantPipeline(getLayout(), l -> l.minWidth(model.getMinWidth()));
            }
        }
    }

    /**
     * Checks if the underlying graph element model should be highlighted.
     * Highlight is the feedback when multiple instances stand out. e.g. variable declarations.
     * @return true if the element should be highlighted
     */
    public boolean shouldBeHighlighted() {
        if (isSelected() || graphView == null) return false;
        if (getModel() instanceof IHasDeclarationModel declarationModel && declarationModel.getDeclarationModel() != null) {
            var dm = declarationModel.getDeclarationModel();
            for (Model model : graphView.getSelected()) {
                if (model instanceof IHasDeclarationModel dm2 && Objects.equals(dm, dm2.getDeclarationModel())) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean showHoverHighlight() {
        return isSelfOrChildHover() || isUnderRegionSelection();
    }

    @Override
    protected void onSelectionInspect(GraphInspector inspector) {
        super.onSelectionInspect(inspector);
        if (graphView != null) inspector.setHistoryStack(graphView.getHistoryStack());
        inspector.inspect(RenameColorConfigurableHelper.build(getModel(), graphView));
    }

    // region LOD

    /**
     * Body colour of the flat stand-in rect drawn at {@link GraphViewLod#SIMPLIFIED}. Defaults to
     * the node background shared by the {@code mc}/{@code modern}/{@code ore} stylesheets, so the
     * silhouette keeps reading as a node rather than as a coloured blob.
     */
    protected int getLodBodyColor() {
        return 0xFF_383838;
    }

    /**
     * Accent colour — the title bar at {@link GraphViewLod#SIMPLIFIED} and the whole node at
     * {@link GraphViewLod#BLOCK}. This is what makes node <em>types</em> distinguishable when
     * zoomed out, so it uses the same resolution the minimap does.
     */
    protected int getLodAccentColor() {
        return NodeColors.resolve(getModel());
    }

    /**
     * At reduced LOD the node's SDF background — its own rounded-rect pipeline and draw state, per
     * node, per frame — is replaced by a single flat rect that batches with every other one.
     */
    @Override
    protected void drawBackgroundTexture(@NotNull IGUIContext context) {
        var lod = lod();
        if (lod == GraphViewLod.FULL || !(context instanceof GUIContext guiContext)) {
            super.drawBackgroundTexture(context);
            return;
        }
        DrawerHelperClient.drawSolidRect(guiContext,
                getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(),
                lod == GraphViewLod.BLOCK ? getLodAccentColor() : getLodBodyColor());
    }

    /**
     * Where the subtree traversal is actually cut. A node is 15-60 elements deep; below
     * {@link GraphViewLod#FULL} none of them are legible, so none of them are visited.
     *
     * <p>Skipping when culled is a correctness fix as much as a performance one:
     * {@link com.lowdragmc.lowdraglib2.gui.ui.UIElement#drawInBackgroundInternal} already skips a
     * culled element's background and overlay but still recurses its children, so an off-screen node
     * used to draw its port icons with no body behind them.
     */
    @Override
    protected boolean shouldDrawChildren() {
        return !isCulled() && lod() == GraphViewLod.FULL;
    }

    /** The silhouette that replaces the subtree: a title colour bar over the body rect. */
    @Override
    protected void drawBackgroundAdditional(@NotNull IGUIContext context) {
        super.drawBackgroundAdditional(context);
        if (lod() == GraphViewLod.SIMPLIFIED && nodeTittle != null && context instanceof GUIContext guiContext) {
            DrawerHelperClient.drawSolidRect(guiContext,
                    nodeTittle.getPositionX(), nodeTittle.getPositionY(),
                    nodeTittle.getSizeWidth(), nodeTittle.getSizeHeight(), getLodAccentColor());
        }
    }

    // endregion

    public void drawBackgroundOverlay(@NotNull GUIContext context) {
        UIElementRendererRegistry.defaultRenderer().drawBackgroundOverlay(this, context);
        var overlay = getNodeStyle().focusOverlay();
        IGuiTexture drawn = null;
        if (isSelected()) {
            drawn = overlay;
        } else if (shouldBeHighlighted()) {
            drawn = tintedOverlay(overlay, HIGHLIGHT_TINT, 0);
        } else if (showHoverHighlight()) {
            drawn = tintedOverlay(overlay, HOVER_TINT, 1);
        }
        if (drawn != null) {
            context.drawTexture(drawn, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
        }
    }

    @LDLRegisterClient(name = "node_element", registry = "ldlib2:ui_element_renderer")
    public static final class NodeElementRenderer extends DelegatingUIElementRenderer<NodeElement, NodeElementRenderer> {
        @Override
        public Class<NodeElement> type() {
            return NodeElement.class;
        }

        @Override
        public void drawBackgroundOverlay(NodeElement element, IGUIContext context) {
            if (!(context instanceof GUIContext guiContext)) {
                drawParentBackgroundOverlay(element, context);
                return;
            }
            element.drawBackgroundOverlay(guiContext);
        }
    }

    /**
     * Caches the tinted variants of the focus overlay, rebuilding only when the style swaps the
     * source texture.
     *
     * <p>{@link IGuiTexture#copy()} round-trips the texture through its codec — an NBT encode plus a
     * decode, and two registry serialization contexts. Doing that per frame per node was affordable
     * when only a hovered node paid it, but {@link #showHoverHighlight()} is also true for every node
     * under a rubber-band selection, so dragging a box over a large graph was paying it hundreds of
     * times a frame.
     */
    private IGuiTexture tintedOverlay(IGuiTexture source, int tint, int slot) {
        if (tintedOverlaySource != source) {
            tintedOverlaySource = source;
            tintedOverlays[0] = source.copy().setColor(HIGHLIGHT_TINT);
            tintedOverlays[1] = source.copy().setColor(HOVER_TINT);
        }
        return tintedOverlays[slot];
    }
}
