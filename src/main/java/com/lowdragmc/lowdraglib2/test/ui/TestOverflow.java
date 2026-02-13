package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ColorSelector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.CodeEditor;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.Languages;
import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.player.Player;

import java.util.concurrent.atomic.AtomicReference;

@LDLRegisterClient(name="overflow", registry = "ldlib2:screen_test")
@NoArgsConstructor
public class TestOverflow implements IScreenTest {
    @Override
    public ModularUI createUI(Player entityPlayer) {
        var root = new UIElement().addClass("panel_bg");

        var left = new UIElement().layout(layout -> layout.flex(2).heightPercent(100))
                .addClass("preview_bg");

        var center = new UIElement().layout(layout -> layout.flex(1).aspectRatio(1))
                .addClass("preview_bg");

        var right = new UIElement().layout(layout -> layout.flex(1).heightPercent(100))
                .addClass("preview_bg");

        var clipImage = new UIElement().setId("clip-image").layout(layout -> layout.widthPercent(100).heightPercent(100));
        var initialLSS = """
                #clip-image {
                  background: #fff;
                }
                #overflow-container {
                }
                """;

        AtomicReference<Stylesheet> stylesheetRef = new AtomicReference<>(Stylesheet.parse(initialLSS));
        left.addChildren(new CodeEditor()
                .setLanguage(Languages.LSS)
                .setLinesResponder(lines -> {
                    var stylesheet = Stylesheet.parse(String.join("\n", lines));
                    if (clipImage.getModularUI() instanceof ModularUI mui) {
                        mui.getStyleEngine().removeStylesheet(stylesheetRef.get());
                        mui.getStyleEngine().addStylesheet(stylesheet);
                    }
                })
                .setValue(initialLSS.split("\n"), false)
                .layout(layout -> layout.widthPercent(100).heightPercent(100))
        );

        center.addChildren(clipImage);

        right.addChild(new UIElement().layout(layout -> {
            layout.heightPercent(100);
            layout.widthPercent(100);
        }).setId("overflow-container").addChild(new ColorSelector().layout(layout -> layout.widthPercent(150))));

        root.getLayout().width(300).height(150).flexDirection(FlexDirection.ROW);
        root.addChildren(
                left,
                center,
                right
        );
        var ui = UI.of(root, StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC), stylesheetRef.get());
        return new ModularUI(ui);
    }

}
