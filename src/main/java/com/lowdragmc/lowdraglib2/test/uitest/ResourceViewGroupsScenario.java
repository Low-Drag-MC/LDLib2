package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.resource.ColorsResource;
import com.lowdragmc.lowdraglib2.editor.resource.Resource;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceGroup;
import com.lowdragmc.lowdraglib2.editor.resource.TexturesResource;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.view.ResourceTabLayout;
import com.lowdragmc.lowdraglib2.editor.ui.view.ResourceTabLayoutStore;
import com.lowdragmc.lowdraglib2.editor.ui.view.ResourceView;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Tab;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.TestEditor;
import com.lowdragmc.lowdraglib2.test.TestProject;
import com.lowdragmc.lowdraglib2.test.resource.GroupedTestProject;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;

import java.util.ArrayList;
import net.minecraft.network.chat.Component;

import java.util.List;

import static com.lowdragmc.lowdraglib2.test.resource.GroupedTestProject.ALPHA;
import static com.lowdragmc.lowdraglib2.test.resource.GroupedTestProject.ALPHA_ONE;
import static com.lowdragmc.lowdraglib2.test.resource.GroupedTestProject.ALPHA_THREE;
import static com.lowdragmc.lowdraglib2.test.resource.GroupedTestProject.ALPHA_TWO;
import static com.lowdragmc.lowdraglib2.test.resource.GroupedTestProject.BETA;
import static com.lowdragmc.lowdraglib2.test.resource.GroupedTestProject.BETA_ONE;
import static com.lowdragmc.lowdraglib2.test.resource.GroupedTestProject.BETA_TWO;

/**
 * Resource groups in the resource view's strip: one tab per group, standing where its first resource
 * would have, opening a panel of its resources by icon and name; picking one selects it; the tab drags,
 * hides and saves its place like any other; and a project without groups is untouched.
 */
