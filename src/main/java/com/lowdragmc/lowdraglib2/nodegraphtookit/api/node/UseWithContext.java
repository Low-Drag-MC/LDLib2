package com.lowdragmc.lowdraglib2.nodegraphtookit.api.node;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares which {@link ContextNode} types a {@link BlockNode} subclass is compatible with.
 *
 * <p>Used by the default implementation of {@code ContextNode.getSupportBlocks()} to
 * auto-discover compatible block types from the graph's registered node classes. A context
 * may also override {@code getSupportBlocks()} explicitly to override or extend this list.</p>
 *
 * <p>A {@code BlockNode} subclass without this annotation is treated as compatible with
 * no contexts (unless a context explicitly opts it in via {@code getSupportBlocks()}).</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface UseWithContext {
    Class<? extends ContextNode>[] value();
}
