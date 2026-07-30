package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import lombok.Getter;

/**
 * Library entry for one of the ports a {@link NodeModelLibraryItem} can be wired to, shown as a child
 * row of its owner when the library is opened from a wire drag (see
 * {@link ItemLibrary#showWithNodesFitPort}).
 *
 * <p>Choosing such an item creates the owner's node and connects the dragged wire to exactly this
 * port, instead of the first compatible one. The {@link #port} belongs to the throwaway model the
 * library builds to inspect a node type — the same contract {@link NodeItemLibraryData} relies on,
 * where the port is later resolved on the real node by its unique name.</p>
 */
public class PortLibraryItem extends ItemLibraryItem {
    @Getter
    private final NodeModelLibraryItem owner;
    @Getter
    private final PortModel port;

    public PortLibraryItem(NodeModelLibraryItem owner, PortModel port) {
        this.owner = owner;
        this.port = port;
        this.icon = port.getConnectorUI().getIcon(false).copy().setColor(port.getDataTypeHandle().getTypeColor());
        this.displayName = port.getDisplayName();
        this.searchableName = port.getUniqueName();
    }
}
