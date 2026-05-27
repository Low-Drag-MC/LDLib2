package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition;

import com.lowdragmc.lowdraglib2.gui.ui.data.Tooltips;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.*;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.ITypeConfigurable;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.function.Consumer;

/**
 * Concrete implementation of option builder.
 *
 * <p>Used to create and configure node options using a fluent builder pattern.</p>
 */
public class OptionBuilder implements IOptionBuilder<OptionBuilder> {
    protected OptionDefinitionContext context;
    protected String optionId;
    protected Component displayName;
    protected TypeHandle dataType;
    protected @Nullable Tooltips tooltip;
    protected boolean showInInspectorOnly;
    protected int order;
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

    /**
     * Creates a new option builder.
     */
    public OptionBuilder() {}

    public void reset() {
        optionId = null;
        displayName = null;
        dataType = null;
        tooltip = null;
        showInInspectorOnly = false;
        order = 0;
        defaultValue = null;
        customTypeConfigurable = null;
        valueField = null;
        valueOwer = null;
        customCodec = null;
        noSerialization = false;
        noConfigurator = false;
    }

    public OptionBuilder addOption(OptionDefinitionContext context, String optionId, TypeHandle dataType) {
        this.context = context;
        this.optionId = optionId;
        this.dataType = dataType;
        return this;
    }

    @Override
    public OptionBuilder withDisplayName(Component displayName) {
        this.displayName = displayName;
        return this;
    }

    @Override
    public OptionBuilder withTooltips(Tooltips tooltips) {
        this.tooltip = tooltips;
        return this;
    }

    @Override
    public OptionBuilder withDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    @Override
    public OptionBuilder showInInspectorOnly() {
        this.showInInspectorOnly = true;
        return this;
    }

    @Override
    public OptionBuilder withConfigurable(ITypeConfigurable configurable) {
        this.customTypeConfigurable = configurable;
        return this;
    }

    @Override
    public OptionBuilder withFieldContext(Field field, Object owner) {
        this.valueField = field;
        this.valueOwer = owner;
        return this;
    }

    /**
     * Returns a callback to install builder-side Constant state, or {@code null} if the builder
     * has no state worth applying — in which case Phase 1 of deserialization already produced
     * the correct constant and {@link com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel}'s
     * reuse path skips the redundant Phase 2.
     */
    @Nullable
    private Consumer<Constant> buildInitializationCallback() {
        if (defaultValue == null && customCodec == null && !noSerialization) return null;
        // Snapshot fields so the lambda isn't bound to the live builder (reset() runs after
        // build() returns and would clear them otherwise).
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
    public OptionBuilder withCodec(Codec<?> codec) {
        this.customCodec = codec;
        return this;
    }

    @Override
    public OptionBuilder withoutSerialization() {
        this.noSerialization = true;
        return this;
    }

    @Override
    public OptionBuilder withoutConfigurator() {
        this.noConfigurator = true;
        return this;
    }

    @Override
    public INodeOption build() {
        if (context == null) throw new IllegalStateException("Option definition context is not set.");

        // Only generate a callback when the builder actually has state to apply. For the common
        // "no defaultValue, no codec, no withoutSerialization" case Phase 1 already produced the
        // correct constant — Phase 2's reset-and-redecode would be redundant work.
        Consumer<Constant> initializationCallback = buildInitializationCallback();
        var nodeModel = context.getScope().nodeModel;
        var result = nodeModel.addNodeOption(optionId, dataType, tooltip, showInInspectorOnly, order, initializationCallback, __ -> {
            // schedule defines while the option value changed
            if (!nodeModel.isCurrentlyDefiningNode()) {
                nodeModel.defineNode();
            }
        });
        if (displayName != null) {
            result.getPortModel().setTitle(displayName);
        }
        // Reapply configurator overrides every build — the underlying PortModel may be reused
        // across defineNode passes, so overwrite (including with null) to avoid inheriting stale
        // state from a previous definition.
        var portModel = result.getPortModel();
        portModel.setCustomTypeConfigurable(customTypeConfigurable);
        portModel.setValueField(valueField);
        portModel.setValueOwer(valueOwer);
        portModel.setConfiguratorEnabled(!noConfigurator);
        context.freeBuilder(this);
        return result;
    }
}
