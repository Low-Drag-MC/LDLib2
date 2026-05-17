package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.ModelElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.ElementRenameColorCommands;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.IHasName;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import org.lwjgl.glfw.GLFW;

public class NodeTitleElement extends ModelElement {
    public final AbstractNodeModel nodeModel;
    @Getter
    protected UIElement titleContainer;
    @Getter
    protected UIElement colorLine;
    @Getter
    protected UIElement nodeIcon;
    @Getter
    protected Label nodeTittle;
    /** Inline edit field shown in place of {@link #nodeTittle} during rename. Null when idle. */
    protected TextField inlineRenameField;

    public NodeTitleElement(AbstractNodeModel nodeModel) {
        this.nodeModel = nodeModel;
    }

    @Override
    protected void buildUI() {
        setId("node-title-bar").setOverflowVisible(false);
        getStyle().background(Sprites.BORDER_DARK);
        getLayout().paddingVertical(3).paddingHorizontal(4);

        colorLine = new UIElement().setId("node-title-color-line");
        colorLine.getLayout().height(2).widthPercent(100).marginBottom(2);

        titleContainer = new UIElement();
        titleContainer.getLayout().alignItems(AlignItems.CENTER).minWidthAuto().minHeightAuto()
                .gapAll(2).flexDirection(FlexDirection.ROW);

        this.nodeIcon = new UIElement().setId("node-title-icon");
        this.nodeIcon.getLayout().aspectRatio(1).width(10);

        this.nodeTittle = new Label();
        this.nodeTittle.setId("node-title");
        this.nodeTittle.getTextStyle().adaptiveWidth(true).adaptiveHeight(true);

        titleContainer.addChildren(nodeIcon, nodeTittle);

        addChildren(colorLine, titleContainer);

        // Double-click the title label to inline-rename, but only when the model says it's
        // renamable AND exposes an IHasName setter. stopPropagation prevents the NodeElement's
        // outer DOUBLE_CLICK listener (e.g. SubgraphNodeModel enter-subgraph) from firing.
        if (nodeModel.isRenamable()) {
            nodeTittle.addEventListener(UIEvents.DOUBLE_CLICK, event -> {
                startInlineRename();
                event.stopPropagation();
            });
        }
    }

    /**
     * Replaces the title label with a text field for inline editing. Pressing Enter commits via
     * {@link ElementRenameColorCommands.RenameElementCommand}; pressing Escape cancels; losing
     * focus commits (matching common desktop editor behavior). Safe to call multiple times — only
     * the first invocation has an effect until the field closes.
     */
    public void startInlineRename() {
        if (inlineRenameField != null) return;
        if (!(nodeModel instanceof IHasName named) || !nodeModel.isRenamable()) return;

        var initial = named.getName();
        nodeTittle.setDisplay(false);
        inlineRenameField = new TextField();
        inlineRenameField.setText(initial == null ? "" : initial);
        inlineRenameField.layout(layout -> layout.minWidth(40));
        inlineRenameField.setId("node-title-rename");

        // Use an array-wrapped boolean to ensure commit-or-cancel runs exactly once. Both ENTER
        // and BLUR will fire; whichever comes first wins.
        final boolean[] done = {false};
        Runnable commit = () -> {
            if (done[0]) return;
            done[0] = true;
            var newName = inlineRenameField.getValue();
            var graphView = getFirstAncestorOfType(GraphView.class);
            if (newName != null && !newName.equals(initial)) {
                if (graphView != null) {
                    graphView.dispatchCommand(new ElementRenameColorCommands.RenameElementCommand(nodeModel, newName));
                } else {
                    named.setName(newName);
                }
            }
            endInlineRename();
        };
        Runnable cancel = () -> {
            if (done[0]) return;
            done[0] = true;
            endInlineRename();
        };

        inlineRenameField.addEventListener(UIEvents.KEY_DOWN, e -> {
            if (e.keyCode == GLFW.GLFW_KEY_ENTER || e.keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                commit.run();
                e.stopPropagation();
            } else if (e.keyCode == GLFW.GLFW_KEY_ESCAPE) {
                cancel.run();
                e.stopPropagation();
            }
        });
        inlineRenameField.addEventListener(UIEvents.BLUR, e -> commit.run());

        nodeTittle.getParent().addChild(inlineRenameField);
        inlineRenameField.focus();
    }

    private void endInlineRename() {
        if (inlineRenameField != null) {
            inlineRenameField.removeSelf();
            inlineRenameField = null;
        }
        nodeTittle.setDisplay(true);
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        super.updateUIFromModel(visitor);
        if (visitor.hasHint(ChangeHint.STYLE) || visitor.hasHint(ChangeHint.DATA)) {
            // title
            nodeTittle.setText(nodeModel.getTitle());
        }
        if (visitor.hasHint(ChangeHint.STYLE)) {
            // icon
            var icon = nodeModel.getNodeIcon();
            nodeIcon.setDisplay(icon != null && icon != IGuiTexture.EMPTY);
            nodeIcon.getStyle().background(icon);
            // tooltip
            nodeTittle.getStyle().tooltips(nodeModel.getTooltip());
        }
        updateLineColorFromModel(visitor);
    }

    protected void updateLineColorFromModel(ModelUpdateVisitor visitor) {
        if (colorLine == null) return;

        if (visitor.hasHint(ChangeHint.STYLE)) {
            var color = nodeModel.getElementColor();
            colorLine.getStyle().background(new ColorRectTexture(color));
            colorLine.setDisplay(ColorUtils.alpha(color) > 0.01f);
        }
    }
}
