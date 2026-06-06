package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphInspector;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
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

    @Getter
    private final NodeStyle nodeStyle = new NodeStyle();

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

    @Override
    public void drawBackgroundOverlay(@NotNull GUIContext guiContext) {
        if (isSelected()) {
            guiContext.drawTexture(getNodeStyle().focusOverlay(),
                    getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
        } else if (shouldBeHighlighted()) {
            guiContext.drawTexture(getNodeStyle().focusOverlay().copy().setColor(0xddffaf00),
                    getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
        } else if (showHoverHighlight()) {
            guiContext.drawTexture(getNodeStyle().focusOverlay().copy().setColor(0xaaffffff),
                    getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
        }
        super.drawBackgroundOverlay(guiContext);
    }
}
