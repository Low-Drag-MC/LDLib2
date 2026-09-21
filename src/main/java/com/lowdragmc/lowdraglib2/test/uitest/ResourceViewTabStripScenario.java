package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.editor.resource.Resource;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.view.ResourceTabLayout;
import com.lowdragmc.lowdraglib2.editor.ui.view.ResourceTabLayoutStore;
import com.lowdragmc.lowdraglib2.editor.ui.view.ResourceView;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Menu;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Tab;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.TestEditor;
import com.lowdragmc.lowdraglib2.test.TestProject;
import com.lowdragmc.lowdraglib2.uitest.ElementRef;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The resource view's tab strip: resizing it, wrapping its tabs, dragging them into a new order,
 * hiding one from the strip's menu, and moving the whole strip to the top.
 *
 * <p>All of it is layout and hit testing against a strip whose flow axis changes underneath it, which
 * is exactly the kind of thing that cannot be checked without running it: the wrap only happens once
 * the strip is genuinely wide enough, and the drop index is worked out from where tabs actually
 * landed rather than from what the code intended.
 */
@LDLRegisterClient(name = "resource_view_tab_strip", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class ResourceViewTabStripScenario implements UIScenario {

    /** How far the seam is dragged to make room for a second tab per line. */
    private static final float WIDEN_BY = 22;
    /** Thick enough for two tabs to a line in every built-in theme. */
    private static final float WIDE_STRIP = 46;
    /** The classes {@code Menu} puts on its rows, which is how a row is found by its label. */
    private static final String LEAF_ROW = "__menu_leaf-node__";
    private static final String BRANCH_ROW = "__menu_branch-node__";
    /** Where the four placement shots start, so they read in order next to the ones before them. */
    private static final int FOUR_SIDES_FIRST_SHOT = 14;

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("editor", "resources", "layout");
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("editor", ctx -> new ModularUI(UI.of(new TestEditor()), ctx.player()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the editor has laid out", ctx -> editor(ctx).centerWindow.getSizeWidth() > 0)
                .step("show the resource view and load a project that fills it with tabs", ctx -> {
                    var view = resourceView(ctx);
                    var container = view.getViewContainer();
                    ctx.require("the resource view is docked", container != null);
                    container.selectView(view);
                    editor(ctx).loadProject(new TestProject(), null);
                })
                .waitUntil("the resource tabs are up", ctx -> !resourceView(ctx).getResourceTabs().isEmpty())
                // The arrangement is saved per editor, so a previous run of this very scenario would
                // otherwise decide what "a fresh strip" looks like.
                .step("start from a strip nobody has touched", ctx -> resourceView(ctx).resetTabLayout())

                .group("a fresh strip is one tab wide down the left", g -> g
                        .check("the tabs are on the left", ctx ->
                                resourceView(ctx).getPlacement() == ResourceTabLayout.Placement.LEFT)
                        .check("the strip is left of the content", ctx -> {
                            var view = resourceView(ctx);
                            return view.tabView.tabHeaderContainer.getPositionX() + view.tabView.tabHeaderContainer.getSizeWidth()
                                    <= view.tabView.tabContentContainer.getPositionX() + 0.5f;
                        })
                        .check("every tab is on a line of its own", ctx -> rows(ctx).size() == tabs(ctx).size())
                        .check("there is more than one tab to arrange", ctx -> tabs(ctx).size() > 2)
                        .step("no tab is cut off by the strip", ResourceViewTabStripScenario::checkTabsFit)
                        .step("name the strip so it can be captured on its own", ctx ->
                                resourceView(ctx).tabView.tabHeaderContainer.setId("resource_tab_header"))
                        .screenshot("01_left_default")
                        .screenshotElement("02_left_default_strip", "#resource_tab_header"))

                .group("a tab dragged onto another one takes the place after it", g -> {
                    var before = new ArrayList<String>();
                    captureOrder(g, before);
                    dragTabOntoTab(g, 0, 2, "03_left_drop_marker");
                    g.step("it landed straight after the tab it was dropped on", ctx -> {
                        var expected = List.of(before.get(1), before.get(2), before.get(0),
                                before.get(3), before.get(4));
                        var actual = names(resourceView(ctx).orderedResources());
                        ctx.check("it landed straight after the tab it was dropped on",
                                actual.equals(expected), expected, actual);
                    });
                    g.check("the dragged tab is showing again", ctx -> tabs(ctx).size() == before.size());
                    g.screenshot("04_left_reordered");
                })

                .group("dragging the seam widens the strip until the tabs wrap", g -> g
                        .step("aim at the seam between the strip and the content", ctx ->
                                ctx.input().moveTo(seamX(ctx), midY(ctx)))
                        .check("the pointer is on the resize handle", ctx ->
                                resourceView(ctx).isOverResizeHandle(seamX(ctx), midY(ctx)))
                        .screenshot("05_resize_handle")
                        .step("press the handle", ctx ->
                                ctx.input().mouseDown(seamX(ctx), midY(ctx), Keys.MOUSE_LEFT))
                        .step("pull it to the right", ctx ->
                                ctx.input().dragTo(seamX(ctx) + WIDEN_BY, midY(ctx), Keys.MOUSE_LEFT))
                        .step("let go", ctx -> ctx.input().mouseUp(seamX(ctx), midY(ctx), Keys.MOUSE_LEFT))
                        .check("the strip got wider", ctx ->
                                resourceView(ctx).getStripSize() > ResourceTabLayout.DEFAULT_STRIP_SIZE + 1)
                        .step("the content kept the rest of the width", ctx -> {
                            var view = resourceView(ctx);
                            var header = view.tabView.tabHeaderContainer;
                            var content = view.tabView.tabContentContainer;
                            ctx.check("the strip and the content fill the view between them",
                                    Math.abs(header.getSizeWidth() + content.getSizeWidth()
                                            - view.tabView.getSizeWidth()) < 1.5f,
                                    view.tabView.getSizeWidth(),
                                    header.getSizeWidth() + content.getSizeWidth());
                            ctx.check("the content starts where the strip ends",
                                    Math.abs(content.getPositionX() - view.getStripSeam()) < 1.5f,
                                    view.getStripSeam(), content.getPositionX());
                        })
                        .check("two tabs now share a line", ctx -> rows(ctx).size() < tabs(ctx).size())
                        .screenshot("06_left_wide")
                        .screenshotElement("07_left_wide_strip", "#resource_tab_header"))

                .group("reordering still works once the tabs wrap", g -> {
                    var before = new ArrayList<String>();
                    captureOrder(g, before);
                    dragTabOntoTab(g, 4, 0, "08_wrapped_drop_marker");
                    g.step("the last tab moved in behind the first", ctx -> {
                        var expected = List.of(before.get(0), before.get(4), before.get(1),
                                before.get(2), before.get(3));
                        var actual = names(resourceView(ctx).orderedResources());
                        ctx.check("the last tab moved in behind the first",
                                actual.equals(expected), expected, actual);
                    });
                    g.screenshot("09_wrapped_reordered");
                })

                .group("the strip's menu hides resources without closing on every tick", g -> g
                        .step("right click the strip", ctx -> {
                            var view = resourceView(ctx);
                            var header = view.tabView.tabHeaderContainer;
                            // The bottom of the strip, which is empty once the tabs have wrapped, so the
                            // press is on the strip itself rather than on one of its tabs.
                            var x = header.getPositionX() + header.getSizeWidth() / 2f;
                            var y = header.getPositionY() + header.getSizeHeight() - 2;
                            ctx.input().moveTo(x, y);
                            ctx.input().mouseDown(x, y, Keys.MOUSE_RIGHT);
                            ctx.input().mouseUp(x, y, Keys.MOUSE_RIGHT);
                        })
                        .step("the menu offers the placements, the visible tabs, a fit and a reset", ctx -> {
                            var leaves = ctx.query().withClass(LEAF_ROW).count();
                            var branches = ctx.query().withClass(BRANCH_ROW).count();
                            // fit + reset with a divider between them and the branches above
                            ctx.check("the menu has its three leaves", leaves == 3, 3, leaves);
                            ctx.check("the long lists are behind submenus", branches == 2, 2, branches);
                        })
                        .screenshot("10_strip_menu")
                        .step("open the visible tabs submenu", ctx -> hoverSubmenu(ctx,
                                Component.translatable("editor.resources.visible_tabs").getString()))
                        .waitUntil("the submenu is up", ctx -> ctx.query().type(Menu.class).count() > 1)
                        .screenshot("11_visible_tabs_submenu")
                        .step("tick the second resource off", ctx -> {
                            var target = resourceView(ctx).orderedResources().get(1);
                            ctx.put("hidden_resource", target.getName());
                            clickMenuEntry(ctx, target.getDisplayName().getString());
                        })
                        .check("that resource's tab is out of the strip", ctx -> {
                            var view = resourceView(ctx);
                            var name = ctx.<String>get("hidden_resource");
                            var resource = view.orderedResources().stream()
                                    .filter(r -> r.getName().equals(name)).findFirst().orElse(null);
                            return resource != null && view.isResourceHidden(resource)
                                    && !view.getResourceTabs().get(resource).isDisplayed();
                        })
                        .check("it still has a place in the order", ctx ->
                                resourceView(ctx).orderedResources().size()
                                        == resourceView(ctx).getResourceTabs().size())
                        // The point of the submenu: a set of toggles is no use if the first tick puts
                        // the menu away, so both menus have to survive being clicked in.
                        .step("the menu and its submenu are both still open", ctx -> {
                            var menus = ctx.query().type(Menu.class).count();
                            ctx.check("ticking an entry left both menus open", menus > 1, "> 1", menus);
                        })
                        .step("tick a second resource off without reopening anything", ctx -> {
                            var target = resourceView(ctx).orderedResources().get(3);
                            ctx.put("second_hidden", target.getName());
                            clickMenuEntry(ctx, target.getDisplayName().getString());
                        })
                        .step("both are hidden and the menu is still up", ctx -> {
                            var view = resourceView(ctx);
                            var hidden = view.orderedResources().stream()
                                    .filter(view::isResourceHidden).map(Resource::getName).toList();
                            ctx.check("both resources are hidden", hidden.size() == 2,
                                    List.of(ctx.get("hidden_resource"), ctx.get("second_hidden")), hidden);
                            var menus = ctx.query().type(Menu.class).count();
                            ctx.check("the menu is still open", menus > 0, "> 0", menus);
                        })
                        .screenshot("12_two_hidden")
                        .step("put the second one back", ctx ->
                                clickMenuEntry(ctx, resourceView(ctx).getResourceTabs().keySet().stream()
                                        .filter(r -> r.getName().equals(ctx.<String>get("second_hidden")))
                                        .findFirst().orElseThrow().getDisplayName().getString()))
                        .check("only the first one is hidden now", ctx -> {
                            var view = resourceView(ctx);
                            return view.orderedResources().stream().filter(view::isResourceHidden).count() == 1;
                        })
                        .step("click away from the menu", ctx -> {
                            var content = resourceView(ctx).tabView.tabContentContainer;
                            var x = content.getPositionX() + content.getSizeWidth() / 2f;
                            var y = content.getPositionY() + content.getSizeHeight() / 2f;
                            ctx.input().moveTo(x, y);
                            ctx.input().mouseDown(x, y, Keys.MOUSE_LEFT);
                            ctx.input().mouseUp(x, y, Keys.MOUSE_LEFT);
                        })
                        .check("losing the focus to something else does close it", ctx ->
                                ctx.query().type(Menu.class).count() == 0)
                        .screenshot("13_hidden_tab"))

                .group("the strip goes on any of the four sides", g -> {
                    for (var placement : ResourceTabLayout.Placement.values()) {
                        var name = placement.styleName();
                        // Numbered off the enum so the four shots stay in placement order in the folder.
                        var index = FOUR_SIDES_FIRST_SHOT + placement.ordinal();
                        g.step("put the strip on the " + name, ctx -> {
                            resourceView(ctx).setPlacement(placement);
                            // Two tabs to a line in every placement, so each shot shows the wrap too.
                            resourceView(ctx).setStripSize(WIDE_STRIP);
                        });
                        g.frames(2);
                        g.step("the strip is on the " + name + " of the content", ctx ->
                                checkPlacementGeometry(ctx, placement));
                        g.step("no tab is cut off by the strip", ResourceViewTabStripScenario::checkTabsFit);
                        g.check("the browser tab is still at the head of it", ctx -> {
                            var view = resourceView(ctx);
                            return view.tabView.tabHeaderContainer.getChildren().getFirst()
                                    == view.getAssetBrowserTab()
                                    && view.tabView.tabHeaderContainer.getChildren().get(1)
                                    == view.pinnedTabSeparator;
                        });
                        g.check("the divider runs across the strip", ctx -> {
                            var view = resourceView(ctx);
                            var separator = view.pinnedTabSeparator;
                            return view.isColumnStrip()
                                    ? separator.getSizeWidth() > separator.getSizeHeight()
                                    : separator.getSizeHeight() > separator.getSizeWidth();
                        });
                        g.check("the tabs wrapped onto a second line", ctx -> {
                            var lines = resourceView(ctx).isColumnStrip() ? rows(ctx) : columns(ctx);
                            return lines.size() < tabs(ctx).size();
                        });
                        g.screenshot("%02d_%s".formatted(index, name));
                        g.screenshotElement("%02d_%s_strip".formatted(index, name), "#resource_tab_header");
                    }
                })

                .group("the seam resizes on whichever side the strip is", g -> {
                    for (var placement : List.of(ResourceTabLayout.Placement.RIGHT,
                            ResourceTabLayout.Placement.BOTTOM)) {
                        var name = placement.styleName();
                        g.step("put the strip on the " + name + " at its own size", ctx -> {
                            resourceView(ctx).setPlacement(placement);
                            resourceView(ctx).fitStripSize();
                        });
                        g.frames(2);
                        g.step("remember how thin it started", ctx ->
                                ctx.put("before_" + name, resourceView(ctx).getStripSize()));
                        dragSeam(g, placement);
                        g.step("the strip grew away from the content", ctx -> {
                            var before = ctx.<Float>get("before_" + name);
                            var after = resourceView(ctx).getStripSize();
                            ctx.check("dragging the " + name + " seam made the strip thicker",
                                    after > before + 1, "> " + (before + 1), after);
                        });
                        g.step("the strip is still on the " + name, ctx ->
                                checkPlacementGeometry(ctx, placement));
                    }
                })

                .group("the arrangement is written down as it is made", g -> g
                        .step("the saved copy matches the strip on screen", ctx -> {
                            var view = resourceView(ctx);
                            var saved = ResourceTabLayoutStore.load(TestEditor.class.getName());
                            ctx.check("the saved placement matches",
                                    saved.getPlacement() == view.getPlacement(),
                                    view.getPlacement(), saved.getPlacement());
                            ctx.check("the saved thickness matches",
                                    Math.abs(saved.getStripSize() - view.getStripSize()) < 0.01f,
                                    view.getStripSize(), saved.getStripSize());
                            ctx.check("the saved order matches",
                                    saved.getOrder().equals(names(view.orderedResources())),
                                    names(view.orderedResources()), saved.getOrder());
                            ctx.check("the saved hidden set matches",
                                    saved.getHidden().contains(ctx.<String>get("hidden_resource")),
                                    ctx.<String>get("hidden_resource"), saved.getHidden());
                        })
                        .step("rebuild the tabs the way reopening a project does", ctx -> {
                            var view = resourceView(ctx);
                            ctx.put("saved_order", names(view.orderedResources()));
                            var project = editor(ctx).getCurrentProject();
                            ctx.require("a project is open", project != null);
                            view.clear();
                            view.loadResources(project.getResources());
                        })
                        .waitUntil("the tabs are back", ctx -> !resourceView(ctx).getResourceTabs().isEmpty())
                        .check("they came back in the order they were left in", ctx ->
                                names(resourceView(ctx).orderedResources())
                                        .equals(ctx.<List<String>>get("saved_order")))
                        .check("the hidden one is still out of the strip", ctx -> {
                            var view = resourceView(ctx);
                            var name = ctx.<String>get("hidden_resource");
                            var resource = view.orderedResources().stream()
                                    .filter(r -> r.getName().equals(name)).findFirst().orElse(null);
                            return resource != null && !view.getResourceTabs().get(resource).isDisplayed();
                        })
                        .screenshot("20_restored"))

                .closeScreen()
                // The saved arrangement outlives the run, and the next scenario to open a TestEditor
                // would inherit it — including resource_view_pinned_tab, which is about a left strip.
                // Straight at the store rather than through the view, because a teardown also runs
                // after a failure partway through, when there may be no screen left to reach it from.
                .teardown("leave the saved strip as it was found", ctx ->
                        ResourceTabLayoutStore.save(TestEditor.class.getName(), new ResourceTabLayout()));
    }

    /// helpers

    /**
     * The strip really is on the side it says, and the content really has the rest.
     *
     * <p>Both halves matter: a strip that reports itself on the right while still drawn on the left is
     * the kind of thing only geometry catches, and a strip that takes the room without leaving any is
     * the other way the same mistake shows up.
     */
    private static void checkPlacementGeometry(TestContext ctx, ResourceTabLayout.Placement placement) {
        var view = resourceView(ctx);
        var header = view.tabView.tabHeaderContainer;
        var content = view.tabView.tabContentContainer;
        var side = placement.styleName();
        if (placement.isColumn) {
            ctx.check("the strip runs the full height on the " + side,
                    header.getSizeHeight() >= view.tabView.getSizeHeight() - 1,
                    view.tabView.getSizeHeight(), header.getSizeHeight());
            ctx.check("the strip and the content fill the width between them",
                    Math.abs(header.getSizeWidth() + content.getSizeWidth()
                            - view.tabView.getSizeWidth()) < 1.5f,
                    view.tabView.getSizeWidth(), header.getSizeWidth() + content.getSizeWidth());
            var headerFirst = header.getPositionX() < content.getPositionX();
            ctx.check("the strip is on the " + side, headerFirst == placement.isLeading,
                    side, headerFirst ? "left" : "right");
        } else {
            ctx.check("the strip runs the full width on the " + side,
                    header.getSizeWidth() >= view.tabView.getSizeWidth() - 1,
                    view.tabView.getSizeWidth(), header.getSizeWidth());
            ctx.check("the strip and the content fill the height between them",
                    Math.abs(header.getSizeHeight() + content.getSizeHeight()
                            - view.tabView.getSizeHeight()) < 1.5f,
                    view.tabView.getSizeHeight(), header.getSizeHeight() + content.getSizeHeight());
            var headerFirst = header.getPositionY() < content.getPositionY();
            ctx.check("the strip is on the " + side, headerFirst == placement.isLeading,
                    side, headerFirst ? "top" : "bottom");
        }
        ctx.check("the content starts where the strip ends",
                Math.abs((placement.isLeading
                        ? (placement.isColumn ? content.getPositionX() : content.getPositionY())
                        : (placement.isColumn
                                ? content.getPositionX() + content.getSizeWidth()
                                : content.getPositionY() + content.getSizeHeight()))
                        - view.getStripSeam()) < 1.5f,
                view.getStripSeam(), placement.isColumn ? content.getPositionX() : content.getPositionY());
    }

    private static void checkTabsFit(TestContext ctx) {
        checkTabsFit(ctx, "a tab fits inside the strip");
    }

    /**
     * Every tab fits inside the strip along the axis the strip is thin on.
     *
     * <p>The one invariant a screenshot shows and a layout check usually does not: a tab bigger than
     * the strip is not an error, it just draws over the edge — and the tab a theme makes bigger is the
     * selected one, so the clipped tab is always the one being looked at.
     *
     * <p>Measured off the tabs rather than off {@code getMinimumStripSize}, which is what the strip
     * sizes itself from: checking that against itself would pass however wrong it was.
     *
     * <p>Package-private because the theme gallery runs the same invariant over every built-in theme,
     * and two copies of it would be two things to keep in step.
     */
    static void checkTabsFit(TestContext ctx, String label) {
        var view = resourceView(ctx);
        var header = view.tabView.tabHeaderContainer;
        var column = view.isColumnStrip();
        var room = column ? header.getContentWidth() : header.getContentHeight();
        for (var tab : view.allTabs()) {
            if (!tab.isDisplayed()) continue;
            var margin = tab.getTaffyLayout().margin();
            var needed = column
                    ? tab.getSizeWidth() + margin.left + margin.right
                    : tab.getSizeHeight() + margin.top + margin.bottom;
            ctx.check(label, needed <= room + 0.01f, "<= " + room, needed);
        }
    }

    /** Moves the pointer onto a submenu's row, which is what opens it. */
    private static void hoverSubmenu(TestContext ctx, String label) {
        var entry = menuEntry(ctx, BRANCH_ROW, label);
        ctx.require("the menu has a submenu called " + label, entry != null);
        var bounds = entry.bounds();
        ctx.input().moveTo(bounds.centerX(), bounds.centerY());
    }

    /** Presses and releases a menu leaf where it is, without re-resolving it in between. */
    private static void clickMenuEntry(TestContext ctx, String label) {
        var entry = menuEntry(ctx, LEAF_ROW, label);
        ctx.require("the menu has an entry for " + label, entry != null);
        var bounds = entry.bounds();
        ctx.input().moveTo(bounds.centerX(), bounds.centerY());
        ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), Keys.MOUSE_LEFT);
        ctx.input().mouseUp(bounds.centerX(), bounds.centerY(), Keys.MOUSE_LEFT);
    }

    /**
     * A menu row by its label. Not {@code withTextContaining}: a row wraps its icon and its label in a
     * box of its own, which is one level deeper than the text extraction looks.
     */
    @Nullable
    private static ElementRef menuEntry(TestContext ctx, String styleClass, String label) {
        return ctx.query().withClass(styleClass)
                .where(element -> element.selfAndAllChildren()
                        .anyMatch(child -> child instanceof TextElement text
                                && text.getText().getString().equals(label)))
                .optional().orElse(null);
    }

    /** Grabs the seam on whichever side the strip is and pulls it away from the content. */
    private static void dragSeam(ScenarioBuilder s, ResourceTabLayout.Placement placement) {
        var from = new float[2];
        var to = new float[2];
        s.step("aim at the " + placement.styleName() + " seam", ctx -> {
            var view = resourceView(ctx);
            var seam = view.getStripSeam();
            // Away from the content is the direction that makes the strip thicker.
            var away = placement.isLeading ? WIDEN_BY : -WIDEN_BY;
            from[0] = placement.isColumn ? seam : midX(ctx);
            from[1] = placement.isColumn ? midY(ctx) : seam;
            to[0] = placement.isColumn ? seam + away : from[0];
            to[1] = placement.isColumn ? from[1] : seam + away;
            ctx.input().moveTo(from[0], from[1]);
            ctx.require("the pointer is on the resize handle", view.isOverResizeHandle(from[0], from[1]));
        });
        s.step("press the handle", ctx -> ctx.input().mouseDown(from[0], from[1], Keys.MOUSE_LEFT));
        s.step("pull it away from the content", ctx -> ctx.input().dragTo(to[0], to[1], Keys.MOUSE_LEFT));
        s.step("let go", ctx -> ctx.input().mouseUp(to[0], to[1], Keys.MOUSE_LEFT));
    }

    /** Remembers the order the strip is in right now, so the drag that follows can be checked against it. */
    private static void captureOrder(ScenarioBuilder s, List<String> into) {
        s.step("remember the order the tabs are in", ctx -> {
            into.clear();
            into.addAll(names(resourceView(ctx).orderedResources()));
            ctx.require("there are five resource tabs to shuffle", into.size() == 5);
        });
    }

    /**
     * Drags the tab at {@code fromIndex} onto the one at {@code toIndex} and drops it just past that
     * tab's middle, which is the strip's "insert after this one" gesture.
     *
     * <p>Spelled out step by step rather than through {@code ScenarioBuilder#drag} so the marker can be
     * captured while the drag is still in the air — a screenshot of where a drop is going to land is
     * the one thing a check on the result cannot show.
     */
    private static void dragTabOntoTab(ScenarioBuilder s, int fromIndex, int toIndex, String marker) {
        var from = new float[2];
        var to = new float[2];
        s.step("aim at tab " + fromIndex, ctx -> {
            var source = tabs(ctx).get(fromIndex);
            var target = tabs(ctx).get(toIndex);
            from[0] = source.getPositionX() + source.getSizeWidth() / 2f;
            from[1] = source.getPositionY() + source.getSizeHeight() / 2f;
            to[0] = target.getPositionX() + target.getSizeWidth() / 2f;
            to[1] = target.getPositionY() + target.getSizeHeight() / 2f;
            ctx.input().moveTo(from[0], from[1]);
        });
        s.step("press it", ctx -> ctx.input().mouseDown(from[0], from[1], Keys.MOUSE_LEFT));
        // Still on the tab, so it becomes the remembered hover and gets the leave event next frame.
        s.step("nudge", ctx -> ctx.input().dragTo(from[0] + 1, from[1] + 1, Keys.MOUSE_LEFT));
        s.step("leave the tab, which starts the drag", ctx -> {
            ctx.input().dragTo((from[0] + to[0]) / 2f, (from[1] + to[1]) / 2f, Keys.MOUSE_LEFT);
            var ui = ctx.ui();
            ctx.require("the drag started", ui != null && ui.getDragHandler().isDragging());
            // A theme is free to make tabs taller than they are wide, and the icon following the
            // pointer is the thing the user looks at for the whole gesture: it must not be stretched.
            var drag = ui.getDragHandler();
            ctx.check("the icon under the pointer is square",
                    Math.abs(drag.width - drag.height) < 0.01f, drag.width, drag.height);
        });
        // Onto the far half along the strip's flow axis, which is the half of a tab that means "after
        // this one". Never the exact middle: that is the one point where neither half wins, and the
        // marker settles under the pointer, so a nudge afterwards would no longer move it.
        s.step("arrive on the far half of tab " + toIndex, ctx ->
                ctx.input().dragTo(to[0] + farHalfX(ctx), to[1] + farHalfY(ctx), Keys.MOUSE_LEFT));
        s.step("jitter so the strip sees an update while hovered", ctx ->
                ctx.input().dragTo(to[0] + farHalfX(ctx) + 1, to[1] + farHalfY(ctx), Keys.MOUSE_LEFT));
        s.step("a drop marker is showing", ctx -> {
            var markers = ctx.query().withClass(ResourceView.CLASS_DROP_PLACEHOLDER).list();
            ctx.check("exactly one drop marker is showing", markers.size() == 1, 1, markers.size());
            if (markers.size() == 1) {
                var container = resourceView(ctx).tabView.tabScroller.viewContainer;
                ctx.log("the marker is at index " + container.getChildren().indexOf(markers.getFirst().element())
                        + " of " + container.getChildren().size());
            }
        });
        s.screenshotElement(marker, "#resource_tab_header");
        s.step("drop it", ctx ->
                ctx.input().mouseUp(to[0] + farHalfX(ctx), to[1] + farHalfY(ctx), Keys.MOUSE_LEFT));
        s.check("the marker is gone", ctx -> ctx.query()
                .withClass(ResourceView.CLASS_DROP_PLACEHOLDER).count() == 0);
    }

    /** Offset from a tab's middle onto its far half, along whichever axis the strip flows on. */
    private static float farHalfX(TestContext ctx) {
        return resourceView(ctx).isColumnStrip() ? 4 : 0;
    }

    private static float farHalfY(TestContext ctx) {
        return resourceView(ctx).isColumnStrip() ? 0 : 4;
    }

    private static List<String> names(List<Resource<?>> resources) {
        return resources.stream().map(Resource::getName).toList();
    }

    /** The resource tabs that are actually in the strip, in the order they are laid out. */
    private static List<Tab> tabs(TestContext ctx) {
        return resourceView(ctx).tabView.tabScroller.viewContainer.getChildren().stream()
                .filter(child -> child instanceof Tab && child.isDisplayed())
                .map(Tab.class::cast)
                .toList();
    }

    /** How many distinct lines the tabs sit on across the strip. */
    private static List<Float> rows(TestContext ctx) {
        return distinct(ctx, UIElement::getPositionY);
    }

    /** How many distinct lines the tabs sit on down the strip. */
    private static List<Float> columns(TestContext ctx) {
        return distinct(ctx, UIElement::getPositionX);
    }

    private static List<Float> distinct(TestContext ctx, Function<UIElement, Float> axis) {
        var seen = new ArrayList<Float>();
        for (var tab : tabs(ctx)) {
            var value = axis.apply(tab);
            if (seen.stream().noneMatch(other -> Math.abs(other - value) < 0.5f)) {
                seen.add(value);
            }
        }
        return seen;
    }

    private static float seamX(TestContext ctx) {
        return resourceView(ctx).getStripSeam();
    }

    private static float midY(TestContext ctx) {
        var view = resourceView(ctx);
        return view.getPositionY() + view.getSizeHeight() / 2f;
    }

    private static float midX(TestContext ctx) {
        var view = resourceView(ctx);
        return view.getPositionX() + view.getSizeWidth() / 2f;
    }

    private static Editor editor(TestContext ctx) {
        return ctx.query().type(Editor.class).one().as(Editor.class);
    }

    private static ResourceView resourceView(TestContext ctx) {
        return editor(ctx).resourceView;
    }
}
