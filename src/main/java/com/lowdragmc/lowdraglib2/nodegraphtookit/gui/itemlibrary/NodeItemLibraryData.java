package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;

import java.lang.reflect.Type;

/**
 * Data for a {@link ItemLibraryItem} linked to a node.
 * @param type The type of the node represented by the item.
 * @param portToConnect The port to which the node will be connected.
 */
public record NodeItemLibraryData(Type type, PortModel portToConnect) implements IItemLibraryData {
}
