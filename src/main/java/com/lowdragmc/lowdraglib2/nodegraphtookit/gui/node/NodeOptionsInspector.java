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
    public record OptionFieldInfo(String name, TypeHandle type, boolean inspectorOnly, boolean configuratorEnabled) {}
    public final NodeModel nodeModel;

    // runtime
    private final List<OptionFieldInfo> mutableFieldInfos = new ArrayList<>();
    /** How many option rows {@link #buildFields()} actually added (options without a configurator add none). */
    private int rowCount;

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
        boolean hidden = rowCount == 0 || nodeModel.isCollapsed();
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
            // a node that toggles which of its options are editable (e.g. a mode option swapping in a
            // different value editor) changes nothing else — without this the UI would never refresh
            if (currentOption.getPortModel().isConfiguratorEnabled() != oldOption.configuratorEnabled) return true;
        }

        return false;
    }

    protected void buildFields() {
        mutableFieldInfos.clear();
        clearAllChildren();
        rowCount = 0;
        for (var nodeOption : nodeModel.getNodeOptions()) {
            var portModel = nodeOption.getPortModel();
            // every option gets an info entry (shouldRebuildFields compares them positionally), but only
            // the ones with a configurator get a row
            mutableFieldInfos.add(new OptionFieldInfo(
                    portModel.getUniqueName(),
                    portModel.getDataTypeHandle(),
                    nodeOption.isShowInInspectorOnly(),
                    portModel.isConfiguratorEnabled())
            );
            if (!portModel.isConfiguratorEnabled()) continue; // else it'd render as a label with nothing beside it
            if (portModel instanceof IFieldValueConfigurable configurable) {
                var inspector = new FieldValueInspector();
                inspector.setFieldName(portModel.getDisplayName());
                if (getGraphView() != null) inspector.setHistoryStack(getGraphView().getHistoryStack());
                inspector.loadValueField(configurable);
                addChildren(inspector);
                rowCount++;
            }
        }
    }
}
