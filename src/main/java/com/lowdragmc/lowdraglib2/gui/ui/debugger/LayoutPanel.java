package com.lowdragmc.lowdraglib2.gui.ui.debugger;

import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import org.jetbrains.annotations.Nullable;

public class LayoutPanel extends UIElement {
    private UIElement container = new UIElement();
    private Label top = new Label();
    private Label bottom = new Label();
    private Label left = new Label();
    private Label right = new Label();
    private UIElement center = new UIElement();

    public LayoutPanel(int color) {
        getLayout().widthPercent(100).heightPercent(100);
        getStyle().background(SDFRectTexture.of(color).setRadius(4));
        addChild(container);

        container.getLayout()
                .widthPercent(100).heightPercent(100)
                .display(TaffyDisplay.GRID)
                .gridTemplateColumns("10px 1fr 10px")
                .gridTemplateRows("10px 1fr 10px");

        top.setText("-");
        top.getTextStyle().adaptiveWidth(true).adaptiveHeight(true).fontSize(4.5f);
        top.getLayout().gridRow("1").gridColumn("2").alignSelf(AlignItems.CENTER).justifySelf(AlignItems.CENTER);
        bottom.setText("-");
        bottom.getTextStyle().adaptiveWidth(true).adaptiveHeight(true).fontSize(4.5f);
        bottom.getLayout().gridRow("3").gridColumn("2").alignSelf(AlignItems.CENTER).justifySelf(AlignItems.CENTER);
        left.setText("-");
        left.getTextStyle().adaptiveWidth(true).adaptiveHeight(true).fontSize(4.5f);
        left.getLayout().gridRow("2").gridColumn("1").alignSelf(AlignItems.CENTER).justifySelf(AlignItems.CENTER);
        right.setText("-");
        right.getTextStyle().adaptiveWidth(true).adaptiveHeight(true).fontSize(4.5f);
        right.getLayout().gridRow("2").gridColumn("3").alignSelf(AlignItems.CENTER).justifySelf(AlignItems.CENTER);

        center.getLayout().gridRow("2").gridColumn("2");

        container.addChildren(top, bottom, left, right, center);
    }

    public LayoutPanel addCenter(UIElement element) {
        center.addChild(element);
        return this;
    }

    public void setValue(@Nullable FloatRect rect) {
        this.top.setText(rect == null ? "-" : String.format("%.1f", rect.top));
        this.bottom.setText(rect == null ? "-" : String.format("%.1f", rect.bottom));
        this.left.setText(rect == null ? "-" : String.format("%.1f", rect.left));
        this.right.setText(rect == null ? "-" : String.format("%.1f", rect.right));
    }
}
