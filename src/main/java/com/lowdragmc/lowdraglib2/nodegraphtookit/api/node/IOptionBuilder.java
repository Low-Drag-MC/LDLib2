package com.lowdragmc.lowdraglib2.nodegraphtookit.api.node;

import com.lowdragmc.lowdraglib2.gui.ui.data.Tooltips;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import net.minecraft.network.chat.Component;


public interface IOptionBuilder<T extends IOptionBuilder<T>> {

    /**
     * Builds and returns the final {@link INodeOption} instance based on the current configuration of the builder.
     *
     * <p>This method is optional. All options are automatically built when the node's
     * {@link Node#onDefineOptions(IOptionDefinitionContext)} method completes.</p>
     *
     * <p>Calling this method releases the memory associated with this option back into the pool immediately.
     * You can choose to call this method if there are lots of options being defined to reduce peak memory usage.</p>
     *
     * <p>Only call this after setting all desired configuration options using the builder methods.</p>
     *
     * @return the constructed {@link INodeOption}
     */
    INodeOption build();

    /**
     * Configures the display name of the option being built.
     *
     * <p>The display name doesn't affect functionality; it can improve usability and readability.
     * If not set explicitly using this method, the name passed during creation
     * (calling {@link Node#onDefineOptions(IOptionDefinitionContext)}) is used as the default display name.</p>
     *
     * @param displayName the display name to assign to the option
     * @return the current builder instance for method chaining
     */
    T withDisplayName(Component displayName);

    /**
     * Configures the tooltip text for the option being built.
     *
     * @param tooltip the tooltip text to assign to the option
     * @return the current builder instance for method chaining
     */
    T withTooltips(Tooltips tooltips);

    /**
     * Configures the default value for the option being built.
     *
     * @param defaultValue the default value to assign to the option
     * @return the current builder instance for method chaining
     */
    T withDefaultValue(Object defaultValue);

    /**
     * Configures the option to be shown only in the inspector, not in the node header.
     *
     * @return the current builder instance for method chaining
     */
    T showInInspectorOnly();
}