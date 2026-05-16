package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElementRendererRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.DelegatingUIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.nodegraphtookit.editor.GraphEditorView;
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

    @Getter
    private final NodeStyle nodeStyle = new NodeStyle();

    public NodeElement(AbstractNodeModel nodeModel) {
        super(nodeModel);
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
    }

    @Override
    protected void buildUI() {
        getLayout().positionType(TaffyPosition.ABSOLUTE);
        getStyle().background(Sprites.RECT_SOLID);

        addChildren(nodeTittle, nodeOptionContainer, portContainerElement);
        internalSetup();

        // Double-click on a subgraph node enters its inner graph. View-level navigation —
        // intentionally not routed through the command/history system so per-level history is kept
        // independent and clean.
        if (getModel() instanceof SubgraphNodeModel) {
            addEventListener(UIEvents.DOUBLE_CLICK, event -> {
                if (!(getModel() instanceof SubgraphNodeModel subNode)) return;
                var editorView = getFirstAncestorOfType(GraphEditorView.class);
                if (editorView != null) {
                    editorView.enterSubgraph(subNode);
                    event.stopPropagation();
                }
            });
        }
    }

    // endregion

    @Override
    public boolean hasModelDependenciesChanged() {
        return getModel() instanceof InputOutputPortsNodeModel ioNode && !ioNode.getNodeOptions().isEmpty();
    }

    @Override
    public void addModelDependencies() {
        super.addModelDependencies();
        if (getModel() instanceof InputOutputPortsNodeModel ioNode) {
            for (var nodeOption : ioNode.getNodeOptions()) {
                getDependencies().addModelDependency(nodeOption.getPortModel());
            }
        }
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        var model = getModel();
        // update layout
        if (visitor.hasHint(ChangeHint.LAYOUT)) {
            getLayout().left(model.getPosition().x).top(model.getPosition().y);
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

    @Override
    protected void onSelectionInspect(GraphInspector inspector) {
        super.onSelectionInspect(inspector);
        if (graphView != null) inspector.setHistoryStack(graphView.getHistoryStack());
        inspector.inspect(RenameColorConfigurableHelper.build(getModel(), graphView));
    }

    public void drawBackgroundOverlay(@NotNull GUIContext context) {
        UIElementRendererRegistry.defaultRenderer().drawBackgroundOverlay(this, context);
        if (isSelected()) {
            context.drawTexture(getNodeStyle().focusOverlay(),
                    getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
        } else if (shouldBeHighlighted()) {
            context.drawTexture(getNodeStyle().focusOverlay().copy().setColor(0xddffaf00),
                    getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
        } else {
            var isHover = isSelfOrChildHover() || isUnderRegionSelection();
            if (isHover) {
                context.drawTexture(getNodeStyle().focusOverlay().copy().setColor(0xaaffffff),
                        getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
            }
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
}
