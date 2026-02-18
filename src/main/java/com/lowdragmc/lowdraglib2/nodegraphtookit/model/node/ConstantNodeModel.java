package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortType;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.CapsuleNodeElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.Capabilities;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ContextualMenuItem;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.NodeDefinitionScope;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ConstantNodeModel extends NodeModel implements ISingleOutputPortNodeModel {
    public final static String OUTPUT_PORT_ID = "Output_0";
    @Getter
    private Constant constant;

    public ConstantNodeModel() {
        setCapability(Capabilities.COLORABLE, false);
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public PortModel getOutputPort() {
        return getOutputPortInfos().portsById.values().getFirst();
    }

    @Override
    public IGuiTexture getNodeIcon() {
        return IGuiTexture.EMPTY;
    }

    public void setConstant(@Nullable Constant constant) {
        if (this.constant == constant) return;
        // Unregister ourselves as the owner of the old constant.
        if (this.constant != null) {
            this.constant.setOwner(null);
        }
        this.constant = constant;
        if (this.constant != null) {
            this.constant.setOwner(this);
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
        getConstant().setValue(value);
    }

    public Type getType() {
        return constant.getType();
    }

    @Override
    protected void onDefineNode(NodeDefinitionScope<? extends NodeModel> scope) {
        scope.nodeModel.addOutputPort(OUTPUT_PORT_ID, getConstant().getTypeHandle(), PortType.DEFAULT, null, null);
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

    @Override
    public @Nullable GraphElement<?> createElementUI() {
        return new CapsuleNodeElement(this);
    }
}
