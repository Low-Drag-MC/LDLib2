package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
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
import dev.vfyjxf.taffy.style.TaffyDisplay;
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
        addClass("__node-title-element__");
    }

    @Override
    protected void buildUI() {
        Style.defaultPipeline(getStyle(), style -> style.background(Sprites.BORDER_DARK).overflowVisible(false));
        Style.defaultPipeline(getLayout(), layout -> layout.paddingVertical(3).paddingHorizontal(4));

        colorLine = new UIElement().addClass("__node-title_color-line__");
        Style.defaultPipeline(colorLine.getLayout(), layout -> layout.height(2).widthPercent(100).marginBottom(2));

        titleContainer = new UIElement().addClass("__node-title_title-container__");
        Style.defaultPipeline(titleContainer.getLayout(), layout -> layout.alignItems(AlignItems.CENTER).minWidthAuto().minHeightAuto()
                .gapAll(2).flexDirection(FlexDirection.ROW));

        this.nodeIcon = new UIElement().addClass("__node-title-icon__");
        Style.defaultPipeline(nodeIcon.getLayout(), layout -> layout.aspectRatio(1).width(10));

        this.nodeTittle = new Label();
        this.nodeTittle.addClass("__node-title__");
        Style.defaultPipeline(this.nodeTittle.getTextStyle(), style -> style.adaptiveWidth(true).adaptiveHeight(true));

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
        Style.importantPipeline(nodeTittle.getLayout(), layout -> layout.display(TaffyDisplay.NONE));
        inlineRenameField = new TextField();
        inlineRenameField.setText(initial == null ? "" : initial);
        Style.defaultPipeline(inlineRenameField.getLayout(), layout -> layout.minWidth(40));
        inlineRenameField.addClass("__node-title-rename__");

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
        // remove display:none {important} from title label
        Style.importantPipeline(nodeTittle.getLayoutStyle(), layout -> layout.display((TaffyDisplay) null));
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
            Style.importantPipeline(nodeIcon.getLayout(), layout -> layout.display(icon != null && icon != IGuiTexture.EMPTY ? TaffyDisplay.FLEX : TaffyDisplay.NONE));
            Style.importantPipeline(nodeIcon.getStyle(), style -> style.background(icon));
            // tooltip
            Style.importantPipeline(nodeTittle.getStyle(), style -> style.tooltips(nodeModel.getTooltip()));
        }
        updateLineColorFromModel(visitor);
    }

    protected void updateLineColorFromModel(ModelUpdateVisitor visitor) {
        if (colorLine == null) return;

        if (visitor.hasHint(ChangeHint.STYLE)) {
            var color = nodeModel.getElementColor();
            Style.importantPipeline(colorLine.getStyle(), style -> style.background(new ColorRectTexture(color)));
            Style.importantPipeline(colorLine.getLayout(), layout -> layout.display(ColorUtils.alpha(color) > 0.01f ? TaffyDisplay.FLEX : TaffyDisplay.NONE));
        }
    }
}
