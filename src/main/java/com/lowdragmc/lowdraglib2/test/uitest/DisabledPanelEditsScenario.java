package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ArrayConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.ColorConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.IGuiTextureConfigurator;
import com.lowdragmc.lowdraglib2.editor.ClipboardManager;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Inspector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Menu;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SearchComponent;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ElementBounds;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** A disabled panel still takes clicks, but none of the ways its configurators open to set a value. */
@LDLRegisterClient(name = "disabled_panel_edits", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class DisabledPanelEditsScenario implements UIScenario {

    private static final String PANEL = "panel";
    private static final String FIELDS = "fields";
    private static final String SEARCH = "search";
    private static final String PRESSES = "presses";
    private static final String ORDER = "order";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("configurator", "inspector").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("an inspector and a search box in one panel", DisabledPanelEditsScenario::buildUI)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the inspector has laid out", ctx -> color(ctx).getSizeHeight() > 0)
                .step("put a texture on the clipboard",
                        ctx -> ClipboardManager.INSTANCE.copyDirect(new ColorRectTexture(0xFF0000FF)))
                .step("expand the list", ctx -> ctx.query().type(ArrayConfiguratorGroup.class).one()
                        .as(ArrayConfiguratorGroup.class).setCollapse(false))
                .settleMs(150)

                .group("an enabled panel edits", g -> attempts(g, true))
                .step("disable the panel", ctx -> {
                    panel(ctx).setActive(false);
                    ctx.<AtomicInteger>get(PRESSES).set(0);
                })
                .group("a disabled panel refuses", g -> attempts(g, false)
                        .check("the presses still reached the panel", ctx -> ctx.<AtomicInteger>get(PRESSES).get() > 0))
                .closeScreen();
    }

    private static ScenarioBuilder attempts(ScenarioBuilder g, boolean enabled) {
        var drag = new float[3];
        return g
                .step("click the colour swatch", ctx -> press(ctx, color(ctx).colorPreview, Keys.MOUSE_LEFT))
                .check("the colour picker opens: " + enabled, ctx -> (color(ctx).colorSelector.getParent() != null) == enabled)
                .step("close it", ctx -> color(ctx).hide())

                .step("click the texture preview", ctx -> press(ctx, texture(ctx).preview, Keys.MOUSE_LEFT))
                .settleMs(150)
                .check("the texture dialog opens: " + enabled, ctx -> dialogShown(ctx) == enabled)
                .step("close it", ctx -> {
                    for (var dialog : ctx.query().type(Dialog.class).list()) dialog.as(Dialog.class).close();
                })
                .settleMs(100)

                .step("right-click the texture row", ctx -> press(ctx, texture(ctx).label, Keys.MOUSE_RIGHT))
                .settleMs(150)
                .check("the menu offers paste: " + enabled, ctx -> menuShows(ctx, "Paste (") == enabled)
                .check("and remove: " + enabled, ctx -> menuShows(ctx, "Remove") == enabled)
                .step("close it", ctx -> {
                    for (var menu : ctx.query().type(Menu.class).list()) {
                        var element = menu.as(Menu.class);
                        if (element.getParent() != null) element.getParent().removeChild(element);
                    }
                })

                .step("press the first list item's handle", ctx -> {
                    ctx.put(ORDER, List.copyOf(fields(ctx).list));
                    var items = ctx.query().type(ArrayConfiguratorGroup.ItemConfigurator.class).list();
                    var handle = ElementBounds.of(items.getFirst().as(ArrayConfiguratorGroup.ItemConfigurator.class).label);
                    var last = items.getLast().bounds();
                    drag[0] = handle.centerX();
                    drag[1] = handle.centerY();
                    drag[2] = last.y() + last.height() + 4;
                    ctx.input().mouseDown(drag[0], drag[1], Keys.MOUSE_LEFT);
                })
                .step("pull it", ctx -> ctx.input().dragTo(drag[0], drag[1] + 3, Keys.MOUSE_LEFT))
                .step("past the last item", ctx -> ctx.input().dragTo(drag[0], drag[2], Keys.MOUSE_LEFT))
                .step("drop", ctx -> ctx.input().mouseUp(drag[0], drag[2], Keys.MOUSE_LEFT))
                .check("the list is reordered: " + enabled, ctx -> !fields(ctx).list.equals(ctx.get(ORDER)) == enabled)

                .step("click the search box", ctx -> press(ctx, search(ctx), Keys.MOUSE_LEFT))
                .settleMs(100)
                .check("the search results open: " + enabled, ctx -> search(ctx).isOpen() == enabled)
                .step("close them", ctx -> {
                    ctx.requireUI().clearFocus();
                    search(ctx).hide();
                })
                .settleMs(100);
    }

    public static class Fields implements IConfigurable {
        @Configurable
        @ConfigColor
        public int color = 0xFFFF0000;
        @Configurable
        public IGuiTexture texture = new ColorRectTexture(0xFF00FF00);
        @Configurable
        public List<Integer> list = new ArrayList<>(List.of(1, 2, 3));
    }

    private static ModularUI buildUI(TestContext ctx) {
        var fields = new Fields();
        var presses = new AtomicInteger();
        ctx.put(FIELDS, fields);
        ctx.put(PRESSES, presses);

        var inspector = new Inspector();
        inspector.layout(layout -> layout.widthPercent(100).flex(1));
        inspector.inspect(fields);
        var search = new SearchComponent<String>();
        search.layout(layout -> layout.widthPercent(100).height(14));
        ctx.put(SEARCH, search);

        var panel = new UIElement();
        panel.layout(layout -> layout.width(220).heightPercent(100));
        panel.addEventListener(UIEvents.MOUSE_DOWN, e -> presses.incrementAndGet(), true);
        panel.addChildren(inspector, search);
        ctx.put(PANEL, panel);

        var root = new UIElement();
        root.layout(layout -> layout.widthPercent(100).heightPercent(100));
        return new ModularUI(UI.of(root.addChild(panel)), ctx.player());
    }

    private static void press(TestContext ctx, UIElement element, int button) {
        var bounds = ElementBounds.of(element);
        ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), button);
        ctx.input().mouseUp(bounds.centerX(), bounds.centerY(), button);
    }

    private static boolean dialogShown(TestContext ctx) {
        return !ctx.query().type(Dialog.class).list().isEmpty();
    }

    private static boolean menuShows(TestContext ctx, String text) {
        return ctx.query().type(TextElement.class).list().stream()
                .anyMatch(ref -> ref.as(TextElement.class).getText().getString().contains(text));
    }

    private static UIElement panel(TestContext ctx) {
        return ctx.get(PANEL);
    }

    private static Fields fields(TestContext ctx) {
        return ctx.get(FIELDS);
    }

    private static SearchComponent<?> search(TestContext ctx) {
        return ctx.get(SEARCH);
    }

    private static ColorConfigurator color(TestContext ctx) {
        return ctx.query().type(ColorConfigurator.class).one().as(ColorConfigurator.class);
    }

    private static IGuiTextureConfigurator texture(TestContext ctx) {
        return ctx.query().type(IGuiTextureConfigurator.class).one().as(IGuiTextureConfigurator.class);
    }
}
