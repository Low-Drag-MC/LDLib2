package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Slider;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Switch;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIRecording;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Recording demo: a form filled in by hand, framed tightly around the panel. Made for a 3840x2160 window. */
@LDLRegisterClient(name = "demo_form", group = "ldlib2", registry = UIRecording.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class VideoDemoFormRecording implements UIRecording {

    @Override
    public void configure(ScenarioOptions options) {
        options.tags("demo").guiScale(6);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("profile form", VideoDemoFormRecording::formUI)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#save")
                .take("form", o -> o.region("#profile").scale(0.5f).burnCaptions("zh"), t -> t
                        .caption("Type a name")
                        .caption("zh", "输入名字")
                        .hold(500)
                        .click("#name")
                        .type("Steve")
                        .hold(250)
                        .caption("Ctrl+A selects it all, typing replaces it")
                        .caption("zh", "Ctrl+A 全选，直接输入即可替换")
                        .key(GLFW.GLFW_KEY_A, Keys.MOD_CONTROL)
                        .type("Alex the Builder")
                        .hold(200)
                        .caption("Pick a game mode")
                        .caption("zh", "选择游戏模式")
                        .spotlight("#mode")
                        .hold(700)
                        .clearSpotlight()
                        .click("the game mode dropdown", ctx -> ctx.el("#mode").element(), Keys.MOUSE_LEFT)
                        .awaitElement(".__selector_dialog__")
                        .hold(250)
                        .click("the Creative candidate", ctx -> textIn(ctx, ".__selector_dialog__", "Creative"), Keys.MOUSE_LEFT)
                        .caption("Switches and sliders work as you would expect")
                        .caption("zh", "开关和滑块也一样")
                        .click("#notify")
                        .dragTo("#volume .__slider_handle__", "80% along the track", ctx -> {
                            var track = ctx.el("#volume .__slider_track__").bounds();
                            var handle = ctx.el("#volume .__slider_handle__").bounds();
                            return new float[]{track.x() + track.width() * 0.8f, handle.centerY()};
                        })
                        .caption("Save")
                        .caption("zh", "保存")
                        .click("#save")
                        .hold(1200))
                .checkText("#name", "Alex the Builder")
                .check("the dropdown picked Creative", ctx -> "Creative".equals(ctx.el("#mode").as(Selector.class).getValue()))
                .check("the switch is on", ctx -> Boolean.TRUE.equals(ctx.el("#notify").as(Switch.class).getValue()))
                .check("the slider moved to about 80", ctx -> {
                    var value = ctx.el("#volume").as(Slider.class).getValue();
                    return value != null && value > 70 && value < 90;
                })
                .checkTextContains("#status", "Saved")
                .closeScreen();
    }

    private static UIElement textIn(TestContext ctx, String container, String text) {
        for (var root : ctx.all(container)) {
            var found = findText(root.element(), text);
            if (found != null) return found;
        }
        throw new IllegalStateException("no '" + text + "' under " + container);
    }

    private static UIElement findText(UIElement element, String text) {
        if (element instanceof TextElement label && text.equals(label.getText().getString())) return element;
        for (var child : element.getChildren()) {
            var found = findText(child, text);
            if (found != null) return found;
        }
        return null;
    }

    private static ModularUI formUI(TestContext ctx) {
        var name = new TextField();
        name.setId("name");
        name.layout(layout -> layout.width(170));

        var mode = new Selector<String>();
        mode.setId("mode");
        mode.setCandidates(List.of("Survival", "Creative", "Adventure", "Spectator"));
        mode.setSelected("Survival", false);
        mode.layout(layout -> layout.width(170));

        var notify = new Switch();
        notify.setId("notify");

        var volume = new Slider.Horizontal();
        volume.setId("volume");
        volume.setRange(0, 100).setValue(30f);
        volume.layout(layout -> layout.width(170));

        var status = new Label();
        status.setId("status");
        status.setText("Not saved yet");

        var save = new Button();
        save.setId("save");
        save.setText("Save");
        save.setOnClick(e -> status.setText("Saved!"));

        var panel = new UIElement().setId("profile").addClass("panel_bg");
        panel.layout(layout -> layout.paddingAll(12).gapAll(8).width(300));
        panel.addChildren(
                new Label().setText("Create a profile").textStyle(style -> style.fontSize(14)),
                row("Name", name), row("Game mode", mode), row("Notifications", notify), row("Volume", volume),
                new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(10).alignItems(AlignItems.CENTER))
                        .addChildren(save.layout(layout -> layout.width(60)), status));
        return centered(ctx, panel);
    }

    private static UIElement row(String caption, UIElement field) {
        return new UIElement()
                .layout(layout -> layout.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapAll(10))
                .addChildren(new Label().setText(caption).layout(layout -> layout.width(100)), field);
    }

    private static ModularUI centered(TestContext ctx, UIElement panel) {
        var root = new UIElement();
        root.layout(layout -> layout.widthPercent(100).heightPercent(100)
                .alignItems(AlignItems.CENTER).justifyContent(AlignContent.CENTER));
        root.addChild(panel);
        return new ModularUI(UI.of(root, StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.GDP)), ctx.player());
    }

}
