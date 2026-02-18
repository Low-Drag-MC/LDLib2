package com.lowdragmc.lowdraglib2.nodegraphtookit.api.port;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.mojang.serialization.DataResult;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Type;

import java.util.List;

public interface IPort {
    /**
     * Gets the unique identifier name of the port.
     *
     * <p>The name is used to retrieve the port programmatically using methods like
     * {@code Node.getInputPortByName(String)} or {@code Node.getOutputPortByName(String)}.
     * It must be unique within its category (input or output) for a node.</p>
     *
     * @return the unique name of the port within its node context
     */
    String getName();

    /**
     * Gets the data type associated with the port.
     *
     * @return the data type associated with the port
     */
    Type getDataType();

    /**
     * Gets the data type handle associated with the port.
     * @return the data type handle associated with the port
     */
    TypeHandle getDataTypeHandle();

    /**
     * Gets the label displayed in the UI for the port.
     *
     * @return the UI label of the port
     */
    Component getDisplayName();

    /**
     * Gets the direction of the port.
     *
     * <p>The direction indicates whether the port is an input or output.
     * Use {@link PortDirection#INPUT} or {@link PortDirection#OUTPUT} to determine behavior.</p>
     *
     * @return the port direction
     */
    PortDirection getDirection();

    /**
     * Indicates whether the port is currently connected to any other port.
     *
     * @return {@code true} if the port has at least one connection; otherwise {@code false}
     */
    boolean isConnected();

    /**
     * Gets the first port connected to this port, if any.
     *
     * <p>If multiple connections exist, only the first connected port is returned.</p>
     *
     * @return the first connected port, or {@code null} if none
     */
    IPort getFirstConnectedPort();

    /**
     * Retrieves all ports connected to this port.
     *
     * <p>This method adds all connected ports to the provided list.
     * It clears the list before adding items.</p>
     *
     */
    void getConnectedPorts(List<IPort> ports);

    /**
     * Tries to retrieve the current value assigned to the port’s UI field.
     *
     * <p>This method is intended for editor-time inspection of an input port’s value, configured
     * through a field displayed in the UI. If the port is connected, the field is hidden and no
     * value is available, so the method returns {@code error}.</p>
     *
     * <p>If the value was never explicitly set, this method still returns {@code success}, and
     * {@code outValue} will contain the default value for the requested type.</p>
     *
     */
    <T> DataResult<T> tryGetValue(Type expectedType);

}
