package com.lowdragmc.lowdraglib2.test.uitest;

import com.google.gson.JsonParser;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIRecording;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

/** Checks the recorder's files: framing, scale, frame rate, poster, caption tracks, timeline, and a second take. */
@LDLRegisterClient(name = "recorder_outputs", group = "ldlib2", registry = UIRecording.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class RecorderOutputsRecording implements UIRecording {

    private static final float PADDING = 12;

    @Override
    public void configure(ScenarioOptions options) {
        options.tags("selfcheck").requiresWorld(true);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("a panel", RecorderOutputsRecording::ui)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#press")
                .step("remember the panel's width", ctx -> ctx.put("panelWidth", ctx.el("#panel").bounds().width()))
                .take("framed", o -> o.region("#panel").padding(PADDING).scale(0.5f).fps(30).poster(500).burnCaptions("en"), t -> t
                        .caption("Hello")
                        .caption("zh", "你好")
                        .spotlight("#press")
                        .click("#press")
                        .clearSpotlight()
                        .caption("")
                        .zoom("#press", 300)
                        .zoomOut(300)
                        .hold(300))
                .take("second", o -> o.noPoster(), t -> t
                        .click("#press")
                        .hold(200))
                .checkTextContains("#count", "2")
                .step("the framed take wrote everything it was asked to", RecorderOutputsRecording::checkFramed)
                .step("the second take recorded too, without a poster", ctx -> {
                    var dir = dir(ctx);
                    ctx.check("second.mp4 is not empty", size(dir.resolve("second.mp4")) > 0);
                    ctx.check("no poster was asked for", !Files.exists(dir.resolve("second.png")));
                    ctx.check("no captions were given", !Files.exists(dir.resolve("second.en.vtt")));
                })
                .closeScreen();
    }

    private static void checkFramed(TestContext ctx) {
        try {
            checkFramedFiles(ctx);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void checkFramedFiles(TestContext ctx) throws IOException {
        var dir = dir(ctx);
        ctx.check("framed.mp4 is not empty", size(dir.resolve("framed.mp4")) > 0);

        var poster = dir.resolve("framed.png");
        ctx.require("the poster exists", Files.exists(poster));
        var header = ByteBuffer.wrap(Files.readAllBytes(poster), 16, 8);
        int width = header.getInt();
        int height = header.getInt();
        double guiScale = ctx.mc().getWindow().getGuiScale();
        int expectedWidth = (int) ((ctx.<Float>get("panelWidth") + PADDING * 2) * guiScale * 0.5) & ~1;
        ctx.check("the video is the padded panel at half scale", width == expectedWidth, expectedWidth, width);
        ctx.check("both sides are even", width % 2 == 0 && height % 2 == 0, "even", width + "x" + height);

        var english = Files.readString(dir.resolve("framed.en.vtt"), StandardCharsets.UTF_8);
        var chinese = Files.readString(dir.resolve("framed.zh.vtt"), StandardCharsets.UTF_8);
        ctx.check("the English track has its cue", english.startsWith("WEBVTT") && english.contains("\nHello\n"));
        ctx.check("the Chinese track has its cue", chinese.contains("\n你好\n"));

        var timeline = JsonParser.parseString(Files.readString(dir.resolve("framed.timeline.json"))).getAsJsonObject();
        ctx.check("the timeline records the take's frame rate", timeline.get("fps").getAsInt() == 30);
        var types = new HashSet<String>();
        double last = 0;
        boolean ordered = true;
        for (var event : timeline.getAsJsonArray("events")) {
            var object = event.getAsJsonObject();
            types.add(object.get("type").getAsString());
            double t = object.get("t").getAsDouble();
            ordered &= t >= last;
            last = t;
        }
        ctx.log("timeline event types: " + types);
        ctx.check("events are in time order", ordered);
        for (var type : new String[]{"step", "caption", "spotlight", "press", "release", "zoom"}) {
            ctx.check("the timeline has a " + type + " event", types.contains(type));
        }
        ctx.check("the take lasted as long as its steps", timeline.get("seconds").getAsDouble() > 0.9);
    }

    private static Path dir(TestContext ctx) {
        return ctx.outDir().resolve("recorder_outputs");
    }

    private static long size(Path file) {
        try {
            return Files.exists(file) ? Files.size(file) : 0;
        } catch (IOException e) {
            return 0;
        }
    }

    private static ModularUI ui(TestContext ctx) {
        var count = new Label();
        count.setId("count");
        count.setText("0");
        var press = new Button();
        press.setId("press");
        press.setText("Press");
        press.setOnClick(e -> count.setText(String.valueOf(Integer.parseInt(count.getText().getString()) + 1)));

        var panel = new UIElement().setId("panel").addClass("panel_bg");
        panel.layout(layout -> layout.paddingAll(10).gapAll(8).width(180));
        panel.addChildren(new Label().setText("Recorder check"), press, count);

        var root = new UIElement();
        root.layout(layout -> layout.widthPercent(100).heightPercent(100)
                .alignItems(AlignItems.CENTER).justifyContent(AlignContent.CENTER));
        root.addChild(panel);
        return new ModularUI(UI.of(root, StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.GDP)), ctx.player());
    }
}
