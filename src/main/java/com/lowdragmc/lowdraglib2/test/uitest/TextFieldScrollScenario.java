package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.utils.TextUtilities;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import dev.vfyjxf.taffy.style.FlexDirection;

/**
 * A text field too narrow for its text shows the start of it unless it is being edited, and never
 * keeps a scroll the text no longer needs once it has room.
 */
@LDLRegisterClient(name = "text_field_scroll", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class TextFieldScrollScenario implements UIScenario {

    private static final String FIELD = "field";
    private static final String NUMBER = "number";
    private static final String EXPECTED_CURSOR = "expected_cursor";
    private static final String TEXT = "Leading words matter most, the tail far less";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("text", "scroll").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("a narrow text field holding more than fits", TextFieldScrollScenario::buildUI)
                .awaitModularUI()
                .frames(3)
                .check("the text does not fit", ctx -> textWidth(field(ctx)) > field(ctx).getContentWidth())
                .check("unfocused, it shows its start", ctx -> field(ctx).getDisplayOffset() == 0)
                .check("a number shows its leading digits", ctx -> textWidth(number(ctx)) > number(ctx).getContentWidth()
                        && number(ctx).getDisplayOffset() == 0)

                .group("focused from code, the cursor at the end is brought into view", g -> g
                        .step("focus it", ctx -> field(ctx).focus())
                        .frames(2)
                        .check("scrolled to the end", ctx -> near(field(ctx).getDisplayOffset(), maxOffset(field(ctx))))
                        .check("which is a real scroll", ctx -> field(ctx).getDisplayOffset() > 0))

                .group("blurred, it goes back to the start", g -> g
                        .step("blur it", ctx -> ctx.requireUI().clearFocus())
                        .frames(2)
                        .check("at the start", ctx -> field(ctx).getDisplayOffset() == 0))

                // Focus arrives before the press: scrolling on focus would move the text under the pointer.
                .group("clicking an unfocused field puts the cursor where it was clicked", g -> g
                        .clickAt("the third character", ctx -> {
                            var field = field(ctx);
                            var x = field.getContentX() + (charX(field, 2) * 3 + charX(field, 3)) / 4;
                            ctx.put(EXPECTED_CURSOR, field.getCursorUnderMouseX(x));
                            return new float[]{x, field.getContentY() + field.getContentHeight() / 2};
                        }, Keys.MOUSE_LEFT)
                        .frames(2)
                        .check("it aimed between the second and third", ctx -> ctx.<Integer>get(EXPECTED_CURSOR) == 2)
                        .check("the field took the focus", ctx -> field(ctx).isFocused())
                        .check("the cursor is there", ctx -> field(ctx).getCursorPos() == 2)
                        .check("and the text did not move", ctx -> field(ctx).getDisplayOffset() == 0))

                .group("growing the field gives back what now fits", g -> g
                        .step("move the cursor to the end", ctx -> field(ctx).setCursor(TEXT.length()))
                        .check("scrolled to the end", ctx -> field(ctx).getDisplayOffset() > 0)
                        .step("widen it a little", ctx -> field(ctx).layout(layout -> layout.width(120)))
                        .frames(3)
                        .check("still too narrow", ctx -> textWidth(field(ctx)) > field(ctx).getContentWidth())
                        .check("scrolled only as far as the text needs", ctx -> near(field(ctx).getDisplayOffset(), maxOffset(field(ctx))))
                        .step("widen it to fit", ctx -> field(ctx).layout(layout -> layout.width(400)))
                        .frames(3)
                        .check("it fits", ctx -> textWidth(field(ctx)) < field(ctx).getContentWidth())
                        .check("so it shows from the start", ctx -> field(ctx).getDisplayOffset() == 0))

                .teardown("close the screen", ctx -> ctx.mc().setScreen(null));
    }

    private static ModularUI buildUI(TestContext ctx) {
        var root = new UIElement();
        root.layout(layout -> layout.widthPercent(100).heightPercent(100).paddingAll(10).gapAll(4)
                .flexDirection(FlexDirection.COLUMN));
        var field = new TextField();
        field.setAnyString();
        field.setText(TEXT);
        field.layout(layout -> layout.width(60));
        var number = new TextField();
        number.setNumbersOnlyLong(Long.MIN_VALUE, Long.MAX_VALUE);
        number.setValue("123456789012345678", false);
        number.layout(layout -> layout.width(40));
        root.addChildren(field, number);
        ctx.put(FIELD, field);
        ctx.put(NUMBER, number);
        return new ModularUI(UI.of(root), ctx.player());
    }

    private static float scale(TextField field) {
        return field.getTextFieldStyle().fontSize() / field.getFont().lineHeight;
    }

    private static float textWidth(TextField field) {
        return field.getFont().getSplitter().stringWidth(field.getStyledLine()) * scale(field);
    }

    private static float charX(TextField field, int column) {
        return field.getFont().getSplitter().stringWidth(
                TextUtilities.truncateStyled(field.getStyledLine(), column)) * scale(field);
    }

    /** The scroll that puts the end of the text, cursor included, at the right edge. */
    private static float maxOffset(TextField field) {
        return Math.max(textWidth(field) - field.getContentWidth() + 1, 0);
    }

    private static boolean near(float a, float b) {
        return Math.abs(a - b) < 0.01f;
    }

    private static TextField field(TestContext ctx) {
        return ctx.get(FIELD);
    }

    private static TextField number(TestContext ctx) {
        return ctx.get(NUMBER);
    }
}
