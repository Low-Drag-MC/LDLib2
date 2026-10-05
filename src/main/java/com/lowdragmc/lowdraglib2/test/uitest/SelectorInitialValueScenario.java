package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.ui.TestSelectorInitialValue;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;

/**
 * A menu-synced {@link Selector} in a content-sized {@link com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView}
 * shows its initial value: the preview label keeps the preview's width across the scroller's second layout pass.
 */
@LDLRegisterClient(name = "selector_initial_value", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class SelectorInitialValueScenario implements UIScenario {

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(30).tags("sync").requiresWorld(true).guiScale(3);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.server("open ui_selector_initial_value", sc ->
                        PlayerUIMenuType.openUI(sc.player(), LDLib2.id("ui_selector_initial_value")))
                .awaitScreen(ModularUIContainerScreen.class)
                .awaitModularUI()
                .awaitElement("#preset")
                .frames(5)
                .screenshot("01_opened")
                .checkEquals("the bind-only selector holds the value", TestSelectorInitialValue.Mode.ORDERED,
                        ctx -> selector(ctx, "#bind_only").getValue())
                .checkEquals("the bind-only selector's preview says it", "ORDERED", ctx -> previewText(ctx, "#bind_only"))
                .check("the bind-only selector's preview label fills the preview", ctx -> labelFills(ctx, "#bind_only"))
                .checkEquals("the preset selector holds the value", TestSelectorInitialValue.Mode.ORDERED,
                        ctx -> selector(ctx, "#preset").getValue())
                .checkEquals("the preset selector's preview says it", "ORDERED", ctx -> previewText(ctx, "#preset"))
                .check("the preset selector's preview label fills the preview", ctx -> labelFills(ctx, "#preset"))
                .closeScreen()
                .teardown("close the container", ctx -> ctx.requirePlayer().closeContainer());
    }

    @SuppressWarnings("unchecked")
    private static Selector<TestSelectorInitialValue.Mode> selector(TestContext ctx, String sel) {
        return (Selector<TestSelectorInitialValue.Mode>) ctx.el(sel).element();
    }

    private static String previewText(TestContext ctx, String sel) {
        var children = selector(ctx, sel).preview.getChildren();
        if (children.isEmpty()) return "<no preview child>";
        return children.getFirst() instanceof TextElement text ? text.getText().getString() : "<not text>";
    }

    private static boolean labelFills(TestContext ctx, String sel) {
        var preview = selector(ctx, sel).preview;
        if (preview.getChildren().isEmpty()) return false;
        return Math.abs(preview.getChildren().getFirst().getSizeWidth() - preview.getContentWidth()) < 0.5f;
    }
}
