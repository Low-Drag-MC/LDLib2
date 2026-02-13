package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.CodeEditor;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.Languages;
import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.player.Player;

import java.util.concurrent.atomic.AtomicReference;

@LDLRegisterClient(name="lss", registry = "ldlib2:screen_test")
@NoArgsConstructor
public class TestLSS implements IScreenTest {
    @Override
    public ModularUI createUI(Player entityPlayer) {
        var root = new UIElement().addClass("panel_bg");

        var left = new UIElement().layout(layout -> layout.flex(2).heightPercent(100))
                .setOverflowVisible(false)
                .addClass("preview_bg");

        var right = new UIElement().layout(layout -> layout.flex(1).heightPercent(100))
                .setOverflowVisible(false)
                .addClass("preview_bg");

        var target = new UIElement().setId("target");
        var initialLSS = """
                #target {
                  width: 50;
                  height: 50;
                  background: #fff;
                }
                """;
        AtomicReference<Stylesheet> stylesheetRef = new AtomicReference<>(Stylesheet.parse(initialLSS));
        left.addChildren(new CodeEditor()
                .setLanguage(Languages.LSS)
                .setLinesResponder(lines -> {
                    var stylesheet = Stylesheet.parse(String.join("\n", lines));
                    if (target.getModularUI() instanceof ModularUI mui) {
                        mui.getStyleEngine().removeStylesheet(stylesheetRef.get());
                        mui.getStyleEngine().addStylesheet(stylesheet);
                    }
                })
                .setValue(initialLSS.split("\n"), false)
                .layout(layout -> layout.widthPercent(100).heightPercent(100))
        );

        right.addChildren(target);

        root.getLayout().width(300).height(150).flexDirection(FlexDirection.ROW);
        root.addChildren(
                left,
                right
        );
        var ui = UI.of(root, StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC), stylesheetRef.get());
        return new ModularUI(ui);
    }

}
