package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.IFieldValueConfigurable;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.FieldValueInspector;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.ModelElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ConstantNodeModel;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ConstantNodeEditorElement extends ModelElement {
    public final ConstantNodeModel constantNodeModel;

    // runtime
    @Getter
    @Nullable
    protected FieldValueInspector editor;
    @Nullable
    protected Constant lastConstant;

    public ConstantNodeEditorElement(ConstantNodeModel constantNodeModel) {
        this.constantNodeModel = constantNodeModel;
        addClass("__constant-node-editor__");
        editor = new FieldValueInspector();
        addChild(editor);
    }

    @Override
    protected void buildUI() {
        if (constantNodeModel instanceof IFieldValueConfigurable) {
            editor = new FieldValueInspector();
            addChild(editor);
        }
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        if (editor != null && constantNodeModel instanceof IFieldValueConfigurable configurable) {
            if (Objects.equals(lastConstant, constantNodeModel.getConstant())) return;
            lastConstant = constantNodeModel.getConstant();
            if (getGraphView() != null) editor.setHistoryStack(getGraphView().getHistoryStack());
            editor.loadValueField(configurable);
        }
    }
}
