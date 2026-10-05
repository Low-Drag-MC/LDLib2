package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * Enum {@link Selector}s bound through {@link DataBindingBuilder#enumVal} inside a content-sized {@link ScrollerView},
 * one bound only and one also given candidates and a value first. Driven by {@code SelectorInitialValueScenario}.
 */
@LDLRegister(name = "ui_selector_initial_value", registry = "ldlib2:menu_test")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TestSelectorInitialValue implements IMenuTest {
    public enum Mode {
        BOTH, RANDOM, ORDERED
    }

    private Mode mode = Mode.ORDERED;

    @Override
    public ModularUI createUI(Player player) {
        var root = new ScrollerView();
        root.scrollerStyle(style -> style.mode(ScrollerMode.VERTICAL));
        root.viewContainer.layout(layout -> layout.gapAll(5).paddingAll(7).maxHeight(200).maxWidth(400).minWidth(100));
        root.addClass("panel_bg");

        var bindOnly = new Selector<Mode>();
        bindOnly.bind(DataBindingBuilder.enumVal(Mode.class, () -> mode, value -> mode = value).build());
        bindOnly.setId("bind_only");

        var preset = new Selector<Mode>();
        preset.setCandidates(List.of(Mode.values()));
        preset.setValue(mode, true);
        preset.bind(DataBindingBuilder.enumVal(Mode.class, () -> mode, value -> mode = value).build());
        preset.setId("preset");

        root.viewContainer.addChildren(
                new Button().setText("Edit ->"),
                bindOnly,
                preset,
                new Button().setText("Delete"),
                new Label().setText("Tag: minecraft:logs"));
        return ModularUI.of(UI.of(root, StylesheetManager.GDP), player);
    }
}
