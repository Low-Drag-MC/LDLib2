package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortModelOptions;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Scope for defining sub ports. Provides methods to instantiate, configure and add sub ports within the context of a parent port.
 * @param <T>
 */
public class SubPortDefinitionScope<T extends NodeModel> {
    public final T nodeModel;
    public List<PortModel> addedPorts;
    public PortModel parentPort;
    @Getter
    private boolean mustSpecifySubPorts = false;

    public SubPortDefinitionScope(T nodeModel) {
        this.nodeModel = nodeModel;
    }

    public void refreshMustSpecifySubPorts() {
        if (parentPort == null) return;
        mustSpecifySubPorts = parentPort.isExpanded() && parentPort.areAncestorsExpanded() || recurseHasWire(parentPort);
    }

    private boolean recurseHasWire(PortModel port) {
        return port.getSubPorts().stream().anyMatch(p -> p.isConnected() || recurseHasWire(p));
    }

    public PortModel addInputSubPort(String portId,
                                     TypeHandle typeHandle,
                                     Supplier<Object> getter,
                                     Consumer<Object> setter,
                                     @Nullable PortModelOptions options) {
        var port = nodeModel.addInputSubPort(parentPort, portId, typeHandle, getter, setter, options);
        if (addedPorts != null) {
            addedPorts.add(port);
        }
        return port;
    }


    public PortModel addSubPort(String portId, TypeHandle dataType, @Nullable PortModelOptions options ) {
        var port = nodeModel.addSubPort(parentPort, portId, dataType, options);
        if (addedPorts != null) {
            addedPorts.add(port);
        }
        return port;
    }

    // todo field and property?
//    /// <inheritdoc />
//    public PortModel AddFieldSubPort(FieldInfo fieldInfo, string portName = null, string portId = null, PortModelOptions options = PortModelOptions.None, Attribute[] attributes = null)
//    {
//        var port = m_NodeModel.AddFieldSubPort(ParentPort, fieldInfo, portName, portId, options, attributes);
//        AddedPorts?.Add(port);
//        return port;
//    }
//
//    /// <inheritdoc />
//    public PortModel AddPropertySubPort(PropertyInfo propertyInfo, string portName = null, string portId = null, PortModelOptions options = PortModelOptions.None, Attribute[] attributes = null)
//    {
//        var port = m_NodeModel.AddPropertySubPort(ParentPort, propertyInfo, portName, portId, options, attributes);
//        AddedPorts?.Add(port);
//        return port;
//    }


}
