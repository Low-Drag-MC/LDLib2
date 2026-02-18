package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Concrete implementation of {@link IPortDefinitionContext}.
 *
 * <p>Provides methods to define input and output ports on a node during node definition.</p>
 */
public class PortDefinitionContext implements IPortDefinitionContext {
    // runtime
    @Getter
    @Setter
    private NodeDefinitionScope<?> scope;
    private final List<PortBuilder> pool = new ArrayList<>();
    private final List<PortBuilder> active = new ArrayList<>();

    public PortBuilder getFreeBuilder() {
        PortBuilder builder;
        if (pool.isEmpty()) {
            builder = new PortBuilder();
        } else {
            builder = pool.removeLast();
        }
        active.add(builder);
        return builder;
    }

    public void freeBuilder(PortBuilder builder) {
        if (builder == null) return;
        if (!active.remove(builder)) return;
        pool.add(builder);
        builder.reset();
    }


    public void finish() {
        while (!active.isEmpty()) {
            active.getFirst().build();
        }
    }

    @Override
    public PortBuilder addInputPort(String portId, TypeHandle typeHandle) {
        return getFreeBuilder().addInputPort(this, portId, typeHandle);
    }

    @Override
    public PortBuilder addOutputPort(String portId, TypeHandle typeHandle) {
        return getFreeBuilder().addOutputPort(this, portId, typeHandle);
    }
}
