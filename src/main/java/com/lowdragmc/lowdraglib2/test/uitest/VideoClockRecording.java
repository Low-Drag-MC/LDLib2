package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIRecording;
import com.lowdragmc.lowdraglib2.uitest.record.VideoClock;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.util.Util;

/** Checks the recorder itself: video time is stepped per frame inside a take, and client and server ticks keep pace with it. */
@LDLRegisterClient(name = "video_clock", group = "ldlib2", registry = UIRecording.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class VideoClockRecording implements UIRecording {

    private static final int TRACK_WIDTH = 320;
    private static final int BOX = 16;
    private static final long CLIP_MS = 3000;

    @Override
    public void configure(ScenarioOptions options) {
        options.tags("selfcheck").requiresWorld(true);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("animated ui", VideoClockRecording::ui)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#sweep")
                .step("drive the boxes from frame time", ctx -> {
                    var sweep = ctx.el("#sweep").element();
                    var bounce = ctx.el("#bounce").element();
                    var readout = (Label) ctx.el("#readout").element();
                    var player = ctx.requirePlayer();
                    var server = ctx.requireServer();
                    ctx.requireUI().addFrameCallback(nanos -> {
                        double seconds = nanos / 1e9;
                        sweep.getLayout().left((float) ((seconds / 2 % 1) * (TRACK_WIDTH - BOX)));
                        bounce.getLayout().left((float) ((Math.sin(seconds * Math.PI) * 0.5 + 0.5) * (TRACK_WIDTH - BOX)));
                        readout.setText("client tick %d  server tick %d  frame %d"
                                .formatted(player.tickCount, server.getTickCount(), VideoClock.frames()));
                    });
                    ctx.requireUI().requestFocus(ctx.el("#caret").element());
                })
                .take("clock", take -> take
                        .step("mark", VideoClockRecording::mark)
                        .waitMs(CLIP_MS)
                        .step("measure", VideoClockRecording::measure))
                .closeScreen();
    }

    private static void mark(TestContext ctx) {
        ctx.put("nanos", Util.getNanos());
        ctx.put("wall", System.nanoTime());
        ctx.put("frames", VideoClock.frames());
        ctx.put("clientTicks", ctx.requirePlayer().tickCount);
        ctx.put("serverTicks", ctx.requireServer().getTickCount());
    }

    private static void measure(TestContext ctx) {
        double seconds = (Util.getNanos() - ctx.<Long>get("nanos")) / 1e9;
        double wall = (System.nanoTime() - ctx.<Long>get("wall")) / 1e9;
        int clientTicks = ctx.requirePlayer().tickCount - ctx.<Integer>get("clientTicks");
        int serverTicks = ctx.requireServer().getTickCount() - ctx.<Integer>get("serverTicks");
        ctx.log("%.3fs of game time in %.3fs wall; %d client ticks, %d server ticks"
                .formatted(seconds, wall, clientTicks, serverTicks));

        long frames = VideoClock.frames() - ctx.<Long>get("frames");
        double expected = frames / (double) ctx.runner().recordSettings().fps();
        ctx.check("game time advanced exactly one video frame per rendered frame",
                Math.abs(seconds - expected) < 1e-6, expected, seconds);
        ctx.check("client ticked at 20 per second of game time",
                Math.abs(clientTicks - seconds * 20) <= 2, Math.round(seconds * 20), clientTicks);
        ctx.check("the integrated server ticked at 20 per second of game time",
                Math.abs(serverTicks - seconds * 20) <= 3, Math.round(seconds * 20), serverTicks);
    }

    private static ModularUI ui(TestContext ctx) {
        var track = new UIElement();
        track.layout(layout -> layout.width(TRACK_WIDTH).height(BOX * 3));
        track.style(style -> style.backgroundTexture(new ColorRectTexture(0xff202830)));
        track.addChildren(box("sweep", 0xffe0a040, 0), box("bounce", 0xff40a0e0, BOX * 2));

        var caret = new TextField();
        caret.setId("caret");
        caret.setText("the caret blinks once per second of video");
        caret.layout(layout -> layout.width(TRACK_WIDTH));

        var panel = new UIElement().addClass("panel_bg");
        panel.layout(layout -> layout.paddingAll(10).gapAll(8));
        panel.addChildren(track, caret, new Label().setId("readout"));

        var root = new UIElement();
        root.layout(layout -> layout.widthPercent(100).heightPercent(100)
                .alignItems(AlignItems.CENTER).justifyContent(AlignContent.CENTER));
        root.addChild(panel);
        return new ModularUI(UI.of(root, StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.GDP)), ctx.player());
    }

    private static UIElement box(String id, int color, float top) {
        var box = new UIElement().setId(id);
        box.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).top(top).left(0).width(BOX).height(BOX));
        box.style(style -> style.backgroundTexture(new ColorRectTexture(color)));
        return box;
    }
}
