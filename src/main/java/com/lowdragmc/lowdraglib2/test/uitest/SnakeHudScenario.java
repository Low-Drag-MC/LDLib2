package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;

/**
 * End-to-end coverage of the client-only half of the harness, against the {@code mcp} screen test
 * (the snake game), which was built with addressable element ids for exactly this purpose.
 *
 * <p>Between them the steps here exercise every mechanism the framework depends on: selector
 * resolution, transform-correct bounds, hover state, click delivery through the real
 * {@code Screen} → {@code ModularUIWidget} → {@code UIEventDispatcher} path, focus, keyboard
 * routing, tick-driven UI updates, and captures. If this scenario is green the plumbing works; a
 * failure here is a harness bug, not a UI bug.
 */
@LDLRegisterClient(name = "snake_hud", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class SnakeHudScenario implements UIScenario {

    @Override
    public void configure(ScenarioOptions options) {
        // A world is required even though nothing here touches it: IScreenTest#createUI takes a
        // Player, so the screen cannot be built on the title screen.
        // A 176x194 panel is unreadable in a maximised screenshot at the default scale.
        options.defaultSettleMs(30).tags("client", "fast").requiresWorld(true).guiScale(4);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openScreenTest("mcp")
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#root")

                // structure
                .checkExists("#board")
                .checkExists("#hud_top")
                .checkExists("#restart_button")
                .checkCount("#board > *", 20)            // one row element per board row
                .checkTextContains("#score_label", "Score:")
                .checkTextContains("#status_label", "Running")
                .screenshot("01_opened")

                // Layout sanity through the bounds pipeline. Cells are laid out inside a scaled,
                // padded container, so getting these right depends on the world transform being
                // applied - which is the exact thing the old driver got wrong.
                .step("board cells form a grid", ctx -> {
                    var origin = ctx.el("#cell_0_0").bounds();
                    var right = ctx.el("#cell_1_0").bounds();
                    var below = ctx.el("#cell_0_1").bounds();
                    ctx.check("cell_1_0 sits right of cell_0_0", right.x() > origin.x(),
                            "> " + origin.x(), right.x());
                    ctx.check("cell_0_1 sits below cell_0_0", below.y() > origin.y(),
                            "> " + origin.y(), below.y());
                    ctx.check("cells are square-ish", Math.abs(origin.width() - origin.height()) < 1.5f,
                            origin.width(), origin.height());
                    ctx.attach("cellSize", "%.2f x %.2f".formatted(origin.width(), origin.height()));
                })

                // hover -> the __hovered__ state class is applied by the same code path a real
                // cursor drives, so this proves the hover sync works rather than just that the
                // element exists
                .hover("#restart_button")
                .checkHovered("#restart_button")
                .checkClass("#restart_button", "__hovered__")
                // The check above already covers the synthetic cursor indirectly - the hover is
                // recomputed from MouseHandler#xpos/ypos on every rendered frame, and a step only runs
                // after one - but a failure there reads as "hover is broken". This says which half.
                .step("Minecraft reads the cursor the harness drives", ctx -> {
                    var window = ctx.mc().getWindow();
                    var reported = ctx.mc().mouseHandler.xpos() * window.getGuiScaledWidth()
                            / Math.max(1, window.getScreenWidth());
                    var expected = ctx.el("#restart_button").bounds().centerX();
                    ctx.check("xpos() reports the hovered element's centre",
                            Math.abs(reported - expected) <= 1.0, expected, reported);
                })
                .screenshotElement("02_restart_hovered", "#restart_button")

                // click -> TestMCP's MOUSE_DOWN listener resets the game and moves focus to the parent
                .click("#restart_button")
                .waitForTextContains("#status_label", "Running")
                .checkText("#score_label", "Score: 0")
                .check("focus moved off the button",
                        ctx -> ctx.requireUI().getFocusedElement() != ctx.el("#restart_button").element())

                // keyboard -> #root owns the KEY_DOWN listener. The game advances on client TICKs,
                // not frames, so the wait has to be in ticks.
                .focus("#root")
                .checkFocused("#root")
                .key(Keys.UP)
                .ticks(8)
                .key(Keys.RIGHT)
                .ticks(8)
                .check("the snake is still alive after two turns",
                        ctx -> ctx.el("#status_label").text().contains("Running"))
                .screenshot("03_after_input")

                // the board really is being redrawn, not just sitting still
                .step("some cells differ from the empty style", ctx -> {
                    var distinct = ctx.all("#board *").stream()
                            .map(ref -> ref.element().getStyle().backgroundTexture())
                            .distinct()
                            .count();
                    ctx.check("the board uses more than one cell texture", distinct > 1, "> 1", distinct);
                })

                .closeScreen()
                .check("the screen closed", ctx -> ctx.screen() == null)
                // Closing back into the world is what makes Minecraft#setScreen call
                // MouseHandler#grabMouse, which would hide and capture the pointer of whoever is at
                // the machine. Cancelled for the duration of a run; this is the guard on that.
                .check("closing the screen did not capture the physical pointer",
                        ctx -> !ctx.mc().mouseHandler.isMouseGrabbed());
    }
}
