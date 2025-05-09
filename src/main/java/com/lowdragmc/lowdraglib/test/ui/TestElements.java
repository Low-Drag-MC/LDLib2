package com.lowdragmc.lowdraglib.test.ui;

import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib.gui.ui.UI;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
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
            layout.setHeight(475);
            layout.setPadding(YogaEdge.ALL, 10);
        }).setId("root");
        root.getStyle().backgroundTexture(ResourceBorderTexture.BORDERED_BACKGROUND);
        root.addChild(new UIElement()
                .layout(layout -> {
                    layout.setFlex(1);
                    layout.setGap(YogaGutter.ROW, 10);
                }).setId("container")
                .style(style -> style.backgroundTexture(ResourceBorderTexture.BORDERED_BACKGROUND))
                .addChildren(new UIElement()
                                .layout(layout -> {
                                    layout.setHeight(60);
                                }).setId("header")
                                .style(style -> style.backgroundTexture(ResourceBorderTexture.BORDERED_BACKGROUND)),
                        new UIElement()
                                .layout(layout -> {
                                    layout.setFlex(1);
                                    layout.setMargin(YogaEdge.HORIZONTAL, 10);
                                }).setId("flex-1")
                                .style(style -> style.backgroundTexture(ResourceBorderTexture.BORDERED_BACKGROUND)),
                        new UIElement()
                                .layout(layout -> {
                                    layout.setFlex(2);
                                    layout.setMargin(YogaEdge.HORIZONTAL, 10);
                                }).setId("flex-2")
                                .style(style -> style.backgroundTexture(ResourceBorderTexture.BORDERED_BACKGROUND)),
                        new UIElement()
                                .layout(layout -> {
                                    layout.setPositionType(YogaPositionType.ABSOLUTE);
                                    layout.setWidthPercent(100);
                                    layout.setPosition(YogaEdge.BOTTOM, 0);
                                    layout.setHeight(64);
                                    layout.setFlexDirection(YogaFlexDirection.ROW);
                                    layout.setAlignItems(YogaAlign.CENTER);
                                    layout.setJustifyContent(YogaJustify.SPACE_AROUND);
                                }).setId("footer")
                                .style(style -> style.backgroundTexture(ResourceBorderTexture.BORDERED_BACKGROUND))
                                .addChildren(new UIElement()
                                                .layout(layout -> {
                                                    layout.setWidth(40);
                                                    layout.setHeight(40);
                                                }).setId("footer-1")
                                                .style(style -> style.backgroundTexture(ResourceBorderTexture.BORDERED_BACKGROUND)),
                                        new UIElement()
                                                .layout(layout -> {
                                                    layout.setWidth(40);
                                                    layout.setHeight(40);
                                                }).setId("footer-2")
                                                .style(style -> style.backgroundTexture(ResourceBorderTexture.BORDERED_BACKGROUND)),
                                        new UIElement()
                                                .layout(layout -> {
                                                    layout.setWidth(40);
                                                    layout.setHeight(40);
                                                }).setId("footer-3")
                                                .style(style -> style.backgroundTexture(ResourceBorderTexture.BORDERED_BACKGROUND)),
                                        new UIElement()
                                                .layout(layout -> {
                                                    layout.setWidth(40);
                                                    layout.setHeight(40);
                                                }).setId("footer-4")
                                                .style(style -> style.backgroundTexture(ResourceBorderTexture.BORDERED_BACKGROUND)))
                )
        );
        return new ModularUI(UI.of(root));
    }
}
