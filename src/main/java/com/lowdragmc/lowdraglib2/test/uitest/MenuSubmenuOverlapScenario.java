package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Menu;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.TestEditor;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.Nullable;

/**
 * A submenu that flips left at the screen edge, over the menu two levels up, is drawn on top of it.
 *
 * <p>Reported from Photon's asset browser (New → Material → Add a New Resource): the entries of the second menu
 * below the open one painted their icons and labels over the fourth. A submenu is a child of its entry, and the
 * entries after that one painted after it.
 */
@LDLRegisterClient(name = "menu_submenu_overlap", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class MenuSubmenuOverlapScenario implements UIScenario {
    private static final int BELOW = 6;

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(30).tags("menu");
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("editor", ctx -> new ModularUI(UI.of(new TestEditor()), ctx.player()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the editor has laid out", ctx -> editor(ctx).centerWindow.getSizeWidth() > 0)
                .step("open a four-level menu three menus away from the right edge", ctx -> {
                    var menu = TreeBuilder.Menu.start().branch(Icons.FILE, "outer", outer -> {
                        outer.branch(Icons.FILE, "open", open -> open.branch(Icons.FILE, "deep", deep -> deep
                                .leaf("deep a", () -> {})
                                .leaf("deep b", () -> {})
                                .leaf("deep c", () -> {})));
                        for (int i = 1; i <= BELOW; i++) {
                            outer.leaf(Icons.FOLDER, "below " + i, () -> {});
                        }
                    });
                    var width = ctx.mc().getWindow().getGuiScaledWidth();
                    editor(ctx).openMenu(width - 3 * 130, 40, menu);
                })
                .frames(3)
                .step("hover outer", ctx -> hover(ctx, "outer"))
                .frames(3)
                .step("hover open", ctx -> hover(ctx, "open"))
                .frames(3)
                .step("hover deep", ctx -> hover(ctx, "deep"))
                .frames(5)
                .screenshot("flipped")
                .step("the deepest menu is drawn over the second", ctx -> {
                    var second = menuOf(ctx, "outer");
                    var deepest = menuOf(ctx, "deep");
                    ctx.require("all four menus are open", second != null && deepest != null && menuOf(ctx, "open") != null);
                    ctx.require("the deepest menu flipped over the second", overlaps(second, deepest));
                    ctx.check("the entry with the open submenu paints last in the second menu",
                            second.getSafeSortedChildren()[0] == row(second, "open"));
                    var third = menuOf(ctx, "open");
                    ctx.check("and in the third", third.getSafeSortedChildren()[0] == row(third, "deep"));
                })
                .step("hover the last entry of the second menu", ctx -> hover(ctx, "below " + BELOW))
                .frames(3)
                .step("the entry is back in line once its submenu closed", ctx -> {
                    var second = menuOf(ctx, "outer");
                    ctx.require("the second menu is still open", second != null);
                    ctx.check("the submenus closed", menuOf(ctx, "open") == null);
                    var open = row(second, "open");
                    ctx.check("its entry is no longer lifted", open != null && open.getStyle().zIndex() == 0,
                            0, open == null ? null : open.getStyle().zIndex());
                })
                .closeScreen();
    }

    private static Editor editor(TestContext ctx) {
        return ctx.query().type(Editor.class).one().as(Editor.class);
    }

    @Nullable
    private static Menu<?, ?> menuOf(TestContext ctx, String name) {
        for (var ref : ctx.query().type(Menu.class).list()) {
            var menu = ref.as(Menu.class);
            if (menu.root.getKey() instanceof Tuple<?, ?> key && key.getB() instanceof Component label
                    && label.getString().equals(name)) {
                return menu;
            }
        }
        return null;
    }

    /** The entry of {@code menu} labelled {@code name}: entries are its direct children. */
    @Nullable
    private static UIElement row(Menu<?, ?> menu, String name) {
        for (var child : menu.getChildren()) {
            var labelled = child.selfAndAllChildren().anyMatch(e -> e instanceof TextElement text
                    && text.getText().getString().equals(name) && text.getParent() != null
                    && text.getFirstAncestorOfType(Menu.class) == menu);
            if (labelled) return child;
        }
        return null;
    }

    private static void hover(TestContext ctx, String name) {
        UIElement found = null;
        for (var ref : ctx.query().type(Menu.class).list()) {
            var entry = row(ref.as(Menu.class), name);
            if (entry != null) found = entry;
        }
        ctx.require("an entry called " + name, found != null);
        ctx.input().moveTo(found.getPositionX() + found.getSizeWidth() / 2f, found.getPositionY() + found.getSizeHeight() / 2f);
    }

    private static boolean overlaps(UIElement a, UIElement b) {
        return a.getPositionX() < b.getPositionX() + b.getSizeWidth() && b.getPositionX() < a.getPositionX() + a.getSizeWidth()
                && a.getPositionY() < b.getPositionY() + b.getSizeHeight() && b.getPositionY() < a.getPositionY() + a.getSizeHeight();
    }
}
