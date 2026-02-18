package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.ModelElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;

public class NodeTitleElement extends ModelElement {
    public final AbstractNodeModel nodeModel;
    @Getter
    protected UIElement colorLine;
    @Getter
    protected UIElement nodeIcon;
    @Getter
    protected Label nodeTittle;

    public NodeTitleElement(AbstractNodeModel nodeModel) {
        this.nodeModel = nodeModel;
    }

    @Override
    protected void buildUI() {
        setId("node-title-bar").setOverflowVisible(false);
        getStyle().background(Sprites.BORDER_DARK);
        getLayout().paddingVertical(3).paddingHorizontal(4);

        colorLine = new UIElement().setId("node-title-color-line");
        colorLine.getLayout().height(2).widthPercent(100).positionType(TaffyPosition.ABSOLUTE);

        var titleContainer = new UIElement();
        titleContainer.getLayout().alignItems(AlignItems.CENTER).minWidthAuto().minHeightAuto()
                .gapAll(2).flexDirection(FlexDirection.ROW);

        this.nodeIcon = new UIElement().setId("node-title-icon");
        this.nodeIcon.getLayout().aspectRatio(1).width(10);

        this.nodeTittle = new Label();
        this.nodeTittle.setId("node-title");
        this.nodeTittle.getTextStyle().adaptiveWidth(true).adaptiveHeight(true);

        titleContainer.addChildren(nodeIcon, nodeTittle);

        addChildren(colorLine, titleContainer);
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
        colorLine.setVisible(updateLineColorFromModel(visitor));
    }

    protected boolean updateLineColorFromModel(ModelUpdateVisitor visitor) {
        if (colorLine == null)
            return false;

        if (visitor.hasHint(ChangeHint.STYLE)) {
            if (nodeModel.hasUserColor()) {
                colorLine.getStyle().background(new ColorRectTexture(nodeModel.getElementColor()));
                return true;
            }

            colorLine.getStyle().background(new ColorRectTexture(0x00000000));
        }

        return false;
    }
}
