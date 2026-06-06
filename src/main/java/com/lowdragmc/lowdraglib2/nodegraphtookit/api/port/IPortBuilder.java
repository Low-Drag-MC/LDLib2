package com.lowdragmc.lowdraglib2.nodegraphtookit.api.port;

import net.minecraft.network.chat.Component;

/**
 * Base interface representing a generic port builder. Used in a builder pattern to configure and construct ports.
 *
 * <p>This interface supports a builder pattern, where each method returns the builder instance, allowing chained
 * configuration of port settings before final construction using {@link #build()}.</p>
 *
 * <p>Use derived interfaces such as {@link IInputPortBuilder} or {@link IOutputPortBuilder} to build specific port types.</p>
 */
public interface IPortBuilder<T extends IPortBuilder<T>> {
    /**
     * Builds and returns the final {@link IPort} instance based on the current configuration of the builder.
     *
     * <p>Call this method after setting all desired configuration options using the builder methods.
     * The builder captures options such as the port's data type, display name, connector style, and default value, if applicable.
     * This method finalizes the port and adds it to the graph. After calling {@code build()}, do not modify the builder further.
     * This method is typically called at the end of a chain.</p>
     *
     * @return the constructed {@link IPort}
     */
    IPort build();

    /**
     * Configures the display name of the port being built.
     *
     * <p>Use this method to assign a custom label to the port. This label appears in the user interface next to the port
     * and helps clarify its purpose. Set the display name before calling {@link #build()}. The value does not affect
     * functionality but improves usability and readability.
     * If not set, the port name passed during creation is used as the fallback display name.</p>
     *
     * @param displayName the display name to assign to the port
     * @return the current builder instance for method chaining
     */
    T withDisplayName(Component displayName);

    /**
     * Configures the connector UI shape for the port being built.
     *
     * <p>Use this method to control the appearance of the port's connector in the UI. The {@link PortConnectorUI} enum
     * provides options such as {@link PortConnectorUI#DEFAULT}. These shapes help
     * users visually distinguish between different kinds of ports or flows.
     * This setting affects only the UI and does not impact port behavior or connectivity.
     * Call this method before {@link #build()} to ensure the selected style is applied to the constructed port.</p>
     *
     * @param connectorUI the {@link PortConnectorUI} shape to use
     * @return the current builder instance for method chaining
     */
    T withConnectorUI(PortConnectorUI connectorUI);

    /**
     * Configures the orientation of the port being built. {@link PortOrientation#Horizontal}
     * (default) places the port in the node's side in/out columns; {@link PortOrientation#Vertical}
     * places it in the top (inputs) / bottom (outputs) rows rendered above the title and below the
     * body (see {@code CollapsibleInOutNodeElement}). UI-only — does not affect connectivity.
     *
     * @param orientation the {@link PortOrientation} to use
     * @return the current builder instance for method chaining
     */
    T withOrientation(PortOrientation orientation);

    /**
     * Configures the default value for the option being built.
     *
     * @param defaultValue the default value to assign to the option
     * @return the current builder instance for method chaining
     */
    T withDefaultValue(Object defaultValue);
}
