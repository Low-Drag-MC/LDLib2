package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.BlockNode;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.ContextNode;
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
import java.util.List;
import java.util.Map;

/**
 * Concrete {@link ContextNodeModel} that wraps a user-defined {@link ContextNode}, matching the
 * {@code NodeModel} + {@link CustomNodeModelImpl} pattern.
 */
public class CustomContextNodeModelImpl extends ContextNodeModel implements ICustomNodeModel {
    @Getter @Nullable
    private Node node;
    @Getter
    private final Map<String, INodeOption> optionsById = new HashMap<>();

    @Override
    public void initCustomNode(Node node) {
        this.node = node;
        setTitle(node.getDisplayName());
        node.setImplementation(this);
    }

    @Nullable
    public ContextNode getContextNode() {
        return node instanceof ContextNode cn ? cn : null;
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
        if (node == null) return;
        optionsById.clear();
        try {
            var optCtx = NodeDefinitionScope.optionContext.get();
            optCtx.setScope(definitionScope);
            node.onDefineOptions(optCtx);
            optCtx.finish();
            for (var nodeOption : getNodeOptions()) {
                optionsById.put(nodeOption.getId(), nodeOption);
            }

            var portCtx = NodeDefinitionScope.portContext.get();
            portCtx.setScope(definitionScope);
            node.onDefinePorts(portCtx);
            portCtx.finish();
        } catch (Exception e) {
            LDLib2.LOGGER.error("Failed to define context node {}", node, e);
        }
    }

    @Override
    protected PortModelImpl createPort(PortDirection direction, PortOrientation orientation, String portId,
                                       PortType portType, TypeHandle dataType, PortModelOptions options,
                                       @Nullable PortModel parentPort) {
        return new PortModelImpl(this, direction, orientation, portId, portType, dataType, options, parentPort);
    }

    // --------------------------------------------------------------
    // User-aware block compatibility (delegates to ContextNode)
    // --------------------------------------------------------------

    @Override
    public boolean acceptsBlock(BlockNodeModel block) {
        if (!(block instanceof CustomBlockNodeModelImpl impl) || impl.getBlockNode() == null) {
            // Non-custom blocks: deny by default in a user-defined context — there's no class
            // info we can validate against. Subclasses can override to relax this.
            return false;
        }
        var ctx = getContextNode();
        return ctx != null && ctx.acceptsBlock(impl.getBlockNode().getClass());
    }

    @Override
    public List<Class<? extends BlockNode>> getSupportBlockClasses() {
        var ctx = getContextNode();
        return ctx == null ? List.of() : ctx.getSupportBlocks();
    }
}
