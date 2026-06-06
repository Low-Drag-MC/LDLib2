package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.*;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.ITypeConfigurable;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModelImpl;
import com.mojang.serialization.Codec;
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
    @Nullable
    protected Codec<?> customCodec;
    protected boolean noSerialization;
    protected boolean noConfigurator;

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
        customCodec = null;
        noSerialization = false;
        noConfigurator = false;
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
        return this;
    }

    @Override
    public PortBuilder withOrientation(PortOrientation orientation) {
        this.portOrientation = orientation;
        return this;
    }

    @Override
    public PortBuilder withDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
        return this;
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

    /** Mirror of {@code OptionBuilder.buildInitializationCallback} — see that for the rationale. */
    @Nullable
    private Consumer<Constant> buildInitializationCallback() {
        if (defaultValue == null && customCodec == null && !noSerialization) return null;
        var snapshotDefault = defaultValue;
        var snapshotCodec = customCodec;
        var snapshotSerializeDisabled = noSerialization;
        return constant -> {
            if (snapshotDefault != null) {
                constant.setDefaultValue(snapshotDefault);
                constant.setValue(snapshotDefault);
            }
            constant.setCustomCodec(snapshotCodec);
            constant.setSerializationEnabled(!snapshotSerializeDisabled);
        };
    }

    @Override
    public PortBuilder withCodec(Codec<?> codec) {
        this.customCodec = codec;
        return this;
    }

    @Override
    public PortBuilder withoutSerialization() {
        this.noSerialization = true;
        return this;
    }

    @Override
    public PortBuilder withoutConfigurator() {
        this.noConfigurator = true;
        return this;
    }

    @Override
    public PortModel build() {
        if (context == null) throw new IllegalStateException("Option definition context is not set.");

        PortModel result;
        var nodeModel = context.getScope().nodeModel;
        if (portDirection == PortDirection.INPUT) {
            // Only generate a callback when the builder actually has state to apply. For the
            // common "no defaultValue, no codec, no withoutSerialization" case Phase 1 already
            // produced the correct constant — Phase 2's reset-and-redecode would be redundant.
            Consumer<Constant> initializationCallback = buildInitializationCallback();
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
        // Configurator opt-out applies to both directions — output ports also descend from
        // IFieldConstantConfigurable via PortModel and should be silenced if requested.
        result.setConfiguratorEnabled(!noConfigurator);
        context.freeBuilder(this);
        return result;
    }

}
