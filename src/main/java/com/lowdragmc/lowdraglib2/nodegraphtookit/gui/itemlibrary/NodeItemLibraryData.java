package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;

import java.lang.reflect.Type;
public record NodeItemLibraryData(Type type, PortModel portToConnect) implements IItemLibraryData {
}
