package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.google.common.collect.Maps;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.DependencyElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.*;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

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

    public final UIElement nodeTitleBar;
    public final UIElement nodeIcon;
    public final Label nodeTittle;
    public final PortContainer nodeOptionContainer;
    public final UIElement nodePortContainer;
    public final PortContainer nodeInputPortContainer;
    public final PortContainer nodeOutputPortContainer;
    @Getter
    private final NodeStyle nodeStyle = new NodeStyle();
    // runtime
    @Nullable
    protected Map<PortModel, PortElement> portElements;

    public NodeElement(AbstractNodeModel nodeModel) {
        super(nodeModel);
        getLayout().positionType(TaffyPosition.ABSOLUTE);
        getStyle().background(Sprites.RECT_SOLID);

        this.nodeTitleBar = new UIElement().setId("node-title-bar").setOverflowVisible(false);
        this.nodeTitleBar.getLayout().alignItems(AlignItems.CENTER).minWidthAuto().minHeightAuto()
                .gapAll(2).paddingVertical(3).paddingHorizontal(4).flexDirection(FlexDirection.ROW);
        this.nodeTitleBar.getStyle().background(Sprites.BORDER_DARK);

        this.nodeIcon = new UIElement().setId("node-title-icon");
        this.nodeIcon.getLayout().aspectRatio(1).width(10);

        this.nodeTittle = new Label();
        this.nodeTittle.setId("node-title");
        this.nodeTittle.getTextStyle().adaptiveWidth(true).adaptiveHeight(true);

        this.nodeTitleBar.addChildren(nodeIcon, nodeTittle);

        this.nodeOptionContainer = new PortContainer(this);
        this.nodeOptionContainer.setId("node-option-container");
        this.nodeOptionContainer.getLayout().paddingAll(3).gapAll(2).minWidth(100);

        this.nodePortContainer = new UIElement().setId("node-port-container");
        this.nodePortContainer.getLayout().flexDirection(FlexDirection.ROW);

        this.nodeInputPortContainer = new PortContainer(this);
        this.nodeInputPortContainer.setId("node-input-port-container");
        this.nodeInputPortContainer.getLayout().paddingAll(4).gapAll(2).flexGrow(1);
        this.nodeInputPortContainer.getStyle().background(Sprites.RECT_LIGHT);

        this.nodeOutputPortContainer = new PortContainer(this);
        this.nodeOutputPortContainer.setId("node-output-port-container");
        this.nodeOutputPortContainer.getLayout().paddingAll(4).gapAll(2).flexGrow(1);
        this.nodeOutputPortContainer.getStyle().background(Sprites.RECT_SOLID);

        this.nodePortContainer.addChildren(nodeInputPortContainer, nodeOutputPortContainer);

        addChildren(nodeTitleBar, nodeOptionContainer, nodePortContainer);

        internalSetup();
    }

    @Override
    public String getLayerName() {
        return NODE_LAYER;
    }

    @Override
    public boolean hasModelDependenciesChanged() {
        return model instanceof InputOutputPortsNodeModel ioNode && !ioNode.getNodeOptions().isEmpty();
    }

    @Override
    public void addModelDependencies() {
        super.addModelDependencies();
        if (model instanceof InputOutputPortsNodeModel ioNode) {
            for (var nodeOption : ioNode.getNodeOptions()) {
                getDependencies().addModelDependency(nodeOption.getPortModel());
            }
        }
    }

    @Override
    public Collection<PortElement> getChildDependencies() {
        return getPortElements().values();
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        super.updateUIFromModel(visitor);
        // update layout
        if (visitor.hasHint(ChangeHint.LAYOUT)) {
            getLayout().left(model.getPosition().x).top(model.getPosition().y);
        }
        // update style
        if (visitor.hasHint(ChangeHint.STYLE)) {
            // title
            nodeTittle.setText(model.getTitle());
            // icon
            nodeIcon.getStyle().background(model.getNodeIcon());
        }
        // update data
        if (visitor.hasHint(ChangeHint.DATA)) {
            if (model instanceof InputOutputPortsNodeModel ioNode) {
                portElements = null;
                nodeOptionContainer.loadPorts(ioNode.getNodeOptions().stream().map(NodeOption::getPortModel).toList());
                portElements = null;
                nodeInputPortContainer.loadPorts(ioNode.getVisibleInputsByDisplayOrder());
                nodeOutputPortContainer.loadPorts(ioNode.getVisibleOutputsByDisplayOrder());
            }
        }
    }

    /**
     * Get all port elements in this node (includes option, input and output ports).
     */
    public Map<PortModel, PortElement> getPortElements() {
        if (portElements == null) {
            portElements = Maps.newHashMap();
            portElements.putAll(nodeOptionContainer.getPortElements());
            portElements.putAll(nodeInputPortContainer.getPortElements());
            portElements.putAll(nodeOutputPortContainer.getPortElements());
        }
        return portElements;
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
