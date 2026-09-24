package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.window.ModularUIWindow;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.PreeditEvent;
import org.lwjgl.sdl.SDLKeyboard;

/**
 * Typing into an LDLib2 text field, which under SDL depends on something no other scenario covers.
 *
 * <p>SDL delivers no typed characters to a window — and runs no input method for it — until text input
 * is started for that window. A text field that never starts it looks perfectly fine and receives
 * nothing at all. The rest of the harness types through {@code Screen#charTyped} directly, which is
 * below that switch, so without this scenario the whole library could lose the keyboard and every other
 * test would still pass. So this checks the switch itself, on the game window and on a window of the
 * UI's own, along with an input method's composition reaching the field and a character outside the
 * basic multilingual plane arriving whole.
 */
@LDLRegisterClient(name = "text_input_ime", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class TextInputScenario implements UIScenario {

    private static final String WINDOW = "text_input_window";
    private static final String WINDOW_FIELD = "text_input_window_field";
    private static final String EMOJI = new String(Character.toChars(0x1F600));

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("text", "input", "ime", "window").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("a text field", ctx -> new ModularUI(UI.of(root("field")), ctx.player()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#field")
                .check("text input is off before anything is focused",
                        ctx -> !SDLKeyboard.SDL_TextInputActive(gameWindow(ctx)))

                .group("focusing the field turns text input on", g -> g
                        .click("#field")
                        .frames(2)
                        .check("the field has the focus", ctx -> field(ctx).isFocused())
                        .check("SDL is delivering text to the game window",
                                ctx -> SDLKeyboard.SDL_TextInputActive(gameWindow(ctx)))
                        .check("the screen reports its input as captured, holding back global key bindings",
                                ctx -> ctx.screen().isInputCaptured()))

                .group("an input method's composition reaches the field", g -> g
                        .step("compose 'ni'", ctx -> ctx.screen().preeditUpdated(PreeditEvent.fromSdlTextEditing("ni", 2, 0)))
                        .frames(2)
                        .check("the field holds the composition", ctx -> field(ctx).getComposition() instanceof PreeditEvent)
                        .check("and has not inserted it", ctx -> field(ctx).getRawText().isEmpty())
                        .screenshot("01_composing"))

                .group("committing ends the composition and types the text", g -> g
                        .step("commit", ctx -> commit(ctx, "你好" + EMOJI))
                        .frames(2)
                        .check("the composition is gone", ctx -> field(ctx).getComposition() == null)
                        .check("the committed text, supplementary character included, is in the field",
                                ctx -> field(ctx).getRawText().equals("你好" + EMOJI))
                        .screenshot("02_committed"))

                .group("losing the focus turns text input off", g -> g
                        .blur()
                        .frames(2)
                        .check("SDL is no longer delivering text", ctx -> !SDLKeyboard.SDL_TextInputActive(gameWindow(ctx)))
                        .check("and the input is no longer captured", ctx -> !ctx.screen().isInputCaptured()))

                .group("a field in a window of its own switches that window", g -> g
                        .step("open a window with a field in it", ctx -> {
                            var ui = new ModularUI(UI.of(root("window_field")));
                            var window = new ModularUIWindow(ui, "Text Input");
                            ctx.require("the window opened",
                                    window.open(Integer.MIN_VALUE, Integer.MIN_VALUE, 480, 240, false));
                            ctx.put(WINDOW, window);
                        })
                        .frames(10)
                        .waitUntil("its field has laid out",
                                ctx -> ctx.in(window(ctx).getModularUI(), "#window_field").count() > 0)
                        .step("focus it", ctx -> {
                            var ui = window(ctx).getModularUI();
                            var field = ctx.in(ui, "#window_field").one().as(TextField.class);
                            ctx.put(WINDOW_FIELD, field);
                            ui.requestFocus(field);
                        })
                        .frames(2)
                        .check("SDL is delivering text to that window",
                                ctx -> SDLKeyboard.SDL_TextInputActive(window(ctx).window().handle()))
                        .check("and not to the game window",
                                ctx -> !SDLKeyboard.SDL_TextInputActive(gameWindow(ctx)))
                        .step("compose in it", ctx -> ctx.input(window(ctx))
                                .preedit(PreeditEvent.fromSdlTextEditing("hao", 3, 0)))
                        .frames(3)
                        .check("its field holds the composition", ctx -> windowField(ctx).getComposition() instanceof PreeditEvent)
                        .step("commit in it", ctx -> ctx.input(window(ctx)).text("好"))
                        .frames(3)
                        .check("the window's field received the text", ctx -> windowField(ctx).getRawText().equals("好"))
                        .check("and its composition ended", ctx -> windowField(ctx).getComposition() == null)
                        .step("close the window", ctx -> window(ctx).close())
                        .frames(2)
                        .check("the window is gone", ctx -> !window(ctx).isOpen()))

                .closeScreen();
    }

    /** What the game's KeyboardHandler does with committed text: end the composition, then type it. */
    private static void commit(TestContext ctx, String text) {
        var screen = ctx.screen();
        screen.preeditUpdated(null);
        text.codePoints().forEach(codePoint -> screen.charTyped(new CharacterEvent(codePoint)));
    }

    private static UIElement root(String fieldId) {
        var root = new UIElement();
        root.layout(layout -> layout.widthPercent(100).heightPercent(100).paddingAll(20));
        var field = new TextField();
        field.setId(fieldId);
        field.layout(layout -> layout.width(160).height(16));
        root.addChild(field);
        return root;
    }

    private static long gameWindow(TestContext ctx) {
        return ctx.mc().getWindow().handle();
    }

    private static TextField field(TestContext ctx) {
        return ctx.el("#field").as(TextField.class);
    }

    private static ModularUIWindow window(TestContext ctx) {
        return ctx.get(WINDOW);
    }

    private static TextField windowField(TestContext ctx) {
        return ctx.get(WINDOW_FIELD);
    }
}
