package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ICustomNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodePreviewModel;
import dev.vfyjxf.taffy.style.*;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * Renders a node's preview panel as a bottom panel inside the node. Content is supplied by the node
 * (custom nodes via {@link com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node#onBuildNodePreview}),
 * or by overriding {@link #buildPreviewContent(NodePreviewContext)} in a
 * {@link NodePreviewModel#createElementUI() custom element}. The panel hides itself when the preview
 * is collapsed ({@link NodePreviewModel#isExpanded()} is false) or the owning node is collapsed — it
 * is the single writer of its own display, so the parent node element must not drive it.
 */
public class NodePreviewElement extends GraphElement<NodePreviewModel> {
    public static final String COLLAPSED_CLASS = "__collapsed__";

    /** Header that stays visible while preview content is collapsed. */
    @Getter
    @Nullable
    protected UIElement header;
    /** Collapse/expand arrow. Its on-state means the preview content is collapsed. */
    @Getter
    @Nullable
    protected Toggle collapseToggle;
    /** Container that holds the node-supplied preview content. */
    @Getter
    @Nullable
    protected UIElement contentContainer;

    public NodePreviewElement(NodePreviewModel model) {
        super(model);
        addClass("__node-preview__");
    }

    @Override
    protected void buildUI() {
        super.buildUI();
        Style.defaultPipeline(getLayout(), l -> l.flexDirection(FlexDirection.COLUMN).paddingAll(2).gapAll(2).flexGrow(1));
        Style.defaultPipeline(getStyle(), s -> s.background(Sprites.RECT_SOLID));

        header = new UIElement().addClass("__node-preview_header__");
        Style.defaultPipeline(header.getLayout(), l -> l.alignItems(AlignItems.CENTER).justifyContent(AlignContent.CENTER).height(8));

        collapseToggle = new Toggle();
        collapseToggle.addClass("__node-preview_toggle__");
        collapseToggle.noText()
                .setOn(!getModel().isExpanded(), false)
                .setOnToggleChanged(collapsed -> {
                    var parent = getModel().getParentNode();
                    if (parent != null) {
                        parent.setPreviewExpanded(!collapsed);
                        applyPreviewState();
                    }
                })
                .toggleStyle(toggleStyle -> toggleStyle
                        .baseTexture(IGuiTexture.EMPTY)
                        .hoverTexture(IGuiTexture.EMPTY)
                        .markTexture(Icons.DOWN_ARROW_NO_BAR_S_LIGHT)
                        .unmarkTexture(Icons.UP_ARROW_NO_BAR_S_LIGHT));
        Style.defaultPipeline(collapseToggle.getLayout(), l -> l.width(8).height(8));
        header.addChild(collapseToggle);

        contentContainer = new UIElement().addClass("__node-preview_content__");
        Style.defaultPipeline(contentContainer.getLayout(), l -> l.flexDirection(FlexDirection.COLUMN).flexGrow(1));
        buildPreviewContent(makeContext(contentContainer));
        addChildren(contentContainer, header);
        applyPreviewState();
    }

    /**
     * Builds the {@link NodePreviewContext} for the given container. Centralizes the references so
     * build/update/rebuild all hand the node the same context shape.
     */
    protected NodePreviewContext makeContext(UIElement container) {
        return new NodePreviewContext(container, getModel().getParentNode(), getModel(), getGraphView(), this);
    }

    /**
     * Populates the preview content container. By default delegates to the owning custom node's
     * {@link com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node#onBuildNodePreview} hook.
     * Subclasses (returned from a custom {@link NodePreviewModel#createElementUI()}) may override
     * this to render fully bespoke content.
     */
    protected void buildPreviewContent(NodePreviewContext context) {
        var parent = context.nodeModel();
        if (parent instanceof ICustomNodeModel cn && cn.getNode() != null) {
            cn.getNode().onBuildNodePreview(context);
        }
    }

    /**
     * Clears and re-populates the content container. Called by {@link NodePreviewContext#rebuild()}
     * when the node needs to change the preview's structure (not just its drawn content).
     */
    public void rebuildContent() {
        if (contentContainer == null) return;
        contentContainer.clearAllChildren();
        buildPreviewContent(makeContext(contentContainer));
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        super.updateUIFromModel(visitor);
        applyPreviewState();

        // Let dynamic previews (e.g. a live shader) refresh when inputs/connections change.
        var parent = getModel().getParentNode();
        if (contentContainer != null
                && getModel().isExpanded()
                && (visitor.hasHint(ChangeHint.DATA) || visitor.hasHint(ChangeHint.GRAPH_TOPOLOGY))
                && parent instanceof ICustomNodeModel cn && cn.getNode() != null) {
            cn.getNode().onUpdateNodePreview(makeContext(contentContainer));
        }
    }

    protected void applyPreviewState() {
        var preview = getModel();
        var parent = preview.getParentNode();
        boolean parentCollapsed = parent != null && parent.isCollapsed();
        boolean previewCollapsed = !preview.isExpanded();
        Style.importantPipeline(getLayout(), l -> l.display(parentCollapsed ? TaffyDisplay.NONE : TaffyDisplay.FLEX));
        if (previewCollapsed) {
            addClass(COLLAPSED_CLASS);
        } else {
            removeClass(COLLAPSED_CLASS);
        }
        if (contentContainer != null) {
            Style.importantPipeline(contentContainer.getLayout(), l -> l.display(previewCollapsed ? TaffyDisplay.NONE : TaffyDisplay.FLEX));
        }
        if (collapseToggle != null) {
            collapseToggle.setOn(previewCollapsed, false);
        }
    }
}
