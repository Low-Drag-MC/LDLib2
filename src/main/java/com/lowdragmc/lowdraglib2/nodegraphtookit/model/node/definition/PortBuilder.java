package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.*;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.ITypeConfigurable;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModelImpl;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.function.Consumer;

public class PortBuilder implements IInputPortBuilder<PortBuilder>, IOutputPortBuilder<PortBuilder> {
    // runtime
    protected PortDefinitionContext context = null;
    protected String portId;
    protected Component displayName;
    protected TypeHandle dataType;
    protected PortDirection portDirection = PortDirection.NONE;
    protected PortOrientation portOrientation = PortOrientation.Horizontal;
    protected PortConnectorUI connectorUI = PortConnectorUI.DEFAULT;
    protected Object defaultValue;
    @Nullable
    protected ITypeConfigurable customTypeConfigurable;
    @Nullable
    protected Field valueField;
    @Nullable
    protected Object valueOwer;

    public void reset() {
        portId = null;
        displayName = null;
        dataType = null;
        portDirection = PortDirection.NONE;
        portOrientation = PortOrientation.Horizontal;
        connectorUI = PortConnectorUI.DEFAULT;
        defaultValue = null;
        customTypeConfigurable = null;
        valueField = null;
        valueOwer = null;
    }

    public PortBuilder addInputPort(PortDefinitionContext context, String portId, TypeHandle typeHandle) {
        this.context = context;
        this.portId = portId;
        this.dataType = typeHandle;
        this.portDirection = PortDirection.INPUT;
        return this;
    }

    public PortBuilder addOutputPort(PortDefinitionContext context, String portId, TypeHandle typeHandle) {
        this.context = context;
        this.portId = portId;
        this.dataType = typeHandle;
        this.portDirection = PortDirection.OUTPUT;
        return this;
    }

    @Override
    public PortBuilder withDisplayName(Component displayName) {
        this.displayName = displayName;
        return this;
    }

    @Override
    public PortBuilder withConnectorUI(PortConnectorUI connectorUI) {
        this.connectorUI = connectorUI;
        return null;
    }

    @Override
    public PortBuilder withDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
        return null;
    }

    @Override
    public PortBuilder withConfigurable(ITypeConfigurable configurable) {
        this.customTypeConfigurable = configurable;
        return this;
    }

    @Override
    public PortBuilder withFieldContext(Field field, Object owner) {
        this.valueField = field;
        this.valueOwer = owner;
        return this;
    }

    @Override
    public PortModel build() {
        if (context == null) throw new IllegalStateException("Option definition context is not set.");

        PortModel result;
        var nodeModel = context.getScope().nodeModel;
        if (portDirection == PortDirection.INPUT) {
            Consumer<Constant> initializationCallback = null;
            if (defaultValue != null) {
                initializationCallback = constant -> {
                    constant.setDefaultValue(defaultValue);
                    constant.setValue(defaultValue);
                };
            }
            result = nodeModel.addInputPort(portId, dataType, null,
                    portOrientation, null, initializationCallback, null);
        } else {
            result = nodeModel.addOutputPort(portId, dataType, null,
                    portOrientation, null);
        }
        if (displayName != null) {
            result.setTitle(displayName);
        }
        if (result instanceof PortModelImpl portModelImpl) {
            portModelImpl.setConnectorUI(connectorUI);
        }
        // Reapply configurator overrides every build — PortModel instances can be reused across
        // defineNode passes, so we must overwrite (including with null) to avoid inheriting a
        // stale override from a previous definition.
        if (portDirection == PortDirection.INPUT) {
            result.setCustomTypeConfigurable(customTypeConfigurable);
            result.setValueField(valueField);
            result.setValueOwer(valueOwer);
        }
        context.freeBuilder(this);
        return result;
    }

}
