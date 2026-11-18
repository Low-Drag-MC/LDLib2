package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.client.GameFrameCapture;
import com.lowdragmc.lowdraglib2.editor.resource.FileResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.UIResource;
import com.lowdragmc.lowdraglib2.editor.resource.UIResourceProviderContainer;
import com.lowdragmc.lowdraglib2.gui.editor.view.UIEditorView;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate;
import com.lowdragmc.lowdraglib2.gui.ui.layout.LayoutProperties;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.capture.FrameCapture;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.vfyjxf.taffy.style.TaffyDimension;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;

/**
 * The UI editor's HUD preview: the toggle previews the UI at the game screen's size over the game, with its root
 * filling the screen and kept so, as an edit that undoes; the simulation runs at that size; a new HUD from the
 * resource panel opens in HUD mode, its root a three by three grid of regions a third of the screen each way.
 */
@LDLRegisterClient(name = "ui_editor_hud", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class UIEditorHudScenario implements UIScenario {
    private static final String VIEW = "view", SAVED = "saved", HIDE_GUI = "hide_gui", PITCH = "pitch", SCALE = "scale";
    private static final List<String> REGIONS = List.of(
            "top_left", "top_center", "top_right",
            "middle_left", "middle_center", "middle_right",
            "bottom_left", "bottom_center", "bottom_right");

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(30).tags("editor", "hud").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.step("show the HUD and look up a little, the background is the game as it is seen", ctx -> {
                    ctx.put(HIDE_GUI, ctx.mc().options.hideGui);
                    ctx.put(PITCH, ctx.player().getXRot());
                    ctx.mc().options.hideGui = false;
                    ctx.player().setXRot(-15);
                })
                .teardown("hide the HUD again", ctx -> {
                    ctx.mc().options.hideGui = Boolean.TRUE.equals(ctx.get(HIDE_GUI));
                    if (ctx.get(PITCH) instanceof Float pitch) {
                        ctx.player().setXRot(pitch);
                    }
                })
                .openModularUI("ui_editor", UIEditorHudScenario::buildUI)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .frames(3)
                .check("a UI that is not a HUD is previewed at the view's size", ctx -> !view(ctx).isHudMode()
                        && previewWidth(ctx) == (int) view(ctx).graphView.getContentWidth()
                        && previewHeight(ctx) == (int) view(ctx).graphView.getContentHeight())

                .group("the HUD toggle", g -> g
                        .click("the HUD toggle", UIEditorHudScenario::hudToggle, Keys.MOUSE_LEFT)
                        .check("turns HUD mode on, in the template", ctx -> view(ctx).isHudMode()
                                && view(ctx).getTemplate().isHud())
                        .check("previews at the game screen's size", ctx ->
                                List.of(previewWidth(ctx), previewHeight(ctx)).equals(screenSize(ctx)))
                        .check("with the root filling it", UIEditorHudScenario::rootFillsScreen)
                        .waitUntil("the game's frame is copied for the background", ctx -> GameFrameCapture.texture() >= 0)
                        .frames(3)
                        .step("the preview shows the game's frame", UIEditorHudScenario::showsTheGame))

                .group("the root is kept filling the screen", g -> g
                        .step("give the root a width of its own", ctx -> root(ctx).layout(layout -> layout.width(50)))
                        .waitUntil("it fills the screen again", UIEditorHudScenario::rootFillsScreen))

                .group("turning HUD mode on is an edit", g -> g
                        .step("remember the zoom that fits the screen", ctx -> ctx.put(SCALE, view(ctx).graphView.getScale()))
                        .step("focus the editor", ctx -> view(ctx).focus())
                        .key(GLFW.GLFW_KEY_Z, Keys.MOD_CONTROL)
                        .check("undo turns it off", ctx -> !view(ctx).isHudMode())
                        .check("and gives the root its size back", ctx -> isLength(root(ctx), 150))
                        .check("previewed at the view's size again", ctx ->
                                previewWidth(ctx) == (int) view(ctx).graphView.getContentWidth())
                        .check("and fitted to it, not left zoomed out to the screen", ctx ->
                                view(ctx).graphView.getScale() > ctx.<Float>get(SCALE))
                        .key(GLFW.GLFW_KEY_Y, Keys.MOD_CONTROL)
                        .check("redo turns it on again", ctx -> view(ctx).isHudMode())
                        .check("with the root filling the screen", UIEditorHudScenario::rootFillsScreen))

                .group("the simulation runs as a HUD", g -> g
                        .step("start the simulation", ctx -> view(ctx).startSimulation())
                        .frames(3)
                        .check("at the game screen's size", ctx -> {
                            var simulated = view(ctx).canvas.getCanvasModularUI();
                            return view(ctx).canvas.isHud() && simulated != null
                                    && List.of(simulated.getScreenWidth(), simulated.getScreenHeight()).equals(screenSize(ctx));
                        })
                        .step("turn HUD mode off while it runs", ctx -> view(ctx).setHudMode(false))
                        .check("it waits for the simulation to stop", ctx -> view(ctx).isHudMode() && view(ctx).canvas.isHud())
                        .step("stop the simulation", ctx -> view(ctx).stopSimulation()))

                .group("a new HUD", g -> g
                        .check("is offered by the resource panel", UIEditorHudScenario::offersNewHud)
                        .step("open one", ctx -> view(ctx).loadTemplate(UIResourceProviderContainer.createHud(),
                                saved -> ctx.put(SAVED, saved)))
                        .frames(3)
                        .check("in HUD mode", ctx -> view(ctx).isHudMode())
                        .check("its root filling the screen", UIEditorHudScenario::rootFillsScreen)
                        .checkEquals("with nine regions", REGIONS, UIEditorHudScenario::regionIds)
                        .check("that align nothing of their own", ctx -> REGIONS.stream()
                                .map(id -> region(ctx, id).getLayoutStyle())
                                .allMatch(layout -> layout.getInline(LayoutProperties.JUSTIFY_CONTENT) == null
                                        && layout.getInline(LayoutProperties.ALIGN_ITEMS) == null))
                        .check("the top left one the top left third", ctx -> isThird(ctx, "top_left", 0, 0))
                        .check("the bottom right one the bottom right third", ctx -> isThird(ctx, "bottom_right", 2, 2))
                        .check("the middle one the middle third", ctx -> isThird(ctx, "middle_center", 1, 1))
                        .step("put something wider and taller than a third in the top left", ctx -> region(ctx, "top_left")
                                .addChild(new UIElement().setId("wide").layout(layout -> layout
                                        .width(screenSize(ctx).get(0) / 2f).height(screenSize(ctx).get(1) / 2f))))
                        .frames(3)
                        .check("the rows and columns stay thirds", ctx -> isThird(ctx, "top_left", 0, 0)
                                && isThird(ctx, "middle_center", 1, 1))
                        .step("take it out", ctx -> root(ctx).selectId("wide").findFirst().ifPresent(UIElement::removeSelf))
                        .step("move the bottom right region first in the hierarchy", ctx -> {
                            var region = region(ctx, "bottom_right");
                            region.removeSelf();
                            root(ctx).addChildAt(region, 0);
                        })
                        .frames(3)
                        .check("it stays the bottom right third", ctx -> isThird(ctx, "bottom_right", 2, 2))
                        .step("save it", ctx -> {
                            view(ctx).markAsDirty();
                            view(ctx).notifySaved();
                        })
                        .check("saved as a HUD", ctx -> ctx.<UITemplate>get(SAVED).isHud())
                        .check("and still one written and read back", ctx -> {
                            var ops = Platform.getFrozenRegistry().createSerializationContext(NbtOps.INSTANCE);
                            var tag = UITemplate.CODEC.encodeStart(ops, ctx.<UITemplate>get(SAVED)).getOrThrow();
                            return UITemplate.CODEC.parse(ops, tag).getOrThrow().isHud();
                        })
                        .check("a UI that is not one writes nothing new", ctx -> {
                            var ops = Platform.getFrozenRegistry().createSerializationContext(NbtOps.INSTANCE);
                            var tag = UITemplate.CODEC.encodeStart(ops, UITemplate.of(new UIElement())).getOrThrow();
                            return tag instanceof CompoundTag compound && !compound.contains("hud");
                        }))

                .closeScreen();
    }

    private static ModularUI buildUI(TestContext ctx) {
        var root = new UIElement().layout(layout -> layout.width(150).height(150)).addChild(
                new UIElement().setId("box").layout(layout -> layout.width(20).height(20))
                        .style(style -> style.backgroundTexture(new ColorRectTexture(0xFFE04040))));
        var view = new UIEditorView().loadTemplate(UITemplate.of(root, StylesheetManager.GDP), template -> {});
        view.layout(layout -> layout.widthPercent(100).heightPercent(100));
        // opaque, as in an editor: the game drawn under this screen must not pass for the preview's background
        view.style(style -> style.backgroundTexture(Sprites.RECT_SOLID));
        ctx.put(VIEW, view);
        return new ModularUI(UI.of(view, List.of(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MODERN))), ctx.player());
    }

    private static UIEditorView view(TestContext ctx) {
        return ctx.get(VIEW);
    }

    private static UIElement root(TestContext ctx) {
        return view(ctx).getCurrentUI().rootElement;
    }

    private static UIElement hudToggle(TestContext ctx) {
        var found = findByClass(view(ctx).header, "__ui-editor-view_hud-toggle__");
        if (found == null) throw new IllegalStateException("no HUD toggle in the header");
        return found;
    }

    @Nullable
    private static UIElement findByClass(UIElement element, String clazz) {
        if (element.hasClass(clazz)) return element;
        for (var child : element.getChildren()) {
            var found = findByClass(child, clazz);
            if (found != null) return found;
        }
        return null;
    }

    private static List<Integer> screenSize(TestContext ctx) {
        var window = ctx.mc().getWindow();
        return List.of(window.getGuiScaledWidth(), window.getGuiScaledHeight());
    }

    private static int previewWidth(TestContext ctx) {
        var modularUI = view(ctx).modularUIPreview.getPreviewModularUI();
        return modularUI == null ? -1 : modularUI.getScreenWidth();
    }

    private static int previewHeight(TestContext ctx) {
        var modularUI = view(ctx).modularUIPreview.getPreviewModularUI();
        return modularUI == null ? -1 : modularUI.getScreenHeight();
    }

    private static boolean rootFillsScreen(TestContext ctx) {
        var root = root(ctx);
        var layout = root.getLayoutStyle();
        var size = screenSize(ctx);
        return isFull(layout.getInline(LayoutProperties.WIDTH)) && isFull(layout.getInline(LayoutProperties.HEIGHT))
                && near(root.getSizeWidth(), size.get(0)) && near(root.getSizeHeight(), size.get(1));
    }

    private static boolean isFull(@Nullable TaffyDimension dimension) {
        return dimension != null && dimension.isPercent() && dimension.getValue() == 1f;
    }

    private static boolean isLength(UIElement element, float length) {
        var width = element.getLayoutStyle().getInline(LayoutProperties.WIDTH);
        return width != null && width.isLength() && width.getValue() == length;
    }

    private static boolean near(float a, float b) {
        return Math.abs(a - b) <= 1;
    }

    private static float rootX(TestContext ctx) {
        return root(ctx).getPositionX();
    }

    private static float rootY(TestContext ctx) {
        return root(ctx).getPositionY();
    }

    private static List<String> regionIds(TestContext ctx) {
        return root(ctx).getChildren().stream().map(UIElement::getId).toList();
    }

    private static UIElement region(TestContext ctx, String id) {
        return root(ctx).selectId(id).findFirst().orElseThrow(() -> new IllegalStateException("no region " + id));
    }

    /** The region is the third of the screen in the given column and row, counted from 0. */
    private static boolean isThird(TestContext ctx, String id, int column, int row) {
        var region = region(ctx, id);
        var width = screenSize(ctx).get(0) / 3f;
        var height = screenSize(ctx).get(1) / 3f;
        return near(region.getPositionX(), rootX(ctx) + column * width) && near(region.getPositionY(), rootY(ctx) + row * height)
                && near(region.getSizeWidth(), width) && near(region.getSizeHeight(), height);
    }

    private static boolean offersNewHud(TestContext ctx) {
        File folder;
        try {
            folder = Files.createTempDirectory("ldlib2_ui_editor_hud").toFile();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        try {
            var provider = new FileResourceProvider<>(UIResource.INSTANCE.getResourceInstance(), folder);
            var container = new UIResourceProviderContainer(provider);
            var menu = TreeBuilder.Menu.start();
            var onCreateMenu = container.getOnCreateMenu();
            if (onCreateMenu == null) return false;
            onCreateMenu.accept(container, menu);
            for (var entry : menu.build().getChildren()) {
                if (TreeBuilder.Menu.getName(entry.getKey()).getContents() instanceof TranslatableContents translatable
                        && translatable.getKey().equals("ldlib.gui.editor.menu.add_hud")) {
                    return true;
                }
            }
            return false;
        } finally {
            deleteQuietly(folder);
        }
    }

    private static void deleteQuietly(File file) {
        var children = file.listFiles();
        if (children != null) {
            for (var child : children) deleteQuietly(child);
        }
        file.delete();
    }

    /**
     * Two points of the screen, one in the sky and one on the hotbar, look the same in the preview as in the copy of the
     * game's frame; and they differ from each other, so a preview showing anything else could not pass.
     */
    private static void showsTheGame(TestContext ctx) {
        var size = screenSize(ctx);
        var hotbarV = 1 - 10f / size.get(1);
        try (var shown = FrameCapture.grab(); var picture = picture()) {
            var pictureSky = pictureAt(picture, 0.5f, 0.25f);
            var pictureHotbar = pictureAt(picture, 0.5f, hotbarV);
            var shownSky = shownAt(ctx, shown, 0.5f, 0.25f);
            var shownHotbar = shownAt(ctx, shown, 0.5f, hotbarV);
            ctx.check("the sky and the hotbar differ in the picture", distance(pictureSky, pictureHotbar) > 40,
                    "> 40", distance(pictureSky, pictureHotbar));
            ctx.check("the sky is shown as in the picture", distance(pictureSky, shownSky) <= 30,
                    hex(pictureSky), hex(shownSky));
            ctx.check("the hotbar is shown as in the picture", distance(pictureHotbar, shownHotbar) <= 30,
                    hex(pictureHotbar), hex(shownHotbar));
        }
    }

    private static NativeImage picture() {
        var image = new NativeImage(GameFrameCapture.width(), GameFrameCapture.height(), false);
        RenderSystem.bindTexture(GameFrameCapture.texture());
        image.downloadTexture(0, true);
        image.flipY();
        return image;
    }

    private static int pictureAt(NativeImage picture, float u, float v) {
        return picture.getPixelRGBA((int) (u * (picture.getWidth() - 1)), (int) (v * (picture.getHeight() - 1)));
    }

    /** The window's pixel where the preview draws the screen's point (u, v), through the graph view's pan and zoom. */
    private static int shownAt(TestContext ctx, NativeImage shown, float u, float v) {
        var view = view(ctx);
        var graph = view.graphView;
        var modularUI = view.modularUIPreview.getPreviewModularUI();
        var originX = graph.contentRoot.getPositionX();
        var originY = graph.contentRoot.getPositionY();
        var x = view.modularUIPreview.getPositionX() - originX + modularUI.getLeftPos() + u * modularUI.getScreenWidth();
        var y = view.modularUIPreview.getPositionY() - originY + modularUI.getTopPos() + v * modularUI.getScreenHeight();
        var guiScale = ctx.mc().getWindow().getGuiScale();
        var windowX = (originX + (x - graph.getOffsetX()) * graph.getScale()) * guiScale;
        var windowY = (originY + (y - graph.getOffsetY()) * graph.getScale()) * guiScale;
        return shown.getPixelRGBA((int) windowX, (int) windowY);
    }

    private static int distance(int a, int b) {
        var distance = 0;
        for (var shift = 0; shift < 24; shift += 8) {
            distance = Math.max(distance, Math.abs(((a >> shift) & 0xFF) - ((b >> shift) & 0xFF)));
        }
        return distance;
    }

    private static String hex(int abgr) {
        return "#%02x%02x%02x".formatted(abgr & 0xFF, (abgr >> 8) & 0xFF, (abgr >> 16) & 0xFF);
    }
}
