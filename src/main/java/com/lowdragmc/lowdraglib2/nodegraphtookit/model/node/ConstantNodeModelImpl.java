package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import com.lowdragmc.lowdraglib2.gui.ui.data.Tooltips;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.IConstantNode;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import com.mojang.serialization.DataResult;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

public class ConstantNodeModelImpl extends ConstantNodeModel implements IConstantNode {
    @Override
    public AbstractNodeModel getNodeModel() {
        return this;
    }

    @Override
    public Type getDataType() {
        return getConstant().getType();
    }

    @Override
    public <T> DataResult<T> tryGetValue(Type type) {
        var value = getConstant();
        if (value == null) {
            return DataResult.error(() -> "Cannot get value of constant as it has no value.");
        }
        return getConstant().tryGetValue(type);
    }

    @Override
    public @Nullable Constant getConfigurableConstant() {
        return getConstant();
    }

    @Override
    public Tooltips getTooltips() {
        return Tooltips.empty();
    }
}
