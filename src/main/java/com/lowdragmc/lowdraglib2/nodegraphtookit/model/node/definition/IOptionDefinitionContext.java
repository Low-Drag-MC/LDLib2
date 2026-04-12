package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.IOptionBuilder;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandleHelpers;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Type;
public interface IOptionDefinitionContext {

    /**
     * Adds a new node option.
     *
     * <p>{@code name} is used to identify the option. It must be unique among ports and options on the node.
     * This name is used as the ID when calling {@link Node#getNodeOptionById(String)}.
     * If {@link IOptionBuilder#withDisplayName(Component)} is not used, this name is also used as the option's display label.</p>
     *
     * @param name     the unique identifier of the option
     * @param typeHandle the data type of the option
     * @return an {@link IOptionBuilder} to further configure the option
     */
    IOptionBuilder<?> addOption(String name, TypeHandle typeHandle);

    default IOptionBuilder<?> addOption(String name, Type type) {
        return addOption(name, TypeHandleHelpers.fromType(type));
    }
}