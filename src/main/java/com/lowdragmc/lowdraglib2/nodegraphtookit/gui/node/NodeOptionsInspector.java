package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.IFieldValueConfigurable;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.FieldValueInspector;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.ModelElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;

import java.util.ArrayList;
import java.util.List;

public class NodeOptionsInspector extends ModelElement {
    public record OptionFieldInfo(String name, TypeHandle type, boolean inspectorOnly) {}
    public final NodeModel nodeModel;

    // runtime
    private final List<OptionFieldInfo> mutableFieldInfos = new ArrayList<>();

    public NodeOptionsInspector(NodeModel nodeModel) {
        this.nodeModel = nodeModel;
    }

    @Override
    protected void buildUI() {
        super.buildUI();
        this.setId("node-option-container");
        this.getLayout().paddingAll(3).gapAll(2).flexGrow(1);
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        if (shouldRebuildFields()) {
            buildFields();
        }
        setDisplay(!mutableFieldInfos.isEmpty());
    }

    protected boolean shouldRebuildFields() {
        var options = nodeModel.getNodeOptions();
        if (options.size() != mutableFieldInfos.size()) return true;

        for (int i = 0; i < options.size(); i++) {
            var oldOption = mutableFieldInfos.get(i);
            var currentOption = options.get(i);
            if (!currentOption.getPortModel().getUniqueName().equals(oldOption.name)) return true;
            if (!currentOption.getPortModel().getDataTypeHandle().equals(oldOption.type)) return true;
            if (currentOption.isShowInInspectorOnly() != oldOption.inspectorOnly) return true;
        }

        return false;
    }

    protected void buildFields() {
        mutableFieldInfos.clear();
        for (var nodeOption : nodeModel.getNodeOptions()) {
            mutableFieldInfos.add(new OptionFieldInfo(
                    nodeOption.getPortModel().getUniqueName(),
                    nodeOption.getPortModel().getDataTypeHandle(),
                    nodeOption.isShowInInspectorOnly())
            );
            if (nodeOption.getPortModel() instanceof IFieldValueConfigurable configurable) {
                var inspector = new FieldValueInspector();
                inspector.fieldName.setText(nodeOption.getPortModel().getDisplayName());
                inspector.loadValueField(configurable);
                addChildren(inspector);
            }
        }
    }
}
