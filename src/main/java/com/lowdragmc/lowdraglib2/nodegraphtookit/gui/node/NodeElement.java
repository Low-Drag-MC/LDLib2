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
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.*;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NodeElement extends GraphElement<AbstractNodeModel> {
    public final static String NODE_LAYER = "Node";

    @Configurable(name = "NodeStyle")
    public class NodeStyle extends Style {
        private static final Property<?>[] PROPERTIES = new Property[] {
                PropertyRegistry.FOCUS_OVERLAY,
                PropertyRegistry.HOVER_OVERLAY,
        };

        protected NodeStyle() {
            super(NodeElement.this);
            setDefault(PropertyRegistry.HOVER_OVERLAY, ColorPattern.T_LIGHT_BLUE.borderTexture(1));
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

        public IGuiTexture hoverOverlay() {
            return getValueSave(PropertyRegistry.HOVER_OVERLAY);
        }

        public NodeStyle hoverOverlay(IGuiTexture texture) {
            set(PropertyRegistry.HOVER_OVERLAY, texture);
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


    @Override
    public void drawBackgroundOverlay(@NotNull GUIContext guiContext) {
        if (isSelected()) {
            guiContext.drawTexture(getNodeStyle().focusOverlay(), getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
        } else {
            var isHover = isSelfOrChildHover() || isUnderRegionSelection();
            if (isHover) {
                guiContext.drawTexture(getNodeStyle().hoverOverlay(), getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
            }
        }
        super.drawBackgroundOverlay(guiContext);
    }
}
