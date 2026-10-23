package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.editor.ui.browser.FileOps;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TreeList;
import com.lowdragmc.lowdraglib2.gui.util.FileNode;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ElementBounds;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/** The file dialog's path box: a selection's path can be jumped to as shown, and nothing outside the dialog's folder can be reached. */
@LDLRegisterClient(name = "file_dialog_paths", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class FileDialogPathsScenario implements UIScenario {
    private static final String HOST = "host";
    private static final String ROOT = "root";
    private static final String DIALOG = "dialog";
    private static final String RESULT = "result";
    private static final String JUMP = "__file-dialog_jump-to__";
    private static final String CONFIRM = "__confirm-button__";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(30).tags("dialog", "files");
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("a host for the dialog", ctx -> {
                    var host = new UIElement();
                    host.layout(layout -> layout.widthPercent(100).heightPercent(100));
                    ctx.put(HOST, host);
                    return new ModularUI(UI.of(host), ctx.player());
                })
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .step("make a tree spelled relative to the working directory, as the assets folder is in a dev run", ctx -> {
                    var root = new File(".", "ldlib2-uitest/file-dialog");
                    FileOps.deleteRecursively(root);
                    ctx.require("could create the folders", new File(root, "inner/deep").mkdirs());
                    try {
                        Files.writeString(new File(root, "inner/a.txt").toPath(), "a");
                    } catch (IOException e) {
                        ctx.require("could write a file: " + e, false);
                    }
                    ctx.put(ROOT, root);
                    ctx.put(RESULT, new AtomicReference<File>());
                })
                .step("open a picker on it", ctx -> openDialog(ctx))
                .frames(2)

                .group("the path a selection writes can be jumped to", g -> g
                        .step("select the root", ctx -> tree(ctx).setSelected(List.of(tree(ctx).getRoot()), true))
                        .step("the box shows the root as the tree spells it",
                                ctx -> checkText(ctx, ctx.<File>get(ROOT).toString()))
                        .step("go to it", ctx -> press(ctx, JUMP))
                        .check("without an error", ctx -> errors(ctx).isEmpty())
                        .check("the root stays selected", ctx -> isSelected(ctx, "")))

                .group("a typed path is revealed in the tree", g -> g
                        .step("type the deep folder's path", ctx ->
                                field(ctx).setText(new File(ctx.<File>get(ROOT), "inner/deep").getPath(), false))
                        .step("go to it", ctx -> press(ctx, JUMP))
                        .check("without an error", ctx -> errors(ctx).isEmpty())
                        .check("the deep folder is selected", ctx -> isSelected(ctx, "inner/deep"))
                        .step("type the inner folder's absolute path, as the system picker gives it", ctx ->
                                field(ctx).setText(canonical(new File(ctx.<File>get(ROOT), "inner")).getPath(), false))
                        .step("go to it", ctx -> press(ctx, JUMP))
                        .check("the inner folder is selected", ctx -> isSelected(ctx, "inner"))
                        .step("and the box is back in the tree's spelling",
                                ctx -> checkText(ctx, new File(ctx.<File>get(ROOT), "inner").getPath())))

                .group("nothing outside the dialog's folder can be reached", g -> g
                        .step("type the working directory", ctx ->
                                field(ctx).setText(canonical(new File(".")).getPath(), false))
                        .step("go to it", ctx -> press(ctx, JUMP))
                        .check("an error says why", ctx -> errors(ctx).size() == 1)
                        .check("the selection did not move", ctx -> isSelected(ctx, "inner"))
                        .step("dismiss the error", ctx -> errors(ctx).forEach(Dialog::close))
                        .step("confirm it anyway", ctx -> press(ctx, CONFIRM))
                        .check("nothing is picked", ctx -> result(ctx).get() == null))

                .group("a picked file keeps the tree's spelling", g -> g
                        .step("dismiss the error and open the picker again", ctx -> {
                            errors(ctx).forEach(Dialog::close);
                            openDialog(ctx);
                        })
                        .frames(2)
                        .step("type the file's path", ctx ->
                                field(ctx).setText(new File(ctx.<File>get(ROOT), "inner/a.txt").getPath(), false))
                        .step("go to it", ctx -> press(ctx, JUMP))
                        .check("the file is selected", ctx -> isSelected(ctx, "inner/a.txt"))
                        .step("confirm", ctx -> press(ctx, CONFIRM))
                        .step("the result is spelled like the root, relative or not", ctx -> {
                            var expected = new File(ctx.<File>get(ROOT), "inner/a.txt").getPath();
                            var actual = String.valueOf(result(ctx).get());
                            ctx.check("result", expected.equals(actual), expected, actual);
                        }))

                .teardown("remove the temporary tree", ctx -> {
                    var root = ctx.<File>get(ROOT);
                    if (root != null) FileOps.deleteRecursively(root);
                })
                .closeScreen();
    }

    private static void openDialog(TestContext ctx) {
        var dialog = Dialog.showFileDialog("file dialog", ctx.<File>get(ROOT), true, null, result(ctx)::set);
        ctx.put(DIALOG, dialog);
        dialog.show(ctx.<UIElement>get(HOST));
    }

    /** Down and up in one step: the buttons act on the press, and what they open can cover them before the release. */
    private static void press(TestContext ctx, String clazz) {
        var button = find(ctx.get(DIALOG), element -> element.hasClass(clazz));
        ctx.require("the dialog has " + clazz, button != null);
        var bounds = ElementBounds.of(button);
        ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), Keys.MOUSE_LEFT);
        ctx.input().mouseUp(bounds.centerX(), bounds.centerY(), Keys.MOUSE_LEFT);
    }

    private static void checkText(TestContext ctx, String expected) {
        var actual = field(ctx).getText();
        ctx.check("path box", expected.equals(actual), expected, actual);
    }

    private static AtomicReference<File> result(TestContext ctx) {
        return ctx.get(RESULT);
    }

    private static List<Dialog> errors(TestContext ctx) {
        return ctx.query().type(Dialog.class).list().stream()
                .map(match -> match.as(Dialog.class))
                .filter(dialog -> dialog != ctx.get(DIALOG))
                .toList();
    }

    private static TextField field(TestContext ctx) {
        return (TextField) find(ctx.get(DIALOG), TextField.class::isInstance);
    }

    @SuppressWarnings("unchecked")
    private static TreeList<FileNode> tree(TestContext ctx) {
        return (TreeList<FileNode>) find(ctx.get(DIALOG), TreeList.class::isInstance);
    }

    @Nullable
    private static FileNode selectedNode(TestContext ctx) {
        return tree(ctx).getSelected().stream().findFirst().orElse(null);
    }

    private static boolean isSelected(TestContext ctx, String relative) {
        var node = selectedNode(ctx);
        var expected = relative.isEmpty() ? ctx.<File>get(ROOT) : new File(ctx.<File>get(ROOT), relative);
        return node != null && canonical(node.getKey()).equals(canonical(expected));
    }

    @Nullable
    private static UIElement find(UIElement element, Predicate<UIElement> predicate) {
        if (predicate.test(element)) return element;
        for (var child : element.getChildren()) {
            var found = find(child, predicate);
            if (found != null) return found;
        }
        return null;
    }

    private static File canonical(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            return file.getAbsoluteFile();
        }
    }
}
