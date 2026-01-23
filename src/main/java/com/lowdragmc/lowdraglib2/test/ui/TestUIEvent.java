package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SplitView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector4f;

import java.util.concurrent.atomic.AtomicBoolean;

@LDLRegisterClient(name="ui_event", registry = "ldlib2:screen_test")
@NoArgsConstructor
public class TestUIEvent implements IScreenTest {
    @Override
    public ModularUI createUI(Player entityPlayer) {
        var root = new UIElement().addClass("panel_bg");

        var left = new UIElement().layout(layout -> layout.flex(1).heightPercent(100))
                .setOverflowVisible(false)
                .layout(layout -> layout.gapAll(2).wrap(FlexWrap.WRAP).flexDirection(FlexDirection.ROW))
                .addClass("preview_bg");
        var right = new UIElement().layout(layout -> layout.flex(1).heightPercent(100))
                .setOverflowVisible(false)
                .layout(layout -> layout.gapAll(2).wrap(FlexWrap.WRAP).flexDirection(FlexDirection.ROW))
                .addClass("preview_bg");

        var target1 = createTarget(ColorPattern.T_RED.color, left, right);
        var target2 = createTarget(ColorPattern.T_GREEN.color, left, right);
        var target3 = createTarget(ColorPattern.T_BRIGHT_RED.color, left, right);
        var target4 = createTarget(ColorPattern.T_CYAN.color, left, right);

        left.addChildren(target1, target2);
        right.addChildren(target3, target4);

        root.getLayout().width(300).height(150).flexDirection(FlexDirection.ROW);
        root.addChildren(
                new SplitView.Horizontal().left(left).right(right)
        );
        var ui = UI.of(root, StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MODERN));
        return new ModularUI(ui);
    }

    private static @NotNull TextElement createTarget(int color, UIElement left, UIElement right) {
        var target = new TextElement();
        target.getStyle().background(SDFRectTexture.of(color).setRadius(new Vector4f(4f)));
        target.getLayout().width(75).height(25);
        target.getTextStyle().textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER);
        target.setText("target");
        var mouseDown = new AtomicBoolean(false);

        target.addEventListener(UIEvents.CLICK, e -> {
            target.setText("click");
        });
        target.addEventListener(UIEvents.DOUBLE_CLICK, e -> {
            target.setText("double click");
        });
        target.addEventListener(UIEvents.MOUSE_ENTER, e -> {
            target.setText("mouse enter");
        });

        target.addEventListener(UIEvents.MOUSE_LEAVE, e -> {
            target.setText("mouse leave");
            if (mouseDown.get()) {
                var parent = target.getParent();
                assert parent != null;
                target.startDrag(null, null);
                target.getLayout().positionType(TaffyPosition.ABSOLUTE)
                        .left(e.x - parent.getContentX() - target.getSizeWidth() / 2)
                        .top(e.y - parent.getContentY() - target.getSizeHeight() / 2);
                mouseDown.set(false);
                target.setText("dragging");
            }
        });
        target.addEventListener(UIEvents.MOUSE_WHEEL, e -> {
            target.setText("wheel " + (e.deltaY > 0 ? "up" : "down"));
        });
        target.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            mouseDown.set(true);
        });
        target.addEventListener(UIEvents.MOUSE_UP, e -> {
            mouseDown.set(false);
        });

        target.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, e -> {
            target.setText("dragging");
            var parent = target.getParent();
            assert parent != null;
            target.getLayout().positionType(TaffyPosition.ABSOLUTE)
                    .left(e.x - parent.getContentX() - target.getSizeWidth() / 2)
                    .top(e.y - parent.getContentY() - target.getSizeHeight() / 2);
            if (left.isMouseOver(e.x, e.y)) {
                left.getStyle().overlay(SDFRectTexture.of(0)
                        .setBorderColor(ColorPattern.T_RED.color)
                        .setRadius(new Vector4f(4f)).setStroke(2));
            } else {
                left.getStyle().overlay(null);
            }
            if (right.isMouseOver(e.x, e.y)) {
                right.getStyle().overlay(SDFRectTexture.of(0)
                        .setBorderColor(ColorPattern.T_RED.color)
                        .setRadius(new Vector4f(4f)).setStroke(2));
            } else {
                right.getStyle().overlay(null);
            }
        });
        target.addEventListener(UIEvents.DRAG_END, e -> {
            target.getLayout().positionType(TaffyPosition.RELATIVE).left(0).top(0);
            if (left != target.getParent() && left.isMouseOver(e.x, e.y)) {
                left.addChildren(target);
            } else if (right != target.getParent() && right.isMouseOver(e.x, e.y)) {
                right.addChildren(target);
            }
            left.getStyle().overlay(null);
            right.getStyle().overlay(null);
        });
        return target;
    }

}
