package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.INodeOption;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.OptionVisibility;
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
    /** Which of the node body and the inspector draw this option's editor. */
    @Getter
    public final OptionVisibility visibility;
    /** @deprecated mirrors {@link #visibility}, kept so code that read the old field still compiles. */
    @Deprecated(since = "1.22")
    public final boolean showInInspectorOnly;
    @Getter
    public final int order;

    public NodeOption(String name, PortModel portModel, OptionVisibility visibility, int order) {
        this.id = name;
        this.portModel = portModel;
        this.visibility = visibility;
        this.showInInspectorOnly = visibility == OptionVisibility.INSPECTOR_ONLY;
        this.order = order;
    }

    /**
     * @deprecated visibility is no longer a single flag, use
     *             {@link #NodeOption(String, PortModel, OptionVisibility, int)}.
     */
    @Deprecated(since = "1.22")
    public NodeOption(String name, PortModel portModel, boolean showInInspectorOnly, int order) {
        this(name, portModel, showInInspectorOnly
                ? OptionVisibility.INSPECTOR_ONLY
                : OptionVisibility.NODE_AND_INSPECTOR, order);
    }

    /** @deprecated use {@link #getVisibility()}. */
    @Deprecated(since = "1.22")
    public boolean isShowInInspectorOnly() {
        return visibility == OptionVisibility.INSPECTOR_ONLY;
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
