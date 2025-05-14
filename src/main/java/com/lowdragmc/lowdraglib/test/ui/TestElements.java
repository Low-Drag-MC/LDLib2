package com.lowdragmc.lowdraglib.test.ui;

import com.lowdragmc.lowdraglib.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib.gui.ui.UI;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib.gui.ui.elements.Toggle;
import com.lowdragmc.lowdraglib.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegisterClient;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.player.Player;
import org.appliedenergistics.yoga.*;

@LDLRegisterClient(name="ui_elements", registry = "ui_test")
@NoArgsConstructor
public class TestElements implements IUITest {

    @Override
    public ModularUI createUI(Player entityPlayer) {
        var root = new UIElement();
        root.layout(layout -> {
            layout.setWidth(250);
            layout.setHeight(400);
            layout.setPadding(YogaEdge.ALL, 10);
        }).setId("root");
        root.getStyle().backgroundTexture(Sprites.BORDER);
        root.addChild(new UIElement()
                .layout(layout -> {
                    layout.setFlex(1);
                    layout.setGap(YogaGutter.ROW, 10);
                }).setId("container")
                .style(style -> style.backgroundTexture(Sprites.BORDER))
                .addChildren(new Label()
                                .setText("Hello World!!")
                                .textStyle(style -> style
                                        .fontSize(20)
                                        .textAlignHorizontal(Horizontal.CENTER)
                                        .textAlignVertical(Vertical.CENTER))
                                .layout(layout -> {
                                    layout.setHeight(60);
                                }).setId("header")
                                .style(style -> style.backgroundTexture(Sprites.BORDER)),
                        new UIElement()
                                .layout(layout -> {
                                    layout.setFlex(1);
                                    layout.setMargin(YogaEdge.HORIZONTAL, 10);
                                    layout.setPadding(YogaEdge.ALL, 5);
                                }).setId("flex-1")
                                .style(style -> style.backgroundTexture(Sprites.BORDER))
                                .addChildren(new Button(), new Toggle()),
                        new UIElement()
                                .layout(layout -> {
                                    layout.setWidthPercent(100);
                                    layout.setPosition(YogaEdge.BOTTOM, 0);
                                    layout.setHeight(64);
                                    layout.setFlexDirection(YogaFlexDirection.ROW);
                                    layout.setAlignItems(YogaAlign.CENTER);
                                    layout.setJustifyContent(YogaJustify.SPACE_AROUND);
                                }).setId("footer")
                                .style(style -> style.backgroundTexture(Sprites.BORDER))
                                .addChildren(new UIElement()
                                                .layout(layout -> {
                                                    layout.setWidth(40);
                                                    layout.setHeight(40);
                                                }).setId("footer-1")
                                                .style(style -> style.backgroundTexture(Sprites.BORDER)),
                                        new UIElement()
                                                .layout(layout -> {
                                                    layout.setWidth(40);
                                                    layout.setHeight(40);
                                                }).setId("footer-2")
                                                .style(style -> style.backgroundTexture(Sprites.BORDER)),
                                        new UIElement()
                                                .layout(layout -> {
                                                    layout.setWidth(40);
                                                    layout.setHeight(40);
                                                }).setId("footer-3")
                                                .style(style -> style.backgroundTexture(Sprites.BORDER)),
                                        new UIElement()
                                                .layout(layout -> {
                                                    layout.setWidth(40);
                                                    layout.setHeight(40);
                                                }).setId("footer-4")
                                                .style(style -> style.backgroundTexture(Sprites.BORDER)))
                )
        );
        return new ModularUI(UI.of(root));
    }
}
