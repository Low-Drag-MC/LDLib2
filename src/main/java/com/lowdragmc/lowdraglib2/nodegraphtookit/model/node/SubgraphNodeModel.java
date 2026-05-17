package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortDirection;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortType;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.CollapsibleInOutNodeElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.SubgraphNodeElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.Capabilities;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.NodeDefinitionScope;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.ModifierFlags;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A node that represents another graph nested inside this one. The inner graph's variables
 * (with READ/WRITE {@link ModifierFlags}) are mirrored as ports on this node, with the direction
 * inverted relative to the inner {@link VariableNodeModel}: inside the inner graph a READ variable
 * is read as an output port; on the outer subgraph node a READ variable becomes an <em>input</em>
 * (the outer graph passes a value in).
 *
 * <p>Two flavors via {@link Kind}:
 * <ul>
 *     <li>{@link Kind#LOCAL} — the inner graph is owned inline by the parent graph's
 *         {@link GraphModel#getLocalSubGraphs() localSubGraphs}; identified by uid.</li>
 *     <li>{@link Kind#EXTERNAL} — the inner graph is an external resource referenced by
 *         {@link IResourcePath}; resolved lazily via {@link GraphModel#getReferenceResolver()}.</li>
 * </ul>
 * When the inner graph isn't resolvable (e.g. running without an editor context), the node falls
 * back to a {@link #portCache cached port shape} captured the last time the inner graph was visible
 * so wires stay connected to ports with the same id.
 */
public class SubgraphNodeModel extends NodeModel {

    public enum Kind { LOCAL, EXTERNAL }

    @Persisted @Getter
    private Kind kind = Kind.LOCAL;
    /** Uid of the local subgraph in {@code parent.localSubGraphs}. Used when {@link #kind} == LOCAL. */
    @Persisted @Getter @Nullable
    private UUID localGraphId;
    /** Serialized form of {@link IResourcePath} (via {@link IResourcePath#getPathWithType()}). EXTERNAL only. */
    @Persisted @Nullable
    private String externalPathString;

    /** Frozen snapshot of the last-known port shape; used when the inner graph isn't resolvable. */
    private final List<CachedPort> portCache = new ArrayList<>();

    /** Cached resolved external graph; cleared by {@link #invalidateResolvedSubgraph()} on external save. */
    @Nullable
    private transient GraphModel resolvedExternal;

    public SubgraphNodeModel() {
        capabilities.add(Capabilities.RENAMABLE);
    }

    /** A row in {@link #portCache}: enough to recreate a port that wires can still resolve to. */
    private record CachedPort(String portId, @Nullable UUID varUid, String typeId, PortDirection direction) {}

    // ---------------------------------------------------------------------
    // Configuration helpers — called by commands when building a subgraph node
    // ---------------------------------------------------------------------

    /** Bind this node to a local subgraph (must have been added to the parent's localSubGraphs first). */
    public void setLocalSubgraph(GraphModel localGraph) {
        this.kind = Kind.LOCAL;
        this.localGraphId = localGraph == null ? null : localGraph.getUid();
        this.externalPathString = null;
        this.resolvedExternal = null;
    }

    /**
     * Rebinds this LOCAL subgraph reference to a different inner-graph uid <em>without</em>
     * needing a {@link GraphModel} reference. Used by the copy/paste pipeline: the cloned inner
     * graph has been deserialized and added to the new parent under a fresh uid, and we need to
     * point this pasted node at that fresh uid. No-op if the node is not in LOCAL kind.
     */
    public void rebindLocalGraphId(UUID newUid) {
        if (kind != Kind.LOCAL) return;
        this.localGraphId = newUid;
    }

    /** Bind this node to an external graph resource. */
    public void setExternalSubgraph(IResourcePath path) {
        this.kind = Kind.EXTERNAL;
        this.externalPathString = path == null ? null : path.getPathWithType();
        this.localGraphId = null;
        this.resolvedExternal = null;
    }

    @Nullable
    public IResourcePath getExternalPath() {
        return externalPathString == null ? null : IResourcePath.parse(externalPathString);
    }

    public void invalidateResolvedSubgraph() {
        this.resolvedExternal = null;
    }

    // ---------------------------------------------------------------------
    // ISubgraphNode / inner-graph resolution
    // ---------------------------------------------------------------------

    @Nullable
    public GraphModel getSubgraphModel() {
        if (kind == Kind.LOCAL) {
            if (graphModel == null || localGraphId == null) return null;
            return graphModel.findLocalSubgraphByUid(localGraphId);
        }
        // EXTERNAL
        if (resolvedExternal != null) return resolvedExternal;
        if (graphModel == null || externalPathString == null) return null;
        var resolver = graphModel.getReferenceResolver();
        if (resolver == null) return null;
        var path = IResourcePath.parse(externalPathString);
        if (path == null) return null;
        var graph = resolver.resolve(path);
        if (graph != null) {
            resolvedExternal = graph.graphModel;
        }
        return resolvedExternal;
    }

    public boolean isReferencingLocalSubgraph() {
        return kind == Kind.LOCAL;
    }

    /** Convenience: peeks at the inner Graph wrapper. Null if unresolved or not a CustomGraphModelImpl. */
    @Nullable
    public Graph getSubgraph() {
        var model = getSubgraphModel();
        if (model instanceof CustomGraphModelImpl custom) {
            return custom.getGraph();
        }
        return null;
    }

    @Override
    public Component getTooltip() {
        return super.getTooltip().copy().append("-").append(Component.translatable("graph.breadcrumb.subgraph"));
    }

    @Override
    public @Nullable GraphElement<?> createElementUI() {
        return new SubgraphNodeElement(this);
    }

    // ---------------------------------------------------------------------
    // Port definition — drive ports from the inner graph's variables (inverted)
    // ---------------------------------------------------------------------

    @Override
    protected void onDefineNode(NodeDefinitionScope<? extends NodeModel> scope) {
        var target = getSubgraphModel();
        if (target == null) {
            // unresolved — replay cached port shape so wires can survive
            for (var cached : portCache) {
                var type = TypeHandle.create(cached.typeId);
                if (cached.direction == PortDirection.INPUT) {
                    scope.nodeModel.addInputPort(cached.portId, type, null, null, null, null, null);
                } else {
                    scope.nodeModel.addOutputPort(cached.portId, type, null, null, null);
                }
            }
            return;
        }
        // rebuild from inner variables
        portCache.clear();
        for (var variable : target.getGraphVariableModels()) {
            if (variable == null) continue;
            var mods = variable.getModifiers();
            if (mods == null || mods == ModifierFlags.NONE) continue;
            var type = variable.getDataTypeHandle();
            if (type == null) type = TypeHandles.UNKNOWN;
            var varUid = variable.getUid();
            var displayName = variable.getName();

            boolean hasRead = mods.hasFlag(ModifierFlags.READ);
            boolean hasWrite = mods.hasFlag(ModifierFlags.WRITE);

            if (hasRead && hasWrite) {
                // READ_WRITE: both directions, suffixed ids so wires can target one specifically
                var inId = varUid.toString() + "-in";
                var outId = varUid.toString() + "-out";
                var inPort = scope.nodeModel.addInputPort(inId, type, null, null, null, null, null);
                inPort.setTitle(Component.literal(displayName));
                var outPort = scope.nodeModel.addOutputPort(outId, type, null, null, null);
                outPort.setTitle(Component.literal(displayName));
                portCache.add(new CachedPort(inId, varUid, type.getIdentification(), PortDirection.INPUT));
                portCache.add(new CachedPort(outId, varUid, type.getIdentification(), PortDirection.OUTPUT));
            } else if (hasRead) {
                // READ inner var → INPUT on outer subgraph node
                var portId = varUid.toString();
                var port = scope.nodeModel.addInputPort(portId, type, null, null, null, null, null);
                port.setTitle(Component.literal(displayName));
                portCache.add(new CachedPort(portId, varUid, type.getIdentification(), PortDirection.INPUT));
            } else { // hasWrite
                // WRITE inner var → OUTPUT on outer subgraph node
                var portId = varUid.toString();
                var port = scope.nodeModel.addOutputPort(portId, type, null, null, null);
                port.setTitle(Component.literal(displayName));
                portCache.add(new CachedPort(portId, varUid, type.getIdentification(), PortDirection.OUTPUT));
            }
        }
    }

    // ---------------------------------------------------------------------
    // Serialization — portCache (kind / localGraphId / externalPathString are auto via @Persisted)
    // ---------------------------------------------------------------------

    @Override
    public Tag serializeAdditionalNBT(HolderLookup.Provider provider) {
        var base = super.serializeAdditionalNBT(provider);
        var tag = base instanceof CompoundTag ct ? ct : new CompoundTag();
        if (!portCache.isEmpty()) {
            var listTag = new ListTag();
            for (var cached : portCache) {
                var entry = new CompoundTag();
                entry.putString("portId", cached.portId);
                if (cached.varUid != null) entry.putUUID("varUid", cached.varUid);
                entry.putString("typeId", cached.typeId);
                entry.putString("dir", cached.direction.name());
                listTag.add(entry);
            }
            tag.put("portCache", listTag);
        }
        return tag;
    }

    @Override
    public void deserializeAdditionalNBT(Tag tag, HolderLookup.Provider provider) {
        super.deserializeAdditionalNBT(tag, provider);
        portCache.clear();
        if (tag instanceof CompoundTag compound && compound.contains("portCache")) {
            var listTag = compound.getList("portCache", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                var entry = listTag.getCompound(i);
                var portId = entry.getString("portId");
                var typeId = entry.getString("typeId");
                UUID varUid = entry.contains("varUid") ? entry.getUUID("varUid") : null;
                PortDirection dir;
                try {
                    dir = PortDirection.valueOf(entry.getString("dir"));
                } catch (IllegalArgumentException e) {
                    continue;
                }
                portCache.add(new CachedPort(portId, varUid, typeId, dir));
            }
        }
    }
}
