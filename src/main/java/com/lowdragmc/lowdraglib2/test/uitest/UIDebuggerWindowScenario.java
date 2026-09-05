package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.client.window.OsWindow;
import com.lowdragmc.lowdraglib2.client.window.OsWindowManager;
import com.lowdragmc.lowdraglib2.gui.ui.debugger.UIDebuggerWindowState;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ElementBounds;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.capture.FrameCapture;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.util.ARGB;
import org.lwjgl.glfw.GLFW;

import static com.lowdragmc.lowdraglib2.test.uitest.UIDebuggerScenario.*;

/**
 * What the window host adds over the panel.
 *
 * <p>Three things, none of which can be seen in an ordinary screenshot:
 * <ul>
 *   <li>the overlay is drawn into the <em>game</em> window even though the debugger is not there any
 *       more. Checked against real pixels: with the picker armed there has to be a red crosshair line
 *       in the game's frame at the pointer, and nothing a few units away from it;</li>
 *   <li>the window's size and position survive being closed and reopened. A tool that is opened and
 *       closed constantly and reverts to a default rectangle each time has to be re-arranged each
 *       time;</li>
 *   <li>it can be pinned above the other windows, which on a single monitor is the only way "beside
 *       the UI" means anything at all.</li>
 * </ul>
 */