@LDLRegisterClient(name = "resource_view_groups", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class ResourceViewGroupsScenario implements UIScenario {

    private static final String SELECTED = "__selected__";
    private static final List<String> THEMES =
            List.of("mc", "modern", "ore", "dusk", "carbon", "mint", "plum", "paper", "latte");
    private static final String COLOR = ColorsResource.INSTANCE.getName();
    private static final String TEXTURE = TexturesResource.INSTANCE.getName();
    private static final String GROUP_ALPHA = ResourceView.GROUP_ORDER_PREFIX + ALPHA.getId();
    private static final String GROUP_BETA = ResourceView.GROUP_ORDER_PREFIX + BETA.getId();

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
                .step("show the resource view and load a project with grouped resources", ctx -> {
                    var view = view(ctx);
                    var container = view.getViewContainer();
                    ctx.require("the resource view is docked", container != null);
                    container.selectView(view);
                    editor(ctx).loadProject(new GroupedTestProject(), null);
                })
                .waitUntil("the resource tabs are up", ctx -> !view(ctx).getResourceTabs().isEmpty())
                .step("start from a strip nobody has touched", ctx -> view(ctx).resetTabLayout())
                .step("name the strip so it can be captured on its own", ctx ->
                        view(ctx).tabView.tabHeaderContainer.setId("resource_tab_header"))

                .group("each group has one tab, where its first resource was added", g -> g
                        .step("the strip", ctx -> ctx.check("the strip holds a tab per group and per ungrouped resource",
                                strip(ctx).equals(List.of(COLOR, GROUP_ALPHA, GROUP_BETA, TEXTURE)),
                                List.of(COLOR, GROUP_ALPHA, GROUP_BETA, TEXTURE), strip(ctx)))
                        .step("the resources in order", ctx -> {
                            var expected = List.of(COLOR, ALPHA_ONE.getName(), ALPHA_TWO.getName(), ALPHA_THREE.getName(),
                                    BETA_ONE.getName(), BETA_TWO.getName(), TEXTURE);
                            var actual = names(view(ctx).orderedResources());
                            ctx.check("a group's resources are listed where its tab is", actual.equals(expected), expected, actual);
                        })
                        .check("every resource still has a tab of its own", ctx -> view(ctx).getResourceTabs().size() == 7)
                        .check("the third resource joined the group by its id alone", ctx ->
                                ALPHA.equals(view(ctx).getGroupOf(ALPHA_THREE))
                                        && view(ctx).getGroupMembers(ALPHA).equals(List.of(ALPHA_ONE, ALPHA_TWO, ALPHA_THREE)))
                        .check("an ungrouped resource is in no group", ctx -> view(ctx).getGroupOf(ColorsResource.INSTANCE) == null)
                        .check("a grouped resource's tab is not in the strip", ctx -> {
                            var strip = view(ctx).tabView.tabScroller.viewContainer;
                            return List.of(ALPHA_ONE, ALPHA_TWO, ALPHA_THREE, BETA_ONE, BETA_TWO).stream()
                                    .noneMatch(resource -> view(ctx).getResourceTabs().get(resource).getParent() == strip);
                        })
                        .check("a group's tab is marked as one", ctx -> groupTab(ctx, ALPHA).hasClass(ResourceView.CLASS_GROUP_TAB)
                                && groupTab(ctx, ALPHA).getStyle().overlayTexture() == ResourceView.GROUP_MARKER)
                        .check("and is a plain tab, so a theme's tab rules reach it as they reach the others", ctx ->
                                groupTab(ctx, ALPHA).getElementName().equals(stripTab(ctx, COLOR).getElementName())
                                        && ALPHA.equals(view(ctx).getGroupOfTab(groupTab(ctx, ALPHA)))
                                        && view(ctx).getGroupOfTab(stripTab(ctx, COLOR)) == null)
                        .check("before any is picked a group shows its first", ctx ->
                                view(ctx).getShownGroupMember(ALPHA) == ALPHA_ONE && view(ctx).getShownGroupMember(BETA) == BETA_ONE
                                        && groupIcon(ctx, ALPHA) == ALPHA_ONE.getIcon())
                        .check("its tooltip is the group's name over the one it shows", ctx -> {
                            var tooltips = groupTab(ctx, ALPHA).collectHoverTooltips();
                            return tooltips != null && tooltips.tooltips().size() == 2
                                    && tooltips.tooltips().get(0) instanceof Component first
                                    && first.getString().equals(ALPHA.getDisplayName().getString())
                                    && tooltips.tooltips().get(1) instanceof Component second
                                    && second.getString().equals("Alpha One");
                        })
                        .screenshotElement("01_groups_strip", "#resource_tab_header"))

                .group("clicking a group's tab opens its resources beside it", g -> g
                        .click("alpha's tab", ctx -> groupTab(ctx, ALPHA), Keys.MOUSE_LEFT)
                        .waitUntil("alpha's panel is open", ctx -> ALPHA.equals(view(ctx).getOpenGroup()))
                        .step("its rows", ctx -> {
                            var expected = List.of("Alpha One", "Alpha Two", "Alpha Three");
                            var actual = rowNames(ctx);
                            ctx.check("one row per resource, by name, in the order they were added",
                                    actual.equals(expected), expected, actual);
                        })
                        .step("beside the tab", ctx -> {
                            var tab = groupTab(ctx, ALPHA);
                            var flyout = view(ctx).getGroupFlyout();
                            ctx.check("to the right of a strip on the left",
                                    flyout.getPositionX() >= tab.getPositionX() + tab.getSizeWidth() - 0.5f,
                                    tab.getPositionX() + tab.getSizeWidth(), flyout.getPositionX());
                            ctx.check("level with the tab", Math.abs(flyout.getPositionY() - tab.getPositionY()) < 0.5f,
                                    tab.getPositionY(), flyout.getPositionY());
                        })
                        .check("it is themed by its class", ctx -> view(ctx).getGroupFlyout().hasClass(ResourceView.CLASS_GROUP_FLYOUT)
                                && row(ctx, "Alpha Two").hasClass(ResourceView.CLASS_GROUP_MEMBER))
                        .check("the tab, still under the mouse, keeps its tooltip off the rows",
                                ctx -> groupTab(ctx, ALPHA).collectHoverTooltips() == null)
                        .check("beta's still has one", ctx -> groupTab(ctx, BETA).collectHoverTooltips() != null)
                        .screenshot("02_alpha_open"))

                .group("picking a resource selects it, marks its group and puts the panel away", g -> g
                        .click("Alpha Two", ctx -> row(ctx, "Alpha Two"), Keys.MOUSE_LEFT)
                        .waitUntil("the panel closed", ctx -> view(ctx).getOpenGroup() == null)
                        .check("Alpha Two is the selected resource", ctx -> selected(ctx) == ALPHA_TWO)
                        .check("alpha's tab shows as selected", ctx -> groupTab(ctx, ALPHA).hasClass(SELECTED))
                        .check("beta's does not", ctx -> !groupTab(ctx, BETA).hasClass(SELECTED))
                        .check("its content is showing", ctx -> view(ctx).getResourceTabs().get(ALPHA_TWO).getContent().isDisplayed())
                        .check("alpha's tab wears Alpha Two's icon now", ctx -> view(ctx).getShownGroupMember(ALPHA) == ALPHA_TWO
                                && groupIcon(ctx, ALPHA) == ALPHA_TWO.getIcon())
                        .screenshot("03_alpha_two_selected")
                        .click("alpha's tab", ctx -> groupTab(ctx, ALPHA), Keys.MOUSE_LEFT)
                        .waitUntil("open again", ctx -> ALPHA.equals(view(ctx).getOpenGroup()))
                        .check("the selected resource's row shows as selected", ctx -> row(ctx, "Alpha Two").hasClass(SELECTED)
                                && !row(ctx, "Alpha One").hasClass(SELECTED)))

                .group("the group's tab closes its own panel", g -> g
                        .click("alpha's tab again", ctx -> groupTab(ctx, ALPHA), Keys.MOUSE_LEFT)
                        .waitUntil("closed", ctx -> view(ctx).getOpenGroup() == null)
                        .check("and the selection stayed", ctx -> selected(ctx) == ALPHA_TWO)
                        .check("its tooltip is back", ctx -> groupTab(ctx, ALPHA).collectHoverTooltips() != null))

                .group("a click anywhere else closes it", g -> g
                        .click("alpha's tab", ctx -> groupTab(ctx, ALPHA), Keys.MOUSE_LEFT)
                        .waitUntil("open", ctx -> ALPHA.equals(view(ctx).getOpenGroup()))
                        .click("the content", ctx -> view(ctx).tabView.tabContentContainer, Keys.MOUSE_LEFT)
                        .waitUntil("closed", ctx -> view(ctx).getOpenGroup() == null))

                .group("another group's tab swaps the panels", g -> g
                        .click("alpha's tab", ctx -> groupTab(ctx, ALPHA), Keys.MOUSE_LEFT)
                        .waitUntil("alpha open", ctx -> ALPHA.equals(view(ctx).getOpenGroup()))
                        .click("beta's tab", ctx -> groupTab(ctx, BETA), Keys.MOUSE_LEFT)
                        .waitUntil("beta open instead", ctx -> BETA.equals(view(ctx).getOpenGroup()))
                        .check("only one panel is up", ctx -> ctx.query().withClass(ResourceView.CLASS_GROUP_FLYOUT).count() == 1)
                        .step("put it away", ctx -> view(ctx).closeGroupFlyout()))

                .group("selecting a grouped resource from code marks its group", g -> g
                        .step("select Beta Two", ctx -> view(ctx).selectResourceInstance(BETA_TWO))
                        .check("Beta Two is selected", ctx -> selected(ctx) == BETA_TWO)
                        .check("beta's tab shows as selected and alpha's no longer does", ctx ->
                                groupTab(ctx, BETA).hasClass(SELECTED) && !groupTab(ctx, ALPHA).hasClass(SELECTED))
                        .step("select the colours", ctx -> view(ctx).selectResourceInstance(ColorsResource.INSTANCE))
                        .check("no group shows as selected", ctx ->
                                !groupTab(ctx, BETA).hasClass(SELECTED) && !groupTab(ctx, ALPHA).hasClass(SELECTED))
                        .check("each keeps the icon of the one last picked in it", ctx ->
                                view(ctx).getShownGroupMember(ALPHA) == ALPHA_TWO && view(ctx).getShownGroupMember(BETA) == BETA_TWO))

                .group("hiding one resource hides its row; hiding them all hides the group", g -> g
                        .step("hide the one alpha shows", ctx -> view(ctx).setResourceHidden(ALPHA_TWO, true))
                        .check("alpha shows the first one left", ctx -> view(ctx).getShownGroupMember(ALPHA) == ALPHA_ONE)
                        .step("show it again", ctx -> view(ctx).setResourceHidden(ALPHA_TWO, false))
                        .check("and alpha goes back to it", ctx -> view(ctx).getShownGroupMember(ALPHA) == ALPHA_TWO)
                        .step("hide Alpha One", ctx -> view(ctx).setResourceHidden(ALPHA_ONE, true))
                        .check("alpha's tab still shows", ctx -> groupTab(ctx, ALPHA).isDisplayed())
                        .click("alpha's tab", ctx -> groupTab(ctx, ALPHA), Keys.MOUSE_LEFT)
                        .waitUntil("open", ctx -> ALPHA.equals(view(ctx).getOpenGroup()))
                        .check("Alpha One's row is hidden", ctx -> !row(ctx, "Alpha One").isDisplayed()
                                && row(ctx, "Alpha Two").isDisplayed())
                        .step("select Alpha Two, then hide the rest of alpha", ctx -> {
                            view(ctx).selectResourceInstance(ALPHA_TWO);
                            view(ctx).setResourceHidden(ALPHA_TWO, true);
                            view(ctx).setResourceHidden(ALPHA_THREE, true);
                        })
                        .check("alpha's tab is gone", ctx -> !groupTab(ctx, ALPHA).isDisplayed())
                        .check("and so is its panel", ctx -> view(ctx).getOpenGroup() == null)
                        .step("the selection moved to a resource that is showing", ctx -> {
                            var selected = selected(ctx);
                            ctx.check("the selection is not left on a hidden resource",
                                    selected != null && !view(ctx).isResourceHidden(selected), "a showing resource", selected);
                        })
                        .step("asking for the hidden group's panel does nothing", ctx -> view(ctx).openGroup(ALPHA))
                        .check("no panel", ctx -> view(ctx).getOpenGroup() == null)
                        .step("show alpha again", ctx -> {
                            for (var resource : List.of(ALPHA_ONE, ALPHA_TWO, ALPHA_THREE)) {
                                view(ctx).setResourceHidden(resource, false);
                            }
                        })
                        .check("alpha's tab is back", ctx -> groupTab(ctx, ALPHA).isDisplayed()))

                .group("a group's tab drags into a new place like any other", g -> {
                    dragOnto(g, ctx -> groupTab(ctx, BETA), ctx -> stripTab(ctx, COLOR));
                    g.step("it landed straight after the tab it was dropped on", ctx -> {
                        var expected = List.of(COLOR, GROUP_BETA, GROUP_ALPHA, TEXTURE);
                        ctx.check("beta after the colours", strip(ctx).equals(expected), expected, strip(ctx));
                    });
                    g.check("beta's tab is showing again", ctx -> groupTab(ctx, BETA).isDisplayed());
                    g.step("the order was saved by the group's id", ctx -> {
                        var saved = ResourceTabLayoutStore.load(TestEditor.class.getName()).getOrder();
                        var tabs = saved.stream().filter(name -> name.startsWith(ResourceView.GROUP_ORDER_PREFIX)
                                || name.equals(COLOR) || name.equals(TEXTURE)).toList();
                        ctx.check("saved", tabs.equals(List.of(COLOR, GROUP_BETA, GROUP_ALPHA, TEXTURE)), "color, beta, alpha, texture", saved);
                        var expected = List.of(COLOR, GROUP_BETA, BETA_ONE.getName(), BETA_TWO.getName(), GROUP_ALPHA,
                                ALPHA_ONE.getName(), ALPHA_TWO.getName(), ALPHA_THREE.getName(), TEXTURE);
                        ctx.check("each group's resources right after it", saved.equals(expected), expected, saved);
                    });
                    g.screenshotElement("04_beta_moved", "#resource_tab_header");
                })

                .group("the order survives the project being loaded again", g -> g
                        .step("reload the resources", ctx -> reload(ctx, new GroupedTestProject()))
                        .step("the strip", ctx -> ctx.check("as it was left",
                                strip(ctx).equals(List.of(COLOR, GROUP_BETA, GROUP_ALPHA, TEXTURE)),
                                List.of(COLOR, GROUP_BETA, GROUP_ALPHA, TEXTURE), strip(ctx))))

                .group("a group's resources drag into a new order inside its panel", g -> {
                    g.click("alpha's tab", ctx -> groupTab(ctx, ALPHA), Keys.MOUSE_LEFT)
                            .waitUntil("open", ctx -> ALPHA.equals(view(ctx).getOpenGroup()));
                    dragRowOnto(g, "Alpha Three", "Alpha One");
                    g.check("the panel stayed open: a drop is not a pick", ctx -> ALPHA.equals(view(ctx).getOpenGroup()))
                            .step("the rows", ctx -> {
                                var expected = List.of("Alpha Three", "Alpha One", "Alpha Two");
                                ctx.check("Alpha Three went before Alpha One", rowNames(ctx).equals(expected), expected, rowNames(ctx));
                            })
                            .check("the dragged row is showing again", ctx -> row(ctx, "Alpha Three").isDisplayed())
                            .check("the group's order is the panel's", ctx ->
                                    view(ctx).getGroupMembers(ALPHA).equals(List.of(ALPHA_THREE, ALPHA_ONE, ALPHA_TWO)))
                            .check("the strip did not move", ctx -> strip(ctx).equals(List.of(COLOR, GROUP_BETA, GROUP_ALPHA, TEXTURE)))
                            .step("saved after the group's entry", ctx -> {
                                var saved = ResourceTabLayoutStore.load(TestEditor.class.getName()).getOrder();
                                var at = saved.indexOf(GROUP_ALPHA);
                                var expected = List.of(ALPHA_THREE.getName(), ALPHA_ONE.getName(), ALPHA_TWO.getName());
                                ctx.check("its resources follow it in the panel's order",
                                        at >= 0 && saved.size() >= at + 4 && saved.subList(at + 1, at + 4).equals(expected), expected, saved);
                            })
                            .step("put it away and load the project again", ctx -> {
                                view(ctx).closeGroupFlyout();
                                reload(ctx, new GroupedTestProject());
                            })
                            .check("the order came back", ctx ->
                                    view(ctx).getGroupMembers(ALPHA).equals(List.of(ALPHA_THREE, ALPHA_ONE, ALPHA_TWO)))
                            .check("and the strip's with it", ctx -> strip(ctx).equals(List.of(COLOR, GROUP_BETA, GROUP_ALPHA, TEXTURE)))
                            .click("alpha's tab", ctx -> groupTab(ctx, ALPHA), Keys.MOUSE_LEFT)
                            .waitUntil("open again", ctx -> ALPHA.equals(view(ctx).getOpenGroup()))
                            .check("its rows in that order", ctx -> rowNames(ctx).equals(List.of("Alpha Three", "Alpha One", "Alpha Two")))
                            .step("put it away", ctx -> view(ctx).closeGroupFlyout());
                })

                .group("dragging a group's tab puts its open panel away", g -> {
                    g.click("alpha's tab", ctx -> groupTab(ctx, ALPHA), Keys.MOUSE_LEFT)
                            .waitUntil("open", ctx -> ALPHA.equals(view(ctx).getOpenGroup()));
                    // pressing its own tab keeps a panel up, so only the drag can put it away
                    dragOnto(g, ctx -> groupTab(ctx, ALPHA), ctx -> stripTab(ctx, COLOR));
                    g.check("alpha after the colours", ctx -> strip(ctx).equals(List.of(COLOR, GROUP_ALPHA, GROUP_BETA, TEXTURE)));
                })

                .group("closing the view takes its panel with it: the panel hangs off the UI's root", g -> g
                        .click("alpha's tab", ctx -> groupTab(ctx, ALPHA), Keys.MOUSE_LEFT)
                        .waitUntil("open", ctx -> ALPHA.equals(view(ctx).getOpenGroup()))
                        .step("take the view out of its dock", ctx -> {
                            var container = view(ctx).getViewContainer();
                            ctx.put("view_container", container);
                            container.removeView(view(ctx));
                        })
                        .check("no panel left behind", ctx -> ctx.query().withClass(ResourceView.CLASS_GROUP_FLYOUT).count() == 0
                                && view(ctx).getOpenGroup() == null)
                        .step("put it back", ctx -> {
                            com.lowdragmc.lowdraglib2.editor.ui.ViewContainer container = ctx.get("view_container");
                            container.addView(view(ctx));
                            container.selectView(view(ctx));
                        })
                        .waitUntil("the strip is back", ctx -> groupTab(ctx, ALPHA).isDisplayed() && groupTab(ctx, ALPHA).getSizeWidth() > 0)
                        .click("alpha's tab", ctx -> groupTab(ctx, ALPHA), Keys.MOUSE_LEFT)
                        .waitUntil("and its panel opens again", ctx -> ALPHA.equals(view(ctx).getOpenGroup()))
                        .step("put it away", ctx -> view(ctx).closeGroupFlyout()))

                .group("an order saved before the groups existed still loads", g -> g
                        .step("an order that names the grouped resources one by one", ctx -> {
                            var layout = view(ctx).getTabLayout();
                            layout.setOrder(List.of(BETA_TWO.getName(), TEXTURE, ALPHA_ONE.getName(), COLOR));
                            ResourceTabLayoutStore.save(TestEditor.class.getName(), layout);
                            reload(ctx, new GroupedTestProject());
                        })
                        .step("the strip", ctx -> {
                            var expected = List.of(TEXTURE, COLOR, GROUP_ALPHA, GROUP_BETA);
                            ctx.check("the tabs it knows in its order, the groups after them",
                                    strip(ctx).equals(expected), expected, strip(ctx));
                        })
                        .check("every resource once", ctx -> view(ctx).orderedResources().size() == 7
                                && view(ctx).orderedResources().stream().distinct().count() == 7)
                        .step("back to a fresh strip", ctx -> view(ctx).resetTabLayout()))

                .group("the panel opens away from the strip wherever the strip is", g -> {
                    placement(g, ResourceTabLayout.Placement.RIGHT, "05_right");
                    placement(g, ResourceTabLayout.Placement.TOP, "06_top");
                    placement(g, ResourceTabLayout.Placement.BOTTOM, "07_bottom");
                    g.step("back down the left", ctx -> view(ctx).setPlacement(ResourceTabLayout.Placement.LEFT));
                })

                .group("every built-in theme dresses the panel like its strip", g -> {
                    var saved = new ArrayList<com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet>();
                    g.step("remember the sheets", ctx -> saved.addAll(ctx.requireUI().getStyleEngine().globalSheets));
                    for (var theme : THEMES) {
                        g.step("switch to " + theme, ctx -> {
                            var engine = ctx.requireUI().getStyleEngine();
                            engine.clearAllStylesheets();
                            engine.addStylesheet(StylesheetManager.INSTANCE.getStylesheetSafe(
                                    LDLib2.id(StylesheetManager.PATH + "/" + theme + ".lss")));
                            view(ctx).selectResourceInstance(ColorsResource.INSTANCE);
                        })
                                .step(theme + ": the colours' tab, selected", ctx -> {
                                    var tab = (Tab) stripTab(ctx, COLOR);
                                    ctx.put("selected_look", List.of(tab.getTabStyle().selectedTexture(), tab.getSizeWidth(), tab.getSizeHeight()));
                                    view(ctx).selectResourceInstance(ALPHA_TWO);
                                })
                                .step(theme + ": alpha's, selected through Alpha Two", ctx -> {
                                    var tab = groupTab(ctx, ALPHA);
                                    var look = List.of(tab.getTabStyle().selectedTexture(), tab.getSizeWidth(), tab.getSizeHeight());
                                    ctx.check(theme + ": a selected group looks like any selected tab",
                                            tab.hasClass(SELECTED) && look.equals(ctx.get("selected_look")), ctx.get("selected_look"), look);
                                })
                                .screenshotElement(theme + "_strip", "#resource_tab_header")
                                .click("alpha's tab", ctx -> groupTab(ctx, ALPHA), Keys.MOUSE_LEFT)
                                .waitUntil(theme + ": alpha open", ctx -> ALPHA.equals(view(ctx).getOpenGroup()))
                                .step(theme + ": rows", ctx -> {
                                    var flyout = view(ctx).getGroupFlyout();
                                    var row = row(ctx, "Alpha Three");
                                    ctx.check(theme + ": a row spans the panel, not a strip tab's square",
                                            row.getSizeWidth() > 40, "> 40", row.getSizeWidth());
                                    ctx.check(theme + ": a row has no background until hovered, as the strip's tabs",
                                            row.getTabStyle().baseTexture() == com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture.EMPTY,
                                            "empty", row.getTabStyle().baseTexture());
                                    ctx.check(theme + ": the panel wears the theme's background, not the built-in default",
                                            flyout.getStyle().backgroundTexture() != com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites.RECT_SOLID,
                                            "the theme's", flyout.getStyle().backgroundTexture());
                                    var offsets = new ArrayList<Float>();
                                    for (var name : List.of("Alpha One", "Alpha Two", "Alpha Three")) {
                                        var each = row(ctx, name);
                                        var icon = each.getChildren().getFirst();
                                        var iconMiddle = icon.getPositionY() + icon.getSizeHeight() / 2f;
                                        var textMiddle = each.text.getPositionY() + each.text.getSizeHeight() / 2f;
                                        ctx.check(theme + ": " + name + "'s icon and name share a middle",
                                                Math.abs(iconMiddle - textMiddle) < 0.5f, iconMiddle, textMiddle);
                                        ctx.check(theme + ": " + name + "'s icon is inside its row",
                                                icon.getPositionY() >= each.getPositionY() - 0.5f
                                                        && icon.getPositionY() + icon.getSizeHeight() <= each.getPositionY() + each.getSizeHeight() + 0.5f,
                                                each.getPositionY() + "+" + each.getSizeHeight(), icon.getPositionY() + "+" + icon.getSizeHeight());
                                        offsets.add(icon.getPositionY() - each.getPositionY());
                                    }
                                    ctx.check(theme + ": the selected row lays out as the others do",
                                            row(ctx, "Alpha Two").hasClass(SELECTED) && offsets.stream().distinct().count() == 1, "one offset", offsets);
                                })
                                .step(theme + ": name the panel", ctx -> view(ctx).getGroupFlyout().setId("group_flyout"))
                                .hover(theme + ": hover a row", ctx -> row(ctx, "Alpha Three"))
                                .screenshotElement(theme + "_flyout", "#group_flyout")
                                .step(theme + ": put it away", ctx -> view(ctx).closeGroupFlyout());
                    }
                    g.step("back to the sheets it had", ctx -> {
                        var engine = ctx.requireUI().getStyleEngine();
                        engine.clearAllStylesheets();
                        engine.addStylesheets(saved);
                    });
                })

                .group("the groups are the project's: the same types grouped another way by another", g -> g
                        .step("load them grouped as a second editor would", ctx -> reload(ctx, GroupedTestProject.regrouped()))
                        .check("the built-in colours are in beta here", ctx -> BETA.equals(view(ctx).getGroupOf(ColorsResource.INSTANCE))
                                && view(ctx).getGroupMembers(BETA).equals(List.of(ColorsResource.INSTANCE, BETA_ONE, BETA_TWO)))
                        .check("and Alpha One has a tab of its own", ctx -> view(ctx).getGroupOf(ALPHA_ONE) == null
                                && strip(ctx).contains(ALPHA_ONE.getName()) && !strip(ctx).contains(COLOR))
                        .check("alpha keeps the other two", ctx -> view(ctx).getGroupMembers(ALPHA).equals(List.of(ALPHA_TWO, ALPHA_THREE)))
                        .step("back to the project's own grouping", ctx -> reload(ctx, new GroupedTestProject()))
                        .check("as it was", ctx -> view(ctx).getGroupOf(ColorsResource.INSTANCE) == null
                                && view(ctx).getGroupMembers(ALPHA).equals(List.of(ALPHA_ONE, ALPHA_TWO, ALPHA_THREE))
                                && view(ctx).getGroupMembers(BETA).equals(List.of(BETA_ONE, BETA_TWO)))
                        .check("a type listed twice is refused", ctx -> {
                            try {
                                com.lowdragmc.lowdraglib2.editor.resource.Resources.builder()
                                        .add(ALPHA_ONE).group(ALPHA, ALPHA_ONE);
                                return false;
                            } catch (IllegalArgumentException expected) {
                                return true;
                            }
                        }))

                .group("a group's tab goes with its last resource", g -> g
                        .step("remove two of alpha's three", ctx -> {
                            view(ctx).removeResource(ALPHA_ONE);
                            view(ctx).removeResource(ALPHA_TWO);
                        })
                        .check("alpha is still there, with one", ctx -> groupTab(ctx, ALPHA) != null
                                && view(ctx).getGroupMembers(ALPHA).equals(List.of(ALPHA_THREE)))
                        .step("remove the third", ctx -> view(ctx).removeResource(ALPHA_THREE))
                        .check("alpha's tab is gone", ctx -> view(ctx).getGroupTab(ALPHA) == null
                                && !strip(ctx).contains(GROUP_ALPHA)))

                .group("a project without groups has none", g -> g
                        .step("load the plain test project's resources", ctx -> reload(ctx, new TestProject()))
                        .check("no groups", ctx -> view(ctx).getResourceGroups().isEmpty())
                        .step("every resource has its tab in the strip", ctx -> {
                            var view = view(ctx);
                            var strip = view.tabView.tabScroller.viewContainer;
                            var resources = new TestProject().getResources().resources;
                            ctx.check("one strip tab per resource", strip(ctx).size() == resources.size(),
                                    resources.size(), strip(ctx).size());
                            ctx.check("each its own", resources.stream()
                                            .allMatch(resource -> view.getResourceTabs().get(resource).getParent() == strip),
                                    true, false);
                        }));

        s.closeScreen()
                // straight at the store: a teardown also runs after a failure, when there may be no screen left
                .teardown("leave the saved strip as it was found", ctx ->
                        ResourceTabLayoutStore.save(TestEditor.class.getName(), new ResourceTabLayout()));
    }

    private static void placement(ScenarioBuilder g, ResourceTabLayout.Placement placement, String shot) {
        g.step("move the strip " + placement, ctx -> view(ctx).setPlacement(placement))
                .click("alpha's tab", ctx -> groupTab(ctx, ALPHA), Keys.MOUSE_LEFT)
                .waitUntil("alpha open", ctx -> ALPHA.equals(view(ctx).getOpenGroup()))
                .step("away from the strip", ctx -> {
                    var tab = groupTab(ctx, ALPHA);
                    var flyout = view(ctx).getGroupFlyout();
                    boolean away = switch (placement) {
                        case RIGHT -> flyout.getPositionX() + flyout.getSizeWidth() <= tab.getPositionX() + 0.5f;
                        case TOP -> flyout.getPositionY() >= tab.getPositionY() + tab.getSizeHeight() - 0.5f;
                        case BOTTOM -> flyout.getPositionY() + flyout.getSizeHeight() <= tab.getPositionY() + 0.5f;
                        default -> flyout.getPositionX() >= tab.getPositionX() + tab.getSizeWidth() - 0.5f;
                    };
                    ctx.check("the panel is on the content's side of the tab", away, "beside " + placement,
                            "tab " + tab.getPositionX() + "," + tab.getPositionY() + " panel " + flyout.getPositionX()
                                    + "," + flyout.getPositionY() + " " + flyout.getSizeWidth() + "x" + flyout.getSizeHeight());
                    var root = ctx.ui().ui.rootElement;
                    ctx.check("and on screen", flyout.getPositionX() >= root.getPositionX() - 0.5f
                                    && flyout.getPositionY() >= root.getPositionY() - 0.5f
                                    && flyout.getPositionX() + flyout.getSizeWidth() <= root.getPositionX() + root.getSizeWidth() + 0.5f
                                    && flyout.getPositionY() + flyout.getSizeHeight() <= root.getPositionY() + root.getSizeHeight() + 0.5f,
                            true, false);
                })
                .screenshot(shot)
                .step("put it away", ctx -> view(ctx).closeGroupFlyout());
    }

    /** Drags one strip tab onto the far half of another, as ResourceViewTabStripScenario does. */
    private static void dragOnto(ScenarioBuilder s, java.util.function.Function<TestContext, UIElement> source,
                                 java.util.function.Function<TestContext, UIElement> target) {
        var from = new float[2];
        var to = new float[2];
        s.step("aim at the tab to drag", ctx -> {
            var a = source.apply(ctx);
            var b = target.apply(ctx);
            from[0] = a.getPositionX() + a.getSizeWidth() / 2f;
            from[1] = a.getPositionY() + a.getSizeHeight() / 2f;
            to[0] = b.getPositionX() + b.getSizeWidth() / 2f;
            to[1] = b.getPositionY() + b.getSizeHeight() / 2f;
            ctx.input().moveTo(from[0], from[1]);
        });
        s.step("press it", ctx -> ctx.input().mouseDown(from[0], from[1], Keys.MOUSE_LEFT));
        s.step("nudge", ctx -> ctx.input().dragTo(from[0] + 1, from[1] + 1, Keys.MOUSE_LEFT));
        s.step("leave the tab, which starts the drag", ctx -> {
            ctx.input().dragTo((from[0] + to[0]) / 2f, (from[1] + to[1]) / 2f, Keys.MOUSE_LEFT);
            ctx.require("the drag started", ctx.ui() != null && ctx.ui().getDragHandler().isDragging());
        });
        s.step("arrive on the far half of the target", ctx -> ctx.input().dragTo(to[0] + farX(ctx), to[1] + farY(ctx), Keys.MOUSE_LEFT));
        s.step("jitter so the strip sees an update", ctx -> ctx.input().dragTo(to[0] + farX(ctx) + 1, to[1] + farY(ctx), Keys.MOUSE_LEFT));
        s.check("a drop marker is showing", ctx -> ctx.query().withClass(ResourceView.CLASS_DROP_PLACEHOLDER).count() == 1);
        s.step("drop it", ctx -> ctx.input().mouseUp(to[0] + farX(ctx), to[1] + farY(ctx), Keys.MOUSE_LEFT));
        s.check("the marker is gone", ctx -> ctx.query().withClass(ResourceView.CLASS_DROP_PLACEHOLDER).count() == 0);
        s.check("no panel opened on the way", ctx -> view(ctx).getOpenGroup() == null);
    }

    private static float farX(TestContext ctx) {
        return view(ctx).isColumnStrip() ? 4 : 0;
    }

    private static float farY(TestContext ctx) {
        return view(ctx).isColumnStrip() ? 0 : 4;
    }

    /** What loading a project does to the view's resources, without the project's own save prompt. */
    private static void reload(TestContext ctx, com.lowdragmc.lowdraglib2.editor.project.IProject project) {
        reload(ctx, project.getResources());
    }

    private static void reload(TestContext ctx, com.lowdragmc.lowdraglib2.editor.resource.Resources resources) {
        var view = view(ctx);
        view.clear();
        view.loadResources(resources);
    }

    /** The strip, as the saved order names its tabs. */
    private static List<String> strip(TestContext ctx) {
        var view = view(ctx);
        var names = new ArrayList<String>();
        for (var child : view.tabView.tabScroller.viewContainer.getChildren()) {
            var group = view.getGroupOfTab(child);
            if (group != null) {
                names.add(ResourceView.GROUP_ORDER_PREFIX + group.getId());
            } else {
                var resource = view.getResourceTabs().inverse().get(child);
                if (resource != null) {
                    names.add(resource.getName());
                }
            }
        }
        return names;
    }

    private static UIElement stripTab(TestContext ctx, String name) {
        var view = view(ctx);
        for (var entry : view.getResourceTabs().entrySet()) {
            if (entry.getKey().getName().equals(name)) return entry.getValue();
        }
        throw new IllegalStateException("no tab for " + name);
    }

    private static Tab groupTab(TestContext ctx, ResourceGroup group) {
        return view(ctx).getGroupTab(group);
    }

    /** What a group's tab draws as its icon right now. */
    private static com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture groupIcon(TestContext ctx, ResourceGroup group) {
        var texture = groupTab(ctx, group).getChildren().getLast().getStyle().backgroundTexture();
        return texture instanceof com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture dynamic ? dynamic.textureSupplier.get() : texture;
    }

    /** Drags one row of the open panel onto the upper half of another, which puts it before that one. */
    private static void dragRowOnto(ScenarioBuilder s, String source, String target) {
        var from = new float[2];
        var to = new float[2];
        s.step("aim at " + source, ctx -> {
            var a = row(ctx, source);
            var b = row(ctx, target);
            from[0] = a.getPositionX() + a.getSizeWidth() / 2f;
            from[1] = a.getPositionY() + a.getSizeHeight() / 2f;
            to[0] = b.getPositionX() + b.getSizeWidth() / 2f;
            to[1] = b.getPositionY() + 2;
            ctx.input().moveTo(from[0], from[1]);
        });
        s.step("press it", ctx -> ctx.input().mouseDown(from[0], from[1], Keys.MOUSE_LEFT));
        s.step("nudge", ctx -> ctx.input().dragTo(from[0] + 1, from[1] + 1, Keys.MOUSE_LEFT));
        s.step("leave the row, which starts the drag", ctx -> {
            ctx.input().dragTo(from[0], (from[1] + to[1]) / 2f, Keys.MOUSE_LEFT);
            ctx.require("the drag started", ctx.ui() != null && ctx.ui().getDragHandler().isDragging());
        });
        s.step("arrive on the top of " + target, ctx -> ctx.input().dragTo(to[0], to[1], Keys.MOUSE_LEFT));
        s.step("jitter so the panel sees an update", ctx -> ctx.input().dragTo(to[0] + 1, to[1], Keys.MOUSE_LEFT));
        s.check("one drop marker, in the panel", ctx -> ctx.query().withClass(ResourceView.CLASS_DROP_PLACEHOLDER).count() == 1
                && view(ctx).getGroupFlyout() != null
                && view(ctx).getGroupFlyout().getChildren().stream().anyMatch(c -> c.hasClass(ResourceView.CLASS_DROP_PLACEHOLDER)));
        s.screenshot("08_row_dragging");
        s.step("drop it", ctx -> ctx.input().mouseUp(to[0] + 1, to[1], Keys.MOUSE_LEFT));
        s.check("the marker is gone", ctx -> ctx.query().withClass(ResourceView.CLASS_DROP_PLACEHOLDER).count() == 0);
    }

    private static List<String> rowNames(TestContext ctx) {
        var names = new ArrayList<String>();
        for (var child : view(ctx).getGroupFlyout().getChildren()) {
            if (child instanceof Tab tab) {
                names.add(tab.text.getText().getString());
            }
        }
        return names;
    }

    private static Tab row(TestContext ctx, String name) {
        for (var child : view(ctx).getGroupFlyout().getChildren()) {
            if (child instanceof Tab tab && tab.text.getText().getString().equals(name)) return tab;
        }
        throw new IllegalStateException("no row " + name);
    }

    @org.jetbrains.annotations.Nullable
    private static Resource<?> selected(TestContext ctx) {
        var instance = view(ctx).getSelectedResourceInstance();
        return instance == null ? null : instance.resource;
    }

    private static List<String> names(List<Resource<?>> resources) {
        return resources.stream().map(Resource::getName).toList();
    }

    private static Editor editor(TestContext ctx) {
        return ctx.query().type(Editor.class).one().as(Editor.class);
    }

    private static ResourceView view(TestContext ctx) {
        return editor(ctx).resourceView;
    }
}
