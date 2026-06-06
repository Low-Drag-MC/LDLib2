package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.IFieldValueConfigurable;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.FieldValueInspector;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.ModelElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import dev.vfyjxf.taffy.style.TaffyDisplay;

import java.util.ArrayList;
import java.util.List;

public class NodeOptionsInspector extends ModelElement {
    public record OptionFieldInfo(String name, TypeHandle type, boolean inspectorOnly) {}
    public final NodeModel nodeModel;

    // runtime
    private final List<OptionFieldInfo> mutableFieldInfos = new ArrayList<>();

    public NodeOptionsInspector(NodeModel nodeModel) {
        this.nodeModel = nodeModel;
        addClass("__node-option-container__");
    }

    @Override
    protected void buildUI() {
        super.buildUI();
        Style.defaultPipeline(getLayout(), l -> l.paddingAll(3).gapAll(2).flexGrow(1));
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        if (shouldRebuildFields()) {
            buildFields();
        }
        // Hide the inspector when there are no field rows OR the node is collapsed. This is the
        // single writer of this element's display: the parent CollapsibleInOutNodeElement must not
        // also drive it, because this method runs after the parent's applyCollapsedState (parts are
        // visited after the owner) and would otherwise overwrite the collapsed state at the same
        // IMPORTANT origin — leaving options visible while collapsed.
        boolean hidden = mutableFieldInfos.isEmpty() || nodeModel.isCollapsed();
        Style.importantPipeline(getLayout(), l -> l.display(hidden ? TaffyDisplay.NONE : TaffyDisplay.FLEX));
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
                inspector.setFieldName(nodeOption.getPortModel().getDisplayName());
                if (getGraphView() != null) inspector.setHistoryStack(getGraphView().getHistoryStack());
                inspector.loadValueField(configurable);
                addChildren(inspector);
            }
        }
    }
}
