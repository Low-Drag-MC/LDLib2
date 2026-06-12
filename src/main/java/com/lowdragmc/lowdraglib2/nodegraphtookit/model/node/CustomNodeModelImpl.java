package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.INodeOption;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortDirection;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortModelOptions;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortOrientation;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortType;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.NodeDefinitionScope;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class CustomNodeModelImpl extends NodeModel implements ICustomNodeModel {
    @Getter @Nullable
    private Node node;
    // runtime
    @Getter
    private Map<String, INodeOption> optionsById = new HashMap<>();

    /**
     * Initializes the custom node by associating it with the containing model implementation.
     *
     * @param node the {@link Node} to be initialized; it represents the custom node to be linked to this model.
     */
    public void initCustomNode(Node node) {
        this.node = node;
        this.setTitle(node.getDisplayName());
        this.node.setImplementation(this);
    }

    @Override
    public IGuiTexture getNodeIcon() {
        return node == null ? super.getNodeIcon() : node.getNodeIcon();
    }

    @Override
    public float getNodeWidth() {
        return node == null ? super.getNodeWidth() : node.getNodeWidth();
    }

    @Override
    protected void onDefineNode(NodeDefinitionScope<? extends NodeModel> definitionScope) {
        if (node == null)
            return;
        optionsById.clear();

        try {
            callOnDefineOptions(definitionScope);
            for (var nodeOption : getNodeOptions()) {
                optionsById.put(nodeOption.getId(), nodeOption);
            }
            callOnDefineNode(definitionScope);
        } catch (Exception e) {
            LDLib2.LOGGER.error("Failed to define node {}", node, e);
        }
    }

    protected void callOnDefineOptions(NodeDefinitionScope<? extends NodeModel> definitionScope) {
        var context = NodeDefinitionScope.optionContext.get();
        context.setScope(definitionScope);
        assert node != null;
        node.onDefineOptions(context);
        context.finish();
    }

    protected void callOnDefineNode(NodeDefinitionScope<? extends NodeModel> definitionScope) {
        var context = NodeDefinitionScope.portContext.get();
        context.setScope(definitionScope);
        assert node != null;
        node.onDefinePorts(context);
        context.finish();
    }

    @Override
    protected PortModelImpl createPort(PortDirection direction, PortOrientation orientation, String portId, PortType portType, TypeHandle dataType, PortModelOptions options, @Nullable PortModel parentPort) {
        return new PortModelImpl(this, direction, orientation, portId, portType, dataType, options, parentPort);
    }

    @Override
    public boolean hasNodePreview() {
        // Custom nodes opt into a preview via their Node; the preview content is built by the
        // node's onBuildNodePreview hook (see NodePreviewElement).
        return node != null && node.hasNodePreview();
    }

    @Override
    public boolean isNodePreviewExpandedByDefault() {
        return node == null || node.isNodePreviewExpandedByDefault();
    }
}
