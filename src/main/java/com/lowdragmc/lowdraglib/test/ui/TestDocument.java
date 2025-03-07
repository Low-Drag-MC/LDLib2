package com.lowdragmc.lowdraglib.test.ui;

import com.lowdragmc.lowdraglib.editor.ColorPattern;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.misc.FluidBlockTransfer;
import com.lowdragmc.lowdraglib.misc.FluidStorage;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.player.Player;

import java.util.concurrent.atomic.AtomicInteger;

@LDLRegisterClient(name="document", group = "ui_test")
@NoArgsConstructor
public class TestDocument implements IUITest {
    @Override
    public ModularUI createUI(IUIHolder holder, Player entityPlayer) {
        return new ModularUI(createUI2(), holder, entityPlayer);
    }

    public WidgetGroup createUI() {
        // create a root container
        var root = new WidgetGroup();
        root.setSize(100, 100);
        root.setBackground(ResourceBorderTexture.BORDERED_BACKGROUND);

        // create a label and a button
        var label = new LabelWidget();
        label.setSelfPosition(20, 20);
        label.setText("Hello, World!");
        var button = new ButtonWidget();
        button.setSelfPosition(20, 60);
        button.setSize(60, 20);
        // prepare button textures
        var backgroundImage = ResourceBorderTexture.BUTTON_COMMON;
        var hoverImage = backgroundImage.copy().setColor(ColorPattern.CYAN.color);
        var textAbove = new TextTexture("Click me!");
        button.setButtonTexture(backgroundImage, textAbove);
        button.setClickedTexture(hoverImage, textAbove);

        // add the label and button to the root container
        root.addWidgets(label, button);

        AtomicInteger counter = new AtomicInteger(0);
        button.setOnPressCallback(clickData -> {
            label.setText("Clicked " + counter.incrementAndGet() + " times!");
        });
        return root;
    }

    public WidgetGroup createUI2() {
        // create a root container
        var root = new WidgetGroup();
        root.setSize(100, 100);
        root.setBackground(ResourceBorderTexture.BORDERED_BACKGROUND);

        var selector = new SelectorWidget();
        selector.setSelfPosition((100 - 60) / 2, (100 - 15) / 2);
        selector.initTemplate();
        selector.setClientSideWidget();
        root.addWidget(selector);
        return root;
    }
}
