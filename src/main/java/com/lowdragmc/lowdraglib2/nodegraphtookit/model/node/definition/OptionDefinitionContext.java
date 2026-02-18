package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import lombok.Getter;
import lombok.Setter;


import java.util.*;

/**
 * Concrete implementation of {@link IOptionDefinitionContext}.
 *
 * <p>Provides methods to define node options during node definition.</p>
 */
public class OptionDefinitionContext implements IOptionDefinitionContext {
    // runtime
    @Getter @Setter
    private NodeDefinitionScope<?> scope;
    private final List<OptionBuilder> pool = new ArrayList<>();
    private final List<OptionBuilder> active = new ArrayList<>();

    public OptionBuilder getFreeBuilder() {
        OptionBuilder builder;
        if (pool.isEmpty()) {
            builder = new OptionBuilder();
        } else {
            builder = pool.removeLast();
        }
        active.add(builder);
        return builder;
    }

    public void freeBuilder(OptionBuilder builder) {
        if (builder == null) return;
        if (!active.remove(builder)) return;
        pool.add(builder);
        builder.reset();
    }

    @Override
    public OptionBuilder addOption(String name, TypeHandle typeHandle) {
        return getFreeBuilder().addOption(this, name, typeHandle);
    }

    public void finish() {
        while (!active.isEmpty()) {
            active.getFirst().build();
        }
    }
}
