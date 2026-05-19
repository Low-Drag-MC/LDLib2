package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition;

import com.lowdragmc.lowdraglib2.gui.ui.data.Tooltips;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.*;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.ITypeConfigurable;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
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

    /**
     * Creates a new option builder.
     *
     * @param nodeModel the node model that will own the option
     * @param name      the unique name of the option
     * @param dataType  the data type of the option
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

    @Override
    public INodeOption build() {
        if (context == null) throw new IllegalStateException("Option definition context is not set.");

        Consumer<Constant> initializationCallback = null;
        if (defaultValue != null) {
            initializationCallback = constant -> {
                constant.setDefaultValue(defaultValue);
                constant.setValue(defaultValue);
            };
        }
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
        context.freeBuilder(this);
        return result;
    }
}
