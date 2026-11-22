package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.configurator.ui.HDRColorConfigurator;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SearchComponent;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.math.HDRColor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.NodeElement;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.OptionTestNode;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestGraph;
import com.lowdragmc.lowdraglib2.uitest.ElementBounds;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.List;

/**
 * Root-mounted dropdowns under a scaled ancestor, and long content in a dropdown's preview.
 *
 * <p>The dialog of a {@link Selector}, {@link SearchComponent} or {@link HDRColorConfigurator} lives
 * outside its anchor's transform, so it must be sized from the anchor as drawn: never under 50, never
 * wider than the screen, and pushed onto the screen in one layout pass. A preview must keep whatever
 * the candidate UI draws inside itself, even content that sizes itself to its text.
 */
@LDLRegisterClient(name = "ngt_selector_dialog_scale", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class NgtSelectorDialogScaleScenario implements UIScenario {

    private static final String LONG_TEXT = "A candidate whose name is far too long to fit";
    private static final List<String> CANDIDATES = List.of(LONG_TEXT, "short");
    private static final List<String> SELECTOR_IDS = List.of("#plain", "#icon_text", "#adaptive", "#fixed");

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("graph", "ngt", "selector").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        var watch = new LayoutWatch();
        s.openModularUI("graph editor beside a column of dropdowns", NgtSelectorDialogScaleScenario::buildUI)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#graph")
                .waitUntil("the node is laid out", ctx -> ctx.count(".__node-element__") == 1)
                .step("give the node's selector a long candidate name", ctx -> nodeSelector(ctx)
                        .setCandidateUIProvider(UIElementProvider.text(v -> Component.literal(LONG_TEXT + " (" + v + ")"))))
                .settleMs(150)

                .group("the preview keeps its content clear of the arrow", g -> {
                    for (var id : SELECTOR_IDS) {
                        g.step("measure " + id, ctx -> checkSelectorPreview(ctx, id, (Selector<?>) ctx.el(id).element()));
                    }
                    g.step("measure #search", ctx -> {
                        var search = ctx.el("#search").as(SearchComponent.class);
                        checkPreview(ctx, "#search", search.preview, ElementBounds.of(search).right());
                    });
                    g.screenshotElement("01_previews", "#column");
                })
                .group("hovering rolls the text inside the preview", g -> g
                        .hover("#plain")
                        .settleMs(400)
                        .screenshotElement("02_rolling_plain", "#column")
                        .hover("#adaptive")
                        .settleMs(400)
                        .screenshotElement("03_rolling_adaptive", "#column"))

                .group("under a scale(2) ancestor", g -> g
                        .step("open the hdr color dialog", ctx -> ctx.el("#hdr").as(HDRColorConfigurator.class).show())
                        .settleMs(120)
                        .step("measure the hdr color dialog", ctx -> {
                            var hdr = ctx.el("#hdr").as(HDRColorConfigurator.class);
                            checkDialogWidth(ctx, "hdr color", hdr, hdr.dialog);
                        })
                        .step("close the hdr color dialog", ctx -> ctx.el("#hdr").as(HDRColorConfigurator.class).hide())
                        .step("open the selector wider than the screen", ctx -> selector(ctx, "#wide").show())
                        .settleMs(120)
                        .step("measure its dialog", ctx -> {
                            var wide = selector(ctx, "#wide");
                            var screen = (float) wide.getModularUI().getScreenWidth();
                            ctx.check("the selector is drawn wider than the screen", ElementBounds.of(wide).width() > screen,
                                    "> " + screen, ElementBounds.of(wide).width());
                            checkDialogWidth(ctx, "wide selector", wide, wide.dialog);
                        })
                        .step("watch the dialog's layout", ctx -> watch.start(selector(ctx, "#wide").dialog))
                        .frames(3)
                        .step("the dialog has settled", ctx -> watch.verify(ctx, "wide selector"))
                        .screenshot("04_wide_selector")
                        .step("close it", ctx -> selector(ctx, "#wide").hide())
                        .step("clear the scaled host off the graph", ctx -> ctx.el("#scaled").element().setDisplay(false)))

                .group("at the bottom edge, the dialog is lifted onto the screen", g -> g
                        .step("open the bottom selector", ctx -> selector(ctx, "#bottom").show())
                        .settleMs(120)
                        .step("measure its dialog", ctx -> {
                            var bottom = selector(ctx, "#bottom");
                            var screen = (float) bottom.getModularUI().getScreenHeight();
                            var bounds = ElementBounds.of(bottom.dialog);
                            ctx.check("the dialog would not fit below the selector",
                                    ElementBounds.of(bottom).bottom() + bounds.height() > screen,
                                    "> " + screen, ElementBounds.of(bottom).bottom() + bounds.height());
                            ctx.check("the dialog stays on screen", bounds.y() >= -0.5f && bounds.bottom() <= screen + 0.5f,
                                    "[0, " + screen + "]", "[" + bounds.y() + ", " + bounds.bottom() + "]");
                            checkDialogWidth(ctx, "bottom selector", bottom, bottom.dialog);
                        })
                        .step("watch the dialog's layout", ctx -> watch.start(selector(ctx, "#bottom").dialog))
                        .frames(3)
                        .step("the dialog has settled", ctx -> watch.verify(ctx, "bottom selector"))
                        .screenshot("05_bottom_selector")
                        .step("close it", ctx -> selector(ctx, "#bottom").hide()))

                .group("zoomed in, the node's dialogs match their anchors", g -> g
                        .step("zoom to 2 (the toolkit's max) on the node", ctx -> zoomOnNode(ctx, 2f))
                        .settleMs(150)
                        .step("measure the node's selector", ctx -> checkSelectorPreview(ctx, "node selector", nodeSelector(ctx)))
                        .step("open the selector", ctx -> nodeSelector(ctx).show())
                        .settleMs(120)
                        .step("measure the selector dialog", ctx -> {
                            var selector = nodeSelector(ctx);
                            checkDialogWidth(ctx, "selector", selector, selector.dialog);
                        })
                        .screenshot("06_zoomed_selector")
                        .step("close the selector", ctx -> nodeSelector(ctx).hide())
                        .step("open the search component", ctx -> nodeSearch(ctx).show())
                        .settleMs(120)
                        .step("measure the search dialog", ctx -> {
                            var search = nodeSearch(ctx);
                            checkDialogWidth(ctx, "search component", search, search.dialog);
                        })
                        .screenshot("07_zoomed_search")
                        .step("close the search component", ctx -> nodeSearch(ctx).hide()))

                .group("zoomed out, the dialog keeps its floor", g -> g
                        .step("zoom to 0.4 on the node", ctx -> zoomOnNode(ctx, 0.4f))
                        .settleMs(150)
                        .step("open the selector", ctx -> nodeSelector(ctx).show())
                        .settleMs(120)
                        .step("measure the selector dialog", ctx -> {
                            var selector = nodeSelector(ctx);
                            ctx.check("the selector is drawn under the floor", ElementBounds.of(selector).width() < 50,
                                    "< 50", ElementBounds.of(selector).width());
                            checkDialogWidth(ctx, "selector", selector, selector.dialog);
                        })
                        .screenshot("08_zoomed_out_selector")
                        .step("close the selector", ctx -> nodeSelector(ctx).hide()))

                .closeScreen();
    }

    private static void checkSelectorPreview(TestContext ctx, String name, Selector<?> selector) {
        var icon = ElementBounds.of(selector.buttonIcon);
        checkPreview(ctx, name, selector.preview, icon.x());
        ctx.check(name + ": the arrow stays inside the selector", icon.right() <= ElementBounds.of(selector).right() + 0.5f,
                "<= " + ElementBounds.of(selector).right(), icon.right());
    }

    private static void checkPreview(TestContext ctx, String name, UIElement preview, float limit) {
        var bounds = ElementBounds.of(preview);
        var content = preview.getChildren().isEmpty() ? null : ElementBounds.of(preview.getChildren().getFirst());
        ctx.log("%s: preview [%.1f, %.1f] content %s limit %.1f".formatted(name, bounds.x(), bounds.right(),
                content == null ? "-" : "[%.1f, %.1f]".formatted(content.x(), content.right()), limit));
        ctx.check(name + ": the preview clips what it holds", !preview.getStyle().overflowVisible());
        ctx.check(name + ": the preview ends before " + limit, bounds.right() <= limit + 0.5f, "<= " + limit, bounds.right());
        ctx.check(name + ": what it holds ends inside the preview", content != null && content.right() <= bounds.right() + 0.5f,
                "<= " + bounds.right(), content == null ? "-" : content.right());
    }

    private static void checkDialogWidth(TestContext ctx, String name, UIElement anchor, UIElement dialog) {
        var drawn = ElementBounds.of(anchor).width();
        var screen = (float) anchor.getModularUI().getScreenWidth();
        var expected = Math.max(Math.min(drawn, screen), 50f);
        var bounds = ElementBounds.of(dialog);
        ctx.log("%s: drawn %.1f, layout %.1f, screen %.1f, dialog [%.1f, %.1f]".formatted(name, drawn,
                anchor.getSizeWidth(), screen, bounds.x(), bounds.right()));
        ctx.check(name + ": the dialog is as wide as the drawn anchor, within [50, screen]",
                Math.abs(bounds.width() - expected) < 1f, expected, bounds.width());
        ctx.check(name + ": the dialog stays on screen", bounds.x() >= -0.5f && bounds.right() <= screen + 0.5f,
                "[0, " + screen + "]", "[" + bounds.x() + ", " + bounds.right() + "]");
    }

    private static void zoomOnNode(TestContext ctx, float scale) {
        var canvas = ctx.el("#graph").as(GraphView.class).graphView;
        var node = ctx.el(".__node-element__").element();
        var x = node.getPositionX() - canvas.getContentX() - 10;
        var y = node.getPositionY() - canvas.getContentY() - 10;
        canvas.fit(x, y, x + canvas.getContentWidth() / scale, y + canvas.getContentHeight() / scale, 0.01f);
        ctx.check("the canvas is at zoom " + scale, Math.abs(canvas.getScale() - scale) < 0.01f, scale, canvas.getScale());
    }

    @SuppressWarnings("unchecked")
    private static Selector<String> selector(TestContext ctx, String id) {
        return (Selector<String>) ctx.el(id).element();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Selector<Object> nodeSelector(TestContext ctx) {
        return (Selector) find(ctx.el(".__node-element__").as(NodeElement.class), Selector.class);
    }

    private static SearchComponent<?> nodeSearch(TestContext ctx) {
        return find(ctx.el(".__node-element__").as(NodeElement.class), SearchComponent.class);
    }

    private static <T extends UIElement> T find(UIElement root, Class<T> type) {
        var found = findOrNull(root, type);
        if (found == null) throw new IllegalStateException("the node has no " + type.getSimpleName());
        return found;
    }

    @Nullable
    private static <T extends UIElement> T findOrNull(UIElement element, Class<T> type) {
        if (type.isInstance(element)) return type.cast(element);
        for (var child : element.getChildren()) {
            var found = findOrNull(child, type);
            if (found != null) return found;
        }
        return null;
    }

    private static final class LayoutWatch {
        private int moves;
        private final UIEventListener counter = e -> moves++;
        @Nullable
        private UIElement target;

        void start(UIElement target) {
            stop();
            this.moves = 0;
            this.target = target;
            target.addEventListener(UIEvents.LAYOUT_CHANGED, counter);
        }

        void verify(TestContext ctx, String name) {
            stop();
            ctx.check("no layout pass moved the " + name + " dialog", moves == 0, 0, moves);
        }

        private void stop() {
            if (target != null) target.removeEventListener(UIEvents.LAYOUT_CHANGED, counter);
            target = null;
        }
    }

    private static Component text(@Nullable String value) {
        return Component.literal(value == null ? "---" : value);
    }

    private static Label adaptiveLabel(@Nullable String value) {
        return (Label) new Label()
                .textStyle(style -> style.adaptiveWidth(true).textWrap(TextWrap.HOVER_ROLL).textAlignVertical(Vertical.CENTER))
                .setText(text(value))
                .setOverflowVisible(false);
    }

    private static Selector<String> longSelector(String id, float width, UIElementProvider<String> provider) {
        var selector = new Selector<String>();
        selector.setId(id);
        selector.layout(layout -> layout.width(width));
        selector.setCandidateUIProvider(provider);
        selector.setCandidates(CANDIDATES);
        selector.setSelected(LONG_TEXT, false);
        return selector;
    }

    private static ModularUI buildUI(TestContext ctx) {
        var root = new UIElement().setId("root");
        root.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
            layout.flexDirection(FlexDirection.ROW);
        });

        var search = new SearchComponent<String>();
        search.setId("search");
        search.layout(layout -> layout.width(110));
        search.setCandidateUIProvider(NgtSelectorDialogScaleScenario::adaptiveLabel);
        search.setSelected(LONG_TEXT, false);

        var column = new UIElement().setId("column");
        column.layout(layout -> {
            layout.width(130);
            layout.paddingAll(10);
            layout.gapAll(6);
        });
        column.addChildren(
                longSelector("plain", 110, UIElementProvider.text(NgtSelectorDialogScaleScenario::text)),
                longSelector("icon_text", 110, UIElementProvider.iconText(v -> Icons.DOWN_ARROW_NO_BAR, NgtSelectorDialogScaleScenario::text)),
                longSelector("adaptive", 110, NgtSelectorDialogScaleScenario::adaptiveLabel),
                longSelector("fixed", 110, v -> new UIElement()
                        .layout(layout -> layout.width(160).height(10))
                        .addChild(new Label().setText(text(v)))),
                search);

        var hdr = new HDRColorConfigurator("hdr", HDRColor::white, color -> {}, HDRColor.white(), false);
        hdr.setId("hdr");
        hdr.layout(layout -> layout.width(60));
        var scaled = new UIElement().setId("scaled");
        scaled.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(10);
            layout.top(140);
            layout.gapAll(6);
        });
        scaled.transform(transform -> transform.pivot(0f, 0f).scale(2f));
        scaled.addChildren(hdr, longSelector("wide", 1100, UIElementProvider.text(NgtSelectorDialogScaleScenario::text)));

        var editor = new GraphView();
        editor.setId("graph");
        editor.layout(layout -> {
            layout.flex(1);
            layout.heightPercent(100);
        });
        var bottom = longSelector("bottom", 110, UIElementProvider.text(NgtSelectorDialogScaleScenario::text));
        bottom.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(10);
            layout.bottom(2);
        });
        root.addChildren(column, editor, scaled, bottom);

        var graph = new TestGraph();
        graph.graphModel.createNodeModel(new OptionTestNode(), new Vector2f(0, 0));
        editor.loadGraph(graph);
        return new ModularUI(UI.of(root), ctx.player());
    }
}
