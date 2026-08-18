package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.ui.BooleanConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ColorConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Inspector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.world.item.Items;

/** Captures components that are not represented by the general component example screen. */
@LDLRegisterClient(name = "wiki_component_special", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class WikiComponentSpecialScenario implements UIScenario {

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(120).tags("wiki", "components", "visual").requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        capture(s, "element", WikiComponentSpecialScenario::elementUI);
        capture(s, "bindable-value", WikiComponentSpecialScenario::bindableValueUI);
        capture(s, "inventory-slots", WikiComponentSpecialScenario::inventorySlotsUI);
        capture(s, "inspector", WikiComponentSpecialScenario::inspectorUI);
    }

    private static void capture(ScenarioBuilder s, String slug,
                                java.util.function.Function<TestContext, ModularUI> factory) {
        s.openModularUI(slug + " wiki capture", factory)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#" + slug + "_demo")
                .screenshotElement(slug, "#" + slug + "_demo")
                .closeScreen();
    }

    private static ModularUI elementUI(TestContext ctx) {
        var panel = panel("element_demo", "UIElement — container, layout and style");
        var row = new UIElement();
        row.layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(8));
        row.addChildren(
                elementBox("Header", "fixed width", ColorPattern.BLUE),
                elementBox("Content", "flex: 1", ColorPattern.GREEN)
                        .layout(layout -> layout.flex(1).height(80)),
                elementBox("Actions", "nested children", ColorPattern.ORANGE)
        );
        panel.layout(layout -> layout.width(460));
        panel.addChildren(row, new Label().setText("Every visible LDLib2 component is a UIElement subtree."));
        return modular(ctx, panel);
    }

    private static UIElement elementBox(String title, String detail, ColorPattern color) {
        var box = new UIElement().addClass("preview_bg");
        box.layout(layout -> layout.width(120).height(80).paddingAll(6).gapAll(4));
        box.addChildren(
                new UIElement().layout(layout -> layout.widthPercent(100).height(8))
                        .style(style -> style.backgroundTexture(color.rectTexture())),
                new Label().setText(title),
                new Label().setText(detail).textStyle(style -> style.textColor(0xffaaaaaa))
        );
        return box;
    }

    private static ModularUI bindableValueUI(TestContext ctx) {
        var value = new BindableValue<>(72);
        value.setId("shared_value");

        var progress = new ProgressBar().setValue(0.72f);
        progress.layout(layout -> layout.width(210));
        progress.label(label -> label.setText("Progress: 72%"));

        var flow = new UIElement();
        flow.layout(layout -> layout.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapAll(10));
        flow.addChildren(
                boxLabel("BindableValue<Integer>", "72"),
                new Label().setText("→").textStyle(style -> style.fontSize(20)),
                boxLabel("Label consumer", "72%"),
                progress
        );

        var panel = panel("bindable-value_demo", "BindableValue — one invisible value, multiple consumers");
        panel.addChildren(value, flow,
                new Label().setText("The BindableValue itself takes no visual space."));
        return modular(ctx, panel);
    }

    private static UIElement boxLabel(String title, String value) {
        var box = new UIElement().addClass("preview_bg");
        box.layout(layout -> layout.paddingAll(6).gapAll(3).width(135));
        box.addChildren(
                new Label().setText(title).textStyle(style -> style.textColor(0xff88ccff)),
                new Label().setText(value).textStyle(style -> style.fontSize(18))
        );
        return box;
    }

    private static ModularUI inventorySlotsUI(TestContext ctx) {
        var slots = new InventorySlots();
        slots.hotbar.slots[0].setItem(Items.DIAMOND_PICKAXE.getDefaultInstance());
        slots.hotbar.slots[1].setItem(Items.TORCH.getDefaultInstance().copyWithCount(32));
        slots.hotbar.slots[2].setItem(Items.COOKED_BEEF.getDefaultInstance().copyWithCount(8));
        slots.rows[2].slots[4].setItem(Items.REDSTONE.getDefaultInstance().copyWithCount(16));
        slots.rows[2].slots[5].setItem(Items.DIAMOND.getDefaultInstance().copyWithCount(5));

        var panel = panel("inventory-slots_demo", "InventorySlots — 36 slots");
        panel.addChild(slots);
        return modular(ctx, panel);
    }

    private static ModularUI inspectorUI(TestContext ctx) {
        var name = new String[]{"Example Node"};
        var visible = new boolean[]{true};
        var opacity = new Number[]{0.85f};
        var color = new int[]{0xff4fa3f0};

        var configurable = IConfigurable.create(group -> group.addConfigurators(
                new StringConfigurator("Name", () -> name[0], value -> name[0] = value, name[0], true),
                new BooleanConfigurator("Visible", () -> visible[0], value -> visible[0] = value, true, true),
                new NumberConfigurator("Opacity", () -> opacity[0], value -> opacity[0] = value, 1f, true)
                        .setRange(0, 1),
                new ColorConfigurator("Color", () -> color[0], value -> color[0] = value, color[0], true)
        ));

        var inspector = new Inspector();
        inspector.layout(layout -> layout.width(360).height(230));
        inspector.inspect(configurable);

        var panel = panel("inspector_demo", "Inspector — generated property editors");
        panel.addChild(inspector);
        return modular(ctx, panel);
    }

    private static UIElement panel(String id, String title) {
        var root = new UIElement().setId(id).addClass("panel_bg");
        root.layout(layout -> layout.paddingAll(10).gapAll(8).widthAuto().heightAuto());
        root.addChild(new Label().setText(title)
                .textStyle(style -> style.fontSize(12).textColor(0xff88ccff).textShadow(true)));
        return root;
    }

    private static ModularUI modular(TestContext ctx, UIElement panel) {
        var root = new UIElement();
        root.layout(layout -> layout.widthPercent(100).heightPercent(100)
                .alignItems(AlignItems.CENTER).justifyContent(AlignContent.CENTER));
        root.addChild(panel);
        return new ModularUI(UI.of(root,
                StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.GDP)), ctx.player());
    }
}
