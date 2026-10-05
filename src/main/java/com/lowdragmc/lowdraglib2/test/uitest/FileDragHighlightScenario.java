package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.window.OsWindowManager;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.editor.ui.browser.AssetBrowser;
import com.lowdragmc.lowdraglib2.editor.ui.browser.FileOps;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.UISurface;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.TestEditor;
import com.lowdragmc.lowdraglib2.uitest.ElementBounds;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.target.Texts;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_Event;

import java.io.File;

/**
 * Files dragged in from outside the game light up the folder they would be dropped into, over the game window and
 * over an editor in a window of its own.
 *
 * <p>Driven through SDL's own queue rather than by calling the UI: the events are pushed as the platform pushes
 * them, so this covers both routes they take. For the game window, vanilla's poll loop hands
 * {@code SDL_EVENT_DROP_POSITION} to a window that ignores it, where a mixin catches it; for one of LDLib's own
 * windows, the event filter takes it out of the queue first. Either way the drag then ends with nothing dropped,
 * which SDL reports as a {@code SDL_EVENT_DROP_COMPLETE} of its own.
 */
@LDLRegisterClient(name = "file_drag_highlight", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class FileDragHighlightScenario implements UIScenario {

    /** Its own id, so the scenario cannot collide with a real editor's window. */
    private static final Identifier WINDOW_ID = LDLib2.id("uitest_file_drag");

    private static final String WINDOW = "editor_window";
    private static final String ROOT = "browser_root";
    private static final String FOLDER = "drag_target";
    private static final String DROP_TARGET = "__drop-target__";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("editor", "assets", "window");
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("editor", ctx -> {
                    var window = EditorWindow.open(WINDOW_ID, TestEditor::new);
                    ctx.put(WINDOW, window);
                    return new ModularUI(UI.of(window), ctx.player())
                            .shouldCloseOnEsc(false)
                            .shouldCloseOnKeyInventory(false);
                })
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the editor has laid out", ctx -> editor(ctx).centerWindow.getSizeWidth() > 0)
                .step("show a folder with a folder in it", ctx -> {
                    var root = new File(ctx.mc().gameDirectory, "ldlib2-uitest/file_drag");
                    ctx.put(ROOT, root);
                    ctx.require("could create the folders", new File(root, FOLDER).isDirectory()
                            || new File(root, FOLDER).mkdirs());
                    var view = editor(ctx).resourceView;
                    var container = view.getViewContainer();
                    ctx.require("the resource view is docked", container != null);
                    container.selectView(view);
                    browser(ctx).setRoot(root);
                    browser(ctx).openDirectory(root);
                })
                .waitUntil("the folder's cell is in the grid", ctx -> folderCell(ctx) != null)

                .group("over the game window", g -> g
                        .check("nothing is lit before a drag", ctx -> tintCount(ctx) == 0)
                        .step("drag files over the folder's cell", ctx -> {
                            var cell = ElementBounds.of(folderCell(ctx));
                            pushDrop(ctx, SDLEvents.SDL_EVENT_DROP_BEGIN, cell.centerX(), cell.centerY());
                            pushDrop(ctx, SDLEvents.SDL_EVENT_DROP_POSITION, cell.centerX(), cell.centerY());
                        })
                        .waitUntil("the folder's cell lights up", ctx -> dropTarget(ctx) == folderCell(ctx))
                        .step("drag them on, past the cells", ctx -> {
                            var grid = ElementBounds.of(browser(ctx).gridScroller);
                            pushDrop(ctx, SDLEvents.SDL_EVENT_DROP_POSITION, grid.right() - 12, grid.bottom() - 12);
                        })
                        .waitUntil("the open folder's grid lights up instead",
                                ctx -> dropTarget(ctx) == browser(ctx).gridScroller)
                        .check("and only it", ctx -> tintCount(ctx) == 1)
                        .step("take them away again without dropping", ctx ->
                                pushDrop(ctx, SDLEvents.SDL_EVENT_DROP_COMPLETE, 0, 0))
                        .waitUntil("nothing is lit", ctx -> tintCount(ctx) == 0))

                .group("over an editor in a window of its own", g -> g
                        .step("pop the editor out", ctx -> ctx.require("popOutToOsWindow reported success",
                                window(ctx).popOutToOsWindow()))
                        // created, rendered into and presented more than once
                        .frames(20)
                        .waitUntil("the folder's cell is laid out in the window", ctx -> {
                            var cell = folderCell(ctx);
                            return cell != null && cell.getSizeWidth() > 0;
                        })
                        .step("drag files over the folder's cell", ctx -> {
                            var cell = ElementBounds.of(folderCell(ctx));
                            pushDrop(ctx, SDLEvents.SDL_EVENT_DROP_BEGIN, cell.centerX(), cell.centerY());
                            pushDrop(ctx, SDLEvents.SDL_EVENT_DROP_POSITION, cell.centerX(), cell.centerY());
                        })
                        .waitUntil("the folder's cell lights up", ctx -> dropTarget(ctx) == folderCell(ctx))
                        .step("take them away again without dropping", ctx ->
                                pushDrop(ctx, SDLEvents.SDL_EVENT_DROP_COMPLETE, 0, 0))
                        .waitUntil("nothing is lit", ctx -> tintCount(ctx) == 0)
                        .check("the window never threw", ctx -> OsWindowManager.totalFailures() == 0))

                .teardown("close the editor's window and remove the folders", ctx -> {
                    for (var host : OsWindowManager.hosts()) {
                        OsWindowManager.close(host);
                    }
                    var root = ctx.<File>get(ROOT);
                    if (root != null) {
                        FileOps.deleteRecursively(root);
                    }
                })
                .closeScreen();
    }

    /**
     * Queues a drop event for the window the editor is in, at a point in its UI's gui coordinates, converted to
     * window coordinates, which SDL reports in.
     */
    private static void pushDrop(TestContext ctx, int type, float guiX, float guiY) {
        var osWindow = window(ctx).getOsWindow();
        var inOsWindow = osWindow != null && osWindow.isOpen();
        UISurface surface = inOsWindow ? osWindow.surface() : UISurface.main();
        var handle = inOsWindow ? osWindow.window().handle() : ctx.mc().getWindow().handle();
        try (var event = SDL_Event.calloc()) {
            event.drop()
                    .type(type)
                    .windowID(SDLVideo.SDL_GetWindowID(handle))
                    .x(guiX * surface.screenWidth() / surface.guiScaledWidth())
                    .y(guiY * surface.screenHeight() / surface.guiScaledHeight());
            var queued = SDLEvents.SDL_PushEvent(event);
            // an event for one of LDLib's windows is taken by its event filter, which SDL reports as not queued
            ctx.require("SDL took the event", queued || inOsWindow);
        }
    }

    /**
     * The grid cell of {@link #FOLDER}: its label's parent. The label itself, since a container whose only text is
     * the label's has that text too.
     */
    @Nullable
    private static UIElement folderCell(TestContext ctx) {
        return browser(ctx).gridScroller.allChildrenStream()
                .filter(element -> element instanceof Label && FOLDER.equals(Texts.of(element)))
                .map(UIElement::getParent)
                .findFirst().orElse(null);
    }

    /** The element the drop tint is on, if there is one. */
    @Nullable
    private static UIElement dropTarget(TestContext ctx) {
        return browser(ctx).selfAndAllChildren()
                .filter(element -> element.hasClass(DROP_TARGET))
                .map(UIElement::getParent)
                .findFirst().orElse(null);
    }

    private static long tintCount(TestContext ctx) {
        return browser(ctx).selfAndAllChildren().filter(element -> element.hasClass(DROP_TARGET)).count();
    }

    private static EditorWindow window(TestContext ctx) {
        return ctx.get(WINDOW);
    }

    private static Editor editor(TestContext ctx) {
        var editor = window(ctx).getCurrentEditor();
        if (editor == null) {
            throw new IllegalStateException("The editor window has no editor");
        }
        return editor;
    }

    private static AssetBrowser browser(TestContext ctx) {
        return editor(ctx).resourceView.getAssetBrowser();
    }
}
