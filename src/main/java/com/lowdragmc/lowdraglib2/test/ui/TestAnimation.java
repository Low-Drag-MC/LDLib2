package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.math.interpolate.Eases;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import it.unimi.dsi.fastutil.floats.FloatObjectPair;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.player.Player;
import org.appliedenergistics.yoga.YogaEdge;
import org.joml.Vector2f;

import java.util.List;

@LDLRegisterClient(name="animation", registry = "ldlib2:screen_test")
@NoArgsConstructor
public class TestAnimation implements IScreenTest {
    @Override
    public ModularUI createUI(Player entityPlayer) {
        var root = new UIElement();
        root.layout(layout -> {
            layout.width(300);
            layout.height(300);
            layout.paddingAll(10);
        }).setId("root").getStyle().backgroundTexture(Sprites.BORDER);
        var target = new UIElement();
        target.getLayout().width(100).height(100);
        target.getStyle().background(ColorPattern.PINK.rectTexture());
        root.addChildren(
                new UIElement().layout(layout -> layout.flex(1))
                        .style(style -> style.backgroundTexture((graphics, mouseX, mouseY, x, y, width, height, partialTicks) -> {
                            DrawerHelper.drawLines(graphics, List.of(new Vector2f(x, y), new Vector2f(x + width / 3, y + height / 5), new Vector2f(x + width, y + height)),
                                    -1, 0xff00ffff, 2);
                        })),
                target,
                new Button().setText("anim 1").setOnClick(e -> {
                    target.animation()
                            .duration(1)
                            .ease(Eases.QUAD_IN_OUT)
                            .style(PropertyRegistry.TRANSFORM_2D,new Transform2D().scale(0.5f).translate(100f, 0))
                            .style(PropertyRegistry.OPACITY, 0f)
                            .onFinished(element -> {
                                target.animation()
                                        .ease(Eases.QUART_IN_OUT)
                                        .style(PropertyRegistry.TRANSFORM_2D,new Transform2D())
                                        .style(PropertyRegistry.OPACITY, 1f)
                                        .start();
                            })
                            .start();
                }),
                new Button().setText("anim 1 optional").setOnClick(e -> {
                    target.animation()
                            .duration(2)
                            .ease(Eases.QUAD_IN_OUT)
                            .style(PropertyRegistry.TRANSFORM_2D,
                                    FloatObjectPair.of(0.5f, new Transform2D().scale(0.5f).translate(100f, 0))
                            )
                            .style(PropertyRegistry.OPACITY,
                                    FloatObjectPair.of(0.5f, 0f)
                            )
                            .start();
                }),
                new Button().setText("anim 2").setOnClick(e -> {
                    target.animation()
                            .ease(Eases.QUART_IN_OUT)
                            .lss("width", 250)
                            .lss("height", 150)
                            .onFinished(element -> {
                                target.animation()
                                        .ease(Eases.QUART_IN_OUT)
                                        .delay(1)
                                        .lss("width", 100)
                                        .lss("height", 100)
                                        .start();
                            })
                            .start();
                })
        );

        return new ModularUI(UI.of(root));
    }
}
