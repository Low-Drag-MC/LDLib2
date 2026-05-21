package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * Title bar variant that appends a right-aligned collapse arrow after the title label.
 * Only meaningful when the model has the {@code COLLAPSIBLE} capability; if the capability is
 * absent the toggle is not created and the title behaves identically to the base class.
 *
 * <p>Used by {@link CollapsibleInOutNodeElement}. Other node element kinds keep the lighter
 * {@link NodeTitleElement} without the toggle/spacer overhead.</p>
 */
public class CollapsibleNodeTitleElement extends NodeTitleElement {
    /** Flexible spacer that grows to push {@link #collapseToggle} to the right edge of {@link #titleContainer}. */
    @Getter
    @Nullable
    protected UIElement titleSpacer;
    /** Collapse arrow on the right edge of the title. Null when the node lacks {@code COLLAPSIBLE}. */
    @Getter
    @Nullable
    protected Toggle collapseToggle;

    public CollapsibleNodeTitleElement(AbstractNodeModel nodeModel) {
        super(nodeModel);
        addClass("__collapsible-node-title-element__");
    }

    @Override
    protected void buildUI() {
        super.buildUI();
        if (!nodeModel.isCollapsible()) return;

        // Flex spacer pushes the toggle to the trailing edge of the title row.
        titleSpacer = new UIElement().addClass("__node-title_spacer__");
        Style.defaultPipeline(titleSpacer.getLayout(), layout -> layout.flex(1).height(1));

        // Pointing DOWN means expanded (clicking will collapse); pointing RIGHT means collapsed.
        collapseToggle = new Toggle();
        collapseToggle.addClass("__node-title_collapse-toggle__");
        collapseToggle.noText()
                .setOn(nodeModel.isCollapsed(), false)
                .setOnToggleChanged(nodeModel::setCollapsed)
                .toggleStyle(toggleStyle -> toggleStyle
                        .baseTexture(IGuiTexture.EMPTY)
                        .hoverTexture(IGuiTexture.EMPTY)
                        .markTexture(Icons.RIGHT_ARROW_NO_BAR_S_LIGHT)
                        .unmarkTexture(Icons.DOWN_ARROW_NO_BAR_S_LIGHT));
        Style.defaultPipeline(collapseToggle.getLayout(), layout -> layout.width(8).height(8));

        titleContainer.addChildren(titleSpacer, collapseToggle);
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        super.updateUIFromModel(visitor);
        if (collapseToggle != null && visitor.hasHint(ChangeHint.LAYOUT)) {
            // Keep toggle visual in sync with the model on external state changes (context menu,
            // undo, etc.). setOn short-circuits when value matches, so this is cheap.
            collapseToggle.setOn(nodeModel.isCollapsed(), false);
        }
    }
}