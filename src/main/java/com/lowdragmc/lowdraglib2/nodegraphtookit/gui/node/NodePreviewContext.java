package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodePreviewModel;
import org.jetbrains.annotations.Nullable;

/**
 * Context handed to a node when it builds or updates its preview panel
 * ({@link com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node#onBuildNodePreview} /
 * {@link com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node#onUpdateNodePreview}).
 *
 * <p>Gives the node everything it needs to render a meaningful preview:
 * <ul>
 *   <li>{@link #container()} — the UI element to add preview content into.</li>
 *   <li>{@link #nodeModel()} — the owning node model, for reading port values, options,
 *       connected wires, etc.</li>
 *   <li>{@link #previewModel()} — the preview model (e.g. its expanded state).</li>
 *   <li>{@link #graphView()} — the live graph view (history stack, command dispatch); may be
 *       {@code null} outside an interactive editor.</li>
 *   <li>{@link #element()} — the preview UI element, for requesting a full content {@link #rebuild()}.</li>
 * </ul>
 */
public record NodePreviewContext(
        UIElement container,
        AbstractNodeModel nodeModel,
        NodePreviewModel previewModel,
        @Nullable GraphView graphView,
        NodePreviewElement element) {

    /**
     * Clears the content container and asks the node to rebuild it from scratch. Use when the
     * preview's structure (not just its drawn content) needs to change in response to a model edit.
     */
    public void rebuild() {
        element.rebuildContent();
    }
}
