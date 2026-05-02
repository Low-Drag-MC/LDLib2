package com.lowdragmc.lowdraglib2.nodegraphtookit.api.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.IFieldConstantConfigurable;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.mojang.serialization.DataResult;

import java.lang.reflect.Type;

/**
 * Interface for a specialized node that outputs a fixed value of a specific data type.
 *
 * <p>Use constant nodes to represent a static, predefined value in the graph. This value remains unchanged
 * and is typically used to feed constant input into computations.
 * To retrieve the value, use {@link #tryGetValue(Type)}. This method is type-safe and provides access
 * to the node's value if the type matches.
 * The {@link #getDataType()} property identifies the constant's type.</p>
 */
public interface IConstantNode extends INode, IFieldConstantConfigurable {

    /**
     * Gets the data type of the constant node's value.
     *
     * <p>The type returned by this property indicates the kind of value the constant node holds,
     * such as {@code Float}, {@code Integer}, {@code String}, or a custom type.</p>
     *
     * @return the data type of the constant value
     */
    Type getDataType();

    /**
     * Attempts to retrieve the value of the constant node as the specified type.
     *
     * <p>This method provides type-safe access to the constant's stored value. It performs a type check
     * and conversion internally. If the value cannot be cast to the specified type, the method returns {@code null}.</p>
     *
     * @param <T> the type to retrieve the value as
     * @param type the class of the expected type
     * @return the value if successfully retrieved and cast, or {@code null} if the type doesn't match
     */
    <T> DataResult<T> tryGetValue(Type type);

    @Override
    default void onValueChanged() {
        var nodeModel = this.getNodeModel();
        if (nodeModel.getGraphModel() != null) {
            nodeModel.getGraphModel().getCurrentGraphChangeDescription().addChangedModel(nodeModel, ChangeHint.DATA);
        }
    }
}
