package com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

public class TypeConstant extends Constant {
    @Getter
    public final Type type;
    @Getter
    @Nullable
    private Object value;
    @Nullable @Getter @Setter
    private Object defaultValue;

    public TypeConstant(Type type) {
        this.type = type;
    }

    @Override
    public void setValue(Object value) {
        this.value = value;
        if (owner != null) {
            var graphModel = owner.getGraphModel();
            if (graphModel != null) {
                graphModel.getCurrentGraphChangeDescription().addChangedModel(owner, ChangeHint.DATA);
                // If OwnerModel is a PortModel, the graph object will not be marked as dirty (since PortModels are not serialized).
                // Make sure the asset is marked as dirty so the changes to the Constant are saved.
                graphModel.setGraphObjectDirty();
            }
        }
    }

    @Override
    public TypeConstant copy() {
        var copy = new TypeConstant(type);
        copy.value = value;
        copy.defaultValue = defaultValue;
        copy.init(typeHandle);
        return copy;
    }
}
