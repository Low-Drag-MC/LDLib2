package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.INodeOption;
import com.mojang.serialization.DataResult;
import lombok.Getter;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Type;

/**
 * Concrete implementation of {@link INodeOption}.
 *
 * <p>Represents a configurable option on a node, such as a dropdown, text field, or checkbox.</p>
 */
public class NodeOption implements INodeOption {
    public static final String PORT_ID_PREFIX = "option_";
    @Getter
    public final String id;
    @Getter
    public final PortModel portModel;
    @Getter
    public final boolean showInInspectorOnly;
    @Getter
    public final int order;

    public NodeOption(String name, PortModel portModel, boolean showInInspectorOnly, int order) {
        this.id = name;
        this.portModel = portModel;
        this.showInInspectorOnly = showInInspectorOnly;
        this.order = order;
    }

    @Override
    public Type getDataType() {
        return null;
    }

    @Override
    public Component getDisplayName() {
        return portModel.getDisplayName();
    }

    @Override
    public <T> DataResult<T> tryGetValue(Type expectedType) {
        var embeddedValue = portModel.getEmbeddedValue();
        if (embeddedValue == null) return DataResult.error(() -> "Cannot get value of option " + id + " as it has no embedded value.");
        return embeddedValue.tryGetValue(expectedType);
    }
}
