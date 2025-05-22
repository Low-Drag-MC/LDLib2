package com.lowdragmc.lowdraglib.editor.ui;

import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import lombok.Getter;
import org.appliedenergistics.yoga.YogaFlexDirection;

@Getter
public class Editor extends UIElement {
    public final UIElement top;
    public final Window left;
    public final Window right;
    public final Window center;
    public final Window bottom;

    public Editor() {
        this.top = new UIElement();
        this.left = new Window();
        this.right = new Window();
        this.center = new Window();
        this.bottom = new Window();

        left.layout(layout -> {
            layout.setWidthPercent(28);
            layout.setHeightPercent(100);
        });
        center.layout(layout -> {
            layout.setFlexGrow(1);
            layout.setHeightPercent(100);
        });
        right.layout(layout -> {
            layout.setWidthPercent(20);
            layout.setHeightPercent(100);
        });
        bottom.layout(layout -> {
            layout.setHeightPercent(25);
            layout.setWidthPercent(100);
        });

        addChildren(top.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeight(30);
        }), new UIElement().layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setWidthPercent(100);
            layout.setFlexGrow(1);
        }).addChildren(new UIElement().layout(layout -> {
            layout.setFlexGrow(1);
            layout.setHeightPercent(100);
        }).addChildren(new UIElement().layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setFlexGrow(1);
            layout.setWidthPercent(100);
        }).addChildren(left, center), bottom), right));
    }
}
