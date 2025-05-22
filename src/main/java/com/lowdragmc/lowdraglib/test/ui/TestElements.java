package com.lowdragmc.lowdraglib.test.ui;

import com.lowdragmc.lowdraglib.editor_outdated.Icons;
import com.lowdragmc.lowdraglib.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib.gui.ui.UI;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib.gui.ui.elements.*;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegisterClient;
import lombok.NoArgsConstructor;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import org.appliedenergistics.yoga.*;

import java.util.Arrays;

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
                                    layout.setHeight(30);
                                }).setId("header")
                                .style(style -> style.backgroundTexture(Sprites.BORDER)),
                        new UIElement()
                                .layout(layout -> {
                                    layout.setFlex(1);
                                    layout.setMargin(YogaEdge.HORIZONTAL, 10);
                                    layout.setMargin(YogaEdge.BOTTOM, 10);
                                    layout.setPadding(YogaEdge.ALL, 5);
                                    layout.setGap(YogaGutter.ROW, 2);
                                }).setId("flex-1")
                                .style(style -> style.backgroundTexture(Sprites.BORDER))
                                .addChildren(
                                        new Button(),
                                        new Toggle(),
                                        new Selector<Direction.Axis>().setCandidates(Arrays.stream(Direction.Axis.values()).toList()),
                                        new Selector<Direction>().setCandidates(Arrays.stream(Direction.values()).toList()),
                                        new TextField(),
                                        new TextField().setResourceLocationOnly(),
                                        new TextField().setNumbersOnlyInt(23, 145),
                                        new Scroller.Horizontal(),
                                        new ScrollerView()
                                                .addScrollViewChildren(new Button(), new Button().layout(layout-> layout.setWidth(300)),
                                                        new TextField().setNumbersOnlyFloat(-3, 3),
                                                        new ScrollerView().addScrollViewChildren(new Button(), new Button(), new Button(), new Button(), new Button(), new Button(), new Button(), new Button(), new Button(), new Button())
                                                                .layout(layout -> {
                                                                    layout.setWidth(120);
                                                                    layout.setHeight(120);
                                                                }),
                                                        new Button(), new Button(), new Button(), new Button(), new Button())
                                                .layout(layout -> layout.setFlexGrow(1)),
                                        new ProgressBar().label(label -> label.setText("30%")).setValue(0.3f),
                                        new TabView().addTab(new Tab().setText("tab1"), new UIElement().addChildren(
                                                new ColorSelector().layout(layout -> {
                                                    layout.setWidth(60);
                                                })))
                                                .addTab(new Tab().setText("second tab"), new UIElement().layout(layout -> layout.setHeight(60)))
                                )
                )
        );
        // menu
        root.addEventListener(UIEvents.MOUSE_DOWN, e -> {
           if (e.button == 1) {
               root.addChildren(new Menu<>(TreeBuilder.Menu.start()
                       .leaf("item1", () -> {})
                       .leaf(Icons.COPY, "item2", () -> {})
                       .crossLine()
                       .branch("branch1", menu -> {
                            menu.leaf("item3", () -> {});
                            menu.leaf("item4", () -> {});
                       })
                       .leaf("item5", () -> {})
                       .build(), TreeBuilder.Menu::uiProvider)
                       .layout(layout -> {
                           layout.setPosition(YogaEdge.LEFT, e.x - root.getPositionX());
                           layout.setPosition(YogaEdge.TOP, e.y - root.getContentY());
                       }));
           }
        });
        return new ModularUI(UI.of(root));
    }
}
