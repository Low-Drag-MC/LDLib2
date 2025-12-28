package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.resource.FilePath;
import com.lowdragmc.lowdraglib2.editor.resource.UIResource;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import lombok.NoArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.function.Supplier;

@LDLRegisterClient(name="component_examples", registry = "ldlib2:screen_test")
@NoArgsConstructor
public class TestComponentExamples implements IScreenTest {
    @Override
    public ModularUI createUI(Player entityPlayer) {
        var ui = Optional.ofNullable(UIResource.INSTANCE.getResourceInstance()
                .getResource(new FilePath(LDLib2.id("resources/examples/example_layout.ui.nbt"))))
                .map(UITemplate::createUI)
                .orElseGet(UI::empty);
        toggleStylesheets(ui, "#gdp-toggle", StylesheetManager.GDP);
        toggleStylesheets(ui, "#mc-toggle", StylesheetManager.MC);
        toggleStylesheets(ui, "#modern-toggle", StylesheetManager.MODERN);
        var scrollerView = ui.select("#example-list", ScrollerView.class).findFirst().orElseThrow();
        var rightContainer = ui.select("#right_container", UIElement.class).findFirst().orElseThrow();
        addExample(scrollerView, rightContainer, "button", this::buttonExample);
        return new ModularUI(ui);
    }

    private void toggleStylesheets(UI ui, String selector, ResourceLocation stylesheet) {
        ui.select(selector, Toggle.class).findFirst().ifPresent(toggle -> toggle.setOnToggleChanged(isOn -> {
            // switch to the selected stylesheet
            var mui = toggle.getModularUI();
            if (isOn && mui != null) {
                mui.getStyleEngine().clearAllStylesheets();
                mui.getStyleEngine().addStylesheet(StylesheetManager.INSTANCE.getStylesheetSafe(stylesheet));
            }
        }));
    }

    private void addExample(ScrollerView scrollerView, UIElement container, String name, Supplier<UIElement> exampleSupplier) {
        scrollerView.addScrollViewChild(new Button().setText(name).setOnClick(e -> {
            container.clearAllChildren();
            container.addChild(exampleSupplier.get());
        }));
    }

    private UIElement buttonExample() {
        return new UIElement().addChildren(
                new Button()
        );
    }
}
