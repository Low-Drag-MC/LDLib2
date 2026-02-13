package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortType;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.Capabilities;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ContextualMenuItem;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.NodeDefinitionScope;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ConstantNodeModel extends NodeModel implements ISingleOutputPortNodeModel {
    public final static String OUTPUT_PORT_ID = "Output_0";
    @Getter
    private Constant value;

    public ConstantNodeModel() {
        setCapability(Capabilities.COLORABLE, false);
    }

    @Override
    public Component getTitle() {
        return Component.empty();
    }

    @Override
    public PortModel getOutputPort() {
        return getOutputPortInfos().portsById.values().getFirst();
    }

    public void setValue(@Nullable Constant value) {
        if (this.value == value) return;
        // Unregister ourselves as the owner of the old constant.
        if (this.value != null) {
            this.value.setOwner(null);
        }
        this.value = value;
        if (this.value != null) {
            this.value.setOwner(this);
        }
        if (graphModel != null) {
            graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
        }
    }

    /**
     * Sets the value of the constant.
     * @param value the value to set.
     */
    public void setConstantValue(Object value) {
        getValue().setValue(value);
    }

    public Type getType() {
        return value.getType();
    }

    @Override
    protected void onDefineNode(NodeDefinitionScope<? extends NodeModel> scope) {
        scope.nodeModel.addOutputPort(OUTPUT_PORT_ID, getValue().getTypeHandle(), PortType.DEFAULT, null, null);
    }

    @Override
    public List<ContextualMenuItem> getContextualMenuItems() {
        var menuItems = new ArrayList<>(super.getContextualMenuItems());
        menuItems.addAll(MENU_ITEMS);
        return menuItems;
    }

    protected static final List<ContextualMenuItem> MENU_ITEMS = List.of(
//            ContextualMenuHelpers.convertToVariableItem,
//            new ContextualMenuItem(ContextualMenuHelpers.itemizeItem, 0),
    );
}