@LDLRegisterClient(name = "ui_debugger_window", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class UIDebuggerWindowScenario implements UIScenario {

    private static final String GEOMETRY = "debugger_window_geometry";
    private static final String PIN_BEFORE = "debugger_pin_before";
    private static final String PIN_AFTER = "debugger_pin_after";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("debugger", "window").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        popOut(openTarget(s))

                .group("the overlay is drawn into the game window, not this one", g -> g
                        .step("arm the picker from the debugger window",
                                ctx -> ctx.input(window(ctx)).key(GLFW.GLFW_KEY_F1, 0))
                        .frames(5)
                        .check("focus mode is on", ctx -> debugger(ctx).isFocusMode())
                        .step("move the game window's pointer onto the button",
                                ctx -> moveTo(ctx, button(ctx)))
                        .frames(3)
                        .check("the debugger is outlining it",
                                ctx -> isWithin(debugger(ctx).getShapingElement(), button(ctx)))
                        .step("the crosshair is in the game's frame, at the pointer", ctx -> {
                            var frame = FrameCapture.grab();
                            try {
                                var pointerY = ElementBounds.of(button(ctx)).centerY();
                                var scale = ctx.mc().getWindow().getGuiScale();
                                var probeX = ctx.mc().getWindow().getGuiScaledWidth() - 12f;
                                ctx.check("a red line crosses the frame at the pointer's row",
                                        hasRedNear(frame, probeX, pointerY, scale));
                                // The control. Without it a frame that had gone red for any other
                                // reason - a failed clear, a shader mod - would answer yes above.
                                ctx.check("and not thirty units above it",
                                        !hasRedNear(frame, probeX, pointerY - 30, scale));
                            } finally {
                                FrameCapture.closeQuietly(frame);
                            }
                        })
                        .step("press", UIDebuggerScenario::press)
                        .step("release", UIDebuggerScenario::release)
                        .frames(5)
                        .check("the debugger selected what the pointer was over",
                                ctx -> debugger(ctx).hierarchy.getSelectedOne().orElse(null)
                                        == targetUI(ctx).getLastHoveredElement())
                        .check("and the button was not pressed by being inspected",
                                ctx -> clicks(ctx).get() == 0)
                        .screenshot("01_overlay_in_the_game_window")
                        .step("disarm the picker",
                                ctx -> ctx.input(window(ctx)).key(GLFW.GLFW_KEY_F1, 0))
                        .frames(3)
                        .step("the crosshair is gone", ctx -> {
                            var frame = FrameCapture.grab();
                            try {
                                var pointerY = ElementBounds.of(button(ctx)).centerY();
                                var scale = ctx.mc().getWindow().getGuiScale();
                                var probeX = ctx.mc().getWindow().getGuiScaledWidth() - 12f;
                                ctx.check("no red line at the pointer's row",
                                        !hasRedNear(frame, probeX, pointerY, scale));
                            } finally {
                                FrameCapture.closeQuietly(frame);
                            }
                        }))

                .group("it can be pinned above the other windows", g -> g
                        // The pin is remembered across runs, so what is asserted is the flip, not an
                        // absolute state. Asserting "pinned" would pass on the first run and fail on
                        // the second, which is a test that reports the previous run rather than this
                        // one.
                        .step("note whether it starts pinned",
                                ctx -> ctx.put(PIN_BEFORE, window(ctx).isAlwaysOnTop()))
                        .step("aim at the pin", ctx ->
                                ctx.input(window(ctx)).moveTo(window(ctx).alwaysOnTopToggle))
                        .frames(2)
                        .step("click it", ctx -> ctx.input(window(ctx))
                                .mouseDown(Keys.MOUSE_LEFT).mouseUp(Keys.MOUSE_LEFT))
                        .frames(10)
                        // Vacuous where the platform refuses to stack a client's own windows, which is
                        // the honest answer there — Wayland has no way to do this at all.
                        .step("the pin reached the real window", ctx -> {
                            if (!OsWindow.supportsAlwaysOnTop()) {
                                ctx.log("this platform cannot pin windows; nothing to check");
                                return;
                            }
                            boolean before = ctx.get(PIN_BEFORE);
                            ctx.check("GLFW's floating attribute flipped",
                                    window(ctx).isAlwaysOnTop() != before);
                            ctx.put(PIN_AFTER, window(ctx).isAlwaysOnTop());
                        }))

                .group("its size and position survive a close and reopen", g -> g
                        .step("move and resize it", ctx -> {
                            var os = window(ctx).window();
                            os.setPosition(140, 110);
                            os.setSize(560, 420);
                        })
                        // The platform reports the result through callbacks, which only run when
                        // Minecraft polls - so the recorded values are what it actually did, not what
                        // it was asked for.
                        .frames(15)
                        .step("record where it ended up", ctx -> {
                            var os = window(ctx).window();
                            ctx.put(GEOMETRY, new int[]{os.getPositionX(), os.getPositionY(),
                                    os.getWindowWidth(), os.getWindowHeight()});
                        })
                        .check("the move and resize took effect", ctx -> {
                            int[] geometry = ctx.get(GEOMETRY);
                            return geometry[2] > 0 && geometry[3] > 0;
                        })

                        .key(GLFW.GLFW_KEY_F12)
                        .frames(10)
                        .check("the debugger closed", ctx -> !OsWindowManager.hasWindows())
                        .check("the inspected UI is still open", ctx -> !targetUI(ctx).isRemoved()))

                .group("reopening puts it back where it was", g -> {
                    popOut(g);
                    g.check("the window came back at the same size and position", ctx -> {
                            int[] before = ctx.get(GEOMETRY);
                            var os = window(ctx).window();
                            return os.getPositionX() == before[0] && os.getPositionY() == before[1]
                                    && os.getWindowWidth() == before[2] && os.getWindowHeight() == before[3];
                    })
                     .check("and with the pin as it was left", ctx -> !OsWindow.supportsAlwaysOnTop()
                             || window(ctx).isAlwaysOnTop() == (boolean) ctx.get(PIN_AFTER))
                     .screenshotSurface("02_reopened_debugger_window", UIDebuggerScenario::requireSurface);
                })

                // The path that leaks a window onto the user's desktop: the inspected UI goes away
                // while its debugger is still open, and nothing else is watching.
                .group("closing the inspected screen takes the window with it", g -> g
                        .closeScreen()
                        .frames(20)
                        .check("the debugger window closed with its host",
                                ctx -> !OsWindowManager.hasWindows()))

                .check("no window host ever threw while being driven",
                        ctx -> OsWindowManager.totalFailures() == 0)

                // Through the stored state, not through a window: by this point the last group has
                // already closed it, and a loop over open windows would quietly do nothing and leave
                // the pin set for whoever runs the game next.
                .teardown("unpin, so a real session is not left with a pinned debugger", ctx -> {
                    var state = UIDebuggerWindowState.get();
                    UIDebuggerWindowState.put(new UIDebuggerWindowState(
                            state.x(), state.y(), state.width(), state.height(), false));
                })
                .teardown("close any window the run left behind", ctx -> {
                    for (var host : OsWindowManager.hosts()) {
                        OsWindowManager.close(host);
                    }
                })
                .teardown("close the screen", ctx -> ctx.mc().gui.setScreen(null));
    }

    /** F12, then the title bar's window toggle — the two clicks a user makes to get a window. */
    static ScenarioBuilder popOut(ScenarioBuilder s) {
        return s.key(GLFW.GLFW_KEY_F12)
                .frames(10)
                .step("aim at the window toggle", ctx -> moveTo(ctx, debugger(ctx).windowModeToggle))
                .frames(2)
                // One step: the toggle acts on the press, and the layer it lives in is popped on the
                // next frame - a release of its own would land on whatever took its place.
                .step("click the window toggle", UIDebuggerScenario::click)
                .frames(25)
                .check("a debugger window is open", ctx -> debuggerWindow(ctx) != null);
    }

    /**
     * Whether any pixel within three GUI units of {@code (guiX, guiY)} is the crosshair's red.
     *
     * <p>A band rather than a point: the line is one GUI unit wide, the pointer position the frame
     * was rendered with is truncated to an int on its way through {@code MouseHandler}, and neither
     * is worth pinning a test to.
     */
    private static boolean hasRedNear(NativeImage frame, float guiX, float guiY, double scale) {
        int x = clamp((int) (guiX * scale), frame.getWidth());
        int from = clamp((int) ((guiY - 3) * scale), frame.getHeight());
        int to = clamp((int) ((guiY + 3) * scale), frame.getHeight());
        for (int y = from; y < to; y++) {
            // Unlike 1.21's getPixelRGBA, which handed back the raw ABGR word, getPixel converts to
            // ARGB on the way out - so the plain accessors are the right lens here.
            var pixel = frame.getPixel(x, y);
            if (ARGB.red(pixel) > 200
                    && ARGB.green(pixel) < 60
                    && ARGB.blue(pixel) < 60) {
                return true;
            }
        }
        return false;
    }

    private static int clamp(int value, int limit) {
        return Math.clamp(value, 0, limit - 1);
    }
}
