package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.IConstantNode;
import com.mojang.serialization.DataResult;

import java.lang.reflect.Type;

public class ConstantNodeModelImpl extends ConstantNodeModel implements IConstantNode {
    @Override
    public AbstractNodeModel getNodeModel() {
        return this;
    }

    @Override
    public Type getDataType() {
        return getValue().getType();
    }

    @Override
    public <T> DataResult<T> tryGetValue(Type type) {
        var value = getValue();
        if (value == null) {
            return DataResult.error(() -> "Cannot get value of constant as it has no value.");
        }
        return getValue().tryGetValue(type);
    }
}
