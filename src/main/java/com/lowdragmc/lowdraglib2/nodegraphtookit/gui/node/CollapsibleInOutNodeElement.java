package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortNodeModel;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

public class CollapsibleInOutNodeElement extends NodeElement {
    /** CSS class applied while the node is collapsed — exposed for stylesheet rules. */
    public static final String COLLAPSED_CLASS = "__collapsed__";

    /** Row of vertical input ports, rendered above the title. */
    @Getter
    protected @Nullable VerticalPortContainerElement topPortContainer;
    /** Row of vertical output ports, rendered below the node body. */
    @Getter
    protected @Nullable VerticalPortContainerElement bottomPortContainer;

    public CollapsibleInOutNodeElement(AbstractNodeModel nodeModel) {
        super(nodeModel);
        addClass("__collapsible-in-out-node__");
    }

    @Override
    protected void buildPartList() {
        parts.add(this.nodeTittle = new CollapsibleNodeTitleElement(getModel()));
        if (getModel() instanceof NodeModel nodeModel) {
            parts.add(this.nodeOptionContainer = new NodeOptionsInspector(nodeModel));
        }
        if (getModel() instanceof PortNodeModel portNodeNode) {
            // Vertical input ports go above the title, vertical output ports below the body; the
            // horizontal in/out ports stay in the middle as before.
            parts.add(this.topPortContainer = new VerticalPortContainerElement(portNodeNode,
                    PortContainerElement.VERTICAL_PORT_FILTER.and(PortContainerElement.INPUT_PORT_FILTER)));
            parts.add(this.portContainerElement = new InOutPortContainerElement(portNodeNode, PortContainerElement.HORIZONTAL_PORT_FILTER));
            parts.add(this.bottomPortContainer = new VerticalPortContainerElement(portNodeNode,
                    PortContainerElement.VERTICAL_PORT_FILTER.and(PortContainerElement.OUTPUT_PORT_FILTER)));
        }
        // This subclass doesn't call super.buildPartList(), so build the preview part explicitly
        // (the base NodeElement adds it for the non-collapsible path).
        buildPreviewPart();
    }

    @Override
    protected void buildUI() {
        // NodeElement.buildUI adds [title, options, horizontal-ports, preview?]; wrap that with the
        // vertical input row at the very top and the vertical output row at the very bottom — below
        // the preview panel as well.
        super.buildUI();
        addChildAt(topPortContainer, 0);
        addChild(bottomPortContainer);
        applyCollapsedState(getModel().isCollapsed());
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        super.updateUIFromModel(visitor);
        if (visitor.hasHint(ChangeHint.LAYOUT)) {
            applyCollapsedState(getModel().isCollapsed());
        }
    }

    /**
     * Hides the options and ports while keeping the title bar visible. Stylesheets can hook into
     * {@link #COLLAPSED_CLASS} for additional theming (e.g. rounded bottom corners on the title).
     */
    protected void applyCollapsedState(boolean collapsed) {
        if (collapsed) {
            addClass(COLLAPSED_CLASS);
        } else {
            removeClass(COLLAPSED_CLASS);
        }
        // NOTE: the options panel and the vertical port rows hide themselves — they are collapse-aware
        // single writers of their own display (they update after this method, so driving them here
        // would be overwritten). The vertical rows also self-hide when they have no ports. We only
        // manage the horizontal port container, which never rewrites its own display.
        var display = collapsed ? TaffyDisplay.NONE : TaffyDisplay.FLEX;
        if (portContainerElement != null) {
            Style.importantPipeline(portContainerElement.getLayout(), l -> l.display(display));
        }
    }
}
