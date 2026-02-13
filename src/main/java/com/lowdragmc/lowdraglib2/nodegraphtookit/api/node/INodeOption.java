package com.lowdragmc.lowdraglib2.nodegraphtookit.api.node;

import com.mojang.serialization.DataResult;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Type;

public interface INodeOption {
    /**
     * Gets the data type of the node option.
     *
     * @return the data type of the option
     */
    Type getDataType();

    /**
     * Gets the unique identifier of the node option.
     *
     * @return the unique name/id of the option
     */
    String getId();

    /**
     * Gets the display name of the node option shown in the UI.
     *
     * @return the display name shown in UI
     */
    Component getDisplayName();

    /**
     * Retrieves the value associated with the node option.
     *
     * <p>This method can be used to get the underlying data stored in the current option,
     * with the return type determined by the generic parameter {@code <T>}.</p>
     *
     * @param <T> the type of the value to be returned
     * @return the value of the node option cast to the specified type {@code <T>}
     */
    <T> DataResult<T> tryGetValue(Type expectedType);
}