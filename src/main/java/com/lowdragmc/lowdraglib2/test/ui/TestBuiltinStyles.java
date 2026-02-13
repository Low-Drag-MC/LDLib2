package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.resource.FilePath;
import com.lowdragmc.lowdraglib2.editor.resource.UIResource;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ToggleGroupElement;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.NoArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

@LDLRegisterClient(name="builtin_styles", registry = "ldlib2:screen_test")
@NoArgsConstructor
public class TestBuiltinStyles implements IScreenTest {
    @Override
    public ModularUI createUI(Player entityPlayer) {
        var root = new UIElement().addClass("panel_bg");

        var left = new UIElement()
                .addClass("preview_bg")
                .addChild(new ToggleGroupElement().layout(layout -> layout.width(80)).addChildren(
                        toggleStylesheets("gdp", StylesheetManager.GDP).selfCall(t -> ((Toggle)t)
                                .setOn(true, false)),
                        toggleStylesheets("mc", StylesheetManager.MC),
                        toggleStylesheets("modern", StylesheetManager.MODERN)
                ));

        var right = new UIElement()
                .addClass("preview_bg");

        right.addChildren(Optional.ofNullable(UIResource.INSTANCE.getResourceInstance()
                        .getResource(new FilePath(LDLib2.id("resources/global/gdp_styles.ui.nbt"))))
                .map(UITemplate::createUI)
                .orElseGet(UI::empty).rootElement);

        root.getLayout().flexDirection(FlexDirection.ROW);
        root.addChildren(
                left,
                right
        );
        var ui = UI.of(root, StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.GDP));
        return new ModularUI(ui);
    }

    private Toggle toggleStylesheets(String name, ResourceLocation stylesheet) {
        var toggle = new Toggle();
        toggle.setText(name);
        toggle.setOnToggleChanged(isOn -> {
            // switch to the selected stylesheet
            var mui = toggle.getModularUI();
            if (isOn && mui != null) {
                mui.getStyleEngine().clearAllStylesheets();
                mui.getStyleEngine().addStylesheet(StylesheetManager.INSTANCE.getStylesheetSafe(stylesheet));
            }
        });
        return toggle;
    }

}
