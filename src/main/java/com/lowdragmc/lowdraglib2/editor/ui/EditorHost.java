package com.lowdragmc.lowdraglib2.editor.ui;

import org.jetbrains.annotations.Nullable;

/**
 * An element that a {@link View} can be found under and still belong to an editor.
 *
 * <p>A docked view reaches its editor by walking up the element tree, but a floated one does not:
 * its tree is rooted in a different {@code ModularUI} entirely. Rather than have {@code View} know
 * about every possible host, each host says which editor it stands in for.
 */
public interface EditorHost {

    /**
     * The editor views under this host belong to, or {@code null} if it has none.
     */
    @Nullable
    Editor getHostedEditor();
}
