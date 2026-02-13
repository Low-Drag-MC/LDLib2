package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.IInputPortBuilder;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.IOutputPortBuilder;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.IPortBuilder;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandleHelpers;

import java.lang.reflect.Type;

public interface IPortDefinitionContext {

    /**
     * Adds a new input port.
     *
     * <p>{@code portId} is used to identify the port. It must be unique among input ports and node options on the node.
     * This name is used as the ID when calling {@link Node#getInputPortById(String)}.
     * If {@link IPortBuilder#withDisplayName(net.minecraft.network.chat.Component)} is not used, this name is also used as the port's display label.</p>
     *
     * <p><b>Warning:</b> Changing a port's name will break any existing connections, as the name is used as the port's unique ID.</p>
     *
     * <p>Use the returned builder to configure port properties and then call {@link IPortBuilder#build()} to create the port.</p>
     *
     * @param portId the unique identifier of the input port
     * @return an {@link IInputPortBuilder} to further configure the input port
     */
    IInputPortBuilder<?> addInputPort(String portId, TypeHandle typeHandle);

    default IInputPortBuilder<?> addInputPort(String portId, Type type) {
        return addInputPort(portId, TypeHandleHelpers.fromType(type));
    }

    /**
     * Adds a new output port with the specified name.
     *
     * <p>{@code portId} is used to identify the port. It must be unique among output ports on the node.
     * This name is used as the ID when calling {@link Node#getOutputPortById(String)}.
     * If {@link IPortBuilder#withDisplayName(net.minecraft.network.chat.Component)} is not used, this name is also used as the port's display label.</p>
     *
     * <p><b>Warning:</b> Changing a port's name will break any existing connections, as the name is used as the port's unique ID.</p>
     *
     * <p>Use the returned builder to configure port properties and then call {@link IPortBuilder#build()} to create the port.</p>
     *
     * @param portId the unique identifier of the output port
     * @return an {@link IOutputPortBuilder} to further configure the output port
     */
    IOutputPortBuilder<?> addOutputPort(String portId, TypeHandle typeHandle);

    default IOutputPortBuilder<?> addOutputPort(String portId, Type type) {
        return addOutputPort(portId, TypeHandleHelpers.fromType(type));
    }
}