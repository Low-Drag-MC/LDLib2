package com.lowdragmc.lowdraglib2.editor.ui;

import com.google.common.collect.Maps;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.window.OsWindowManager;
import com.lowdragmc.lowdraglib2.editor.settings.AppearanceSettings;
import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import com.lowdragmc.lowdraglib2.gui.ui.window.ModularUIWindow;
import com.lowdragmc.lowdraglib2.gui.ui.window.WindowBounds;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.util.WindowDragHelper;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;

import org.jetbrains.annotations.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class EditorWindow extends UIElement {
    public static final Identifier DEFAULT_ID = LDLib2.id("default");
    /** Element id of the title bar button that moves the editor between the game window and its own. */
    public static final String HOST_TOGGLE_ID = "editor_host_toggle";
    private static final Map<Identifier, EditorWindow> MINIMIZED_WINDOWS = Maps.newConcurrentMap();

    public final UIElement window = new UIElement();
    public final UIElement editorButtonContainer = new UIElement();
    public final UIElement editorContainer = new UIElement();
    @Nullable
    public final Identifier windowID;

    // runtime
    private int initialScreenScale;
    @Getter
    private boolean maximized = true;
    private float windowWidth = 300;
    private float windowHeight = 200;
    private float windowLeft = -150;
    private float windowTop = -100;
    protected boolean isResizing = false;
    @Getter
    @Nullable
    private Editor currentEditor;
    @Getter
    private final LinkedHashMap<Editor, UIElement> editors = new LinkedHashMap<>();
    /** The OS window hosting this editor, or null when it is a screen in the game window. */
    @Getter
    @Nullable
    private EditorOsWindow osWindow;
    /**
     * Set while moving between hosts. The host being left still holds this element as its root and
     * calls {@link #onRemoved()} on the way out, which would tear down the editor just moved.
     */
    private boolean rehosting;

    public static EditorWindow openDefault(Supplier<Editor> editorCreator) {
        return open(DEFAULT_ID, editorCreator);
    }

    /**
     * Opens {@code windowID}'s editor and shows it, creating it if it does not exist and restoring it
     * if it was minimized.
     *
     * @return the editor window, or {@code null} when it was already open in a window of its own,
     *         which is focused instead
     */
    @Nullable
    public static EditorWindow show(Identifier windowID, Supplier<Editor> editorCreator) {
        // Before open(): an editor in a window of its own is not in the minimized map, so open()
        // would build a second editor beside the one already on the player's other monitor.
        var alreadyOpen = osWindowOf(windowID);
        if (alreadyOpen != null && alreadyOpen.isOpen()) {
            alreadyOpen.window().focus();
            return null;
        }
        var editorWindow = open(windowID, editorCreator);
        editorWindow.show();
        return editorWindow;
    }

    /** Shows this editor: a screen in the game window unless it was last left in one of its own. */
    public void show() {
        if (isInOsWindow()) return;
        if (prefersOsWindow() && popOutToOsWindow()) return;
        hostAsScreen();
    }

    /**
     * Shows this editor in the game window, as a screen. Esc and the inventory key are not close keys:
     * both are ordinary typing in an editor.
     */
    public void hostAsScreen() {
        var ui = new ModularUI(UI.of(this))
                .shouldCloseOnEsc(false)
                .shouldCloseOnKeyInventory(false);
        Minecraft.getInstance().setScreen(new ModularUIScreen(ui, Component.empty()));
    }

    public static EditorWindow open(Identifier windowID, Supplier<Editor> editorCreator) {
        var editorWindow = MINIMIZED_WINDOWS.remove(windowID);
        if (editorWindow != null && LDLib2.isClient()) {
            ClientSupport.restoreGuiScale(editorWindow);
            ClientSupport.notifyOpenToast();
            return editorWindow;
        }
        return new EditorWindow(windowID, editorCreator);
    }

    public EditorWindow(Supplier<Editor> editorCreator) {
        this(null, editorCreator);
    }

    public EditorWindow(@Nullable Identifier windowID, Supplier<Editor> editorCreator) {
        this.windowID = windowID;

        if (LDLib2.isClient()) {
            var minecraft = Minecraft.getInstance();
            initialScreenScale = minecraft.options.guiScale().get();
        }

        getLayout().widthPercent(100);
        getLayout().heightPercent(100);

        this.editorButtonContainer.layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.top(15);
            layout.widthPercent(100);
            layout.gapAll(1);
            layout.height(14);
        }).setDisplay(false).style(style -> style.backgroundTexture(ColorPattern.BLACK.rectTexture()));
        this.editorButtonContainer.addClass("__editor-window_editor-button-container__").moveInlineAsDefault();

        this.editorContainer.getLayout().widthPercent(100).flex(1);
        this.editorContainer.addClass("__editor-window_editor-container__").moveInlineAsDefault();
        this.window.layout(layout -> layout.widthPercent(100).heightPercent(100))
                .addChildren(this.editorContainer, this.editorButtonContainer);
        WindowDragHelper.setBorderResize(this.window, this.window, 4,
                new Vector2f(200f, 150f),
                new Vector2f(Float.MAX_VALUE), e -> !isMaximized(), (e, handle) -> {
                    isResizing = true;
                    return true;
                }, e -> isResizing = false);

        addChild(window);
        createNewEditor(editorCreator);
    }

    @Override
    protected void onRemoved() {
        if (rehosting) return;
        if (windowID != null && MINIMIZED_WINDOWS.containsKey(windowID)) return;
        super.onRemoved();
    }

    /** The editor for {@code windowID} currently open in a window of its own, or null. */
    @Nullable
    public static EditorOsWindow osWindowOf(Identifier windowID) {
        for (var window : ModularUIWindow.openWindows()) {
            if (window instanceof EditorOsWindow editorOsWindow
                    && windowID.equals(editorOsWindow.getEditorWindow().windowID)) {
                return editorOsWindow;
            }
        }
        return null;
    }

    /** Whether this editor is hosted in a window of its own right now. */
    public boolean isInOsWindow() {
        return osWindow != null && osWindow.isOpen();
    }

    /** Owned by {@link EditorOsWindow}. */
    void _setOsWindowInternal(@Nullable EditorOsWindow osWindow) {
        this.osWindow = osWindow;
    }

    /**
     * Moves this editor out of the game window into one of its own.
     *
     * @return {@code false} if no window could be opened, in which case nothing has moved
     */
    public boolean popOutToOsWindow() {
        if (isInOsWindow()) return true;
        if (!LDLib2.isClient() || !OsWindowManager.isAvailable()) return false;

        var mui = getModularUI();
        var previousScreen = mui == null ? null : ModularUIClientAccess.getScreen(mui);
        // It owns the whole of its own window; a restored rectangle would be a window inside a window.
        maximizeWindow();

        var window = new EditorOsWindow(this);
        var bounds = initialBounds();
        rehosting = true;
        try {
            // Open first, close the screen only once it succeeded: opening is what re-hosts the
            // subtree, so a failure here leaves the editor where it was rather than homeless.
            if (!window.open(bounds.x(), bounds.y(), bounds.width(), bounds.height(), false)) {
                _setOsWindowInternal(null);
                return false;
            }
            if (previousScreen != null) {
                previousScreen.onClose();
            }
        } finally {
            rehosting = false;
        }
        // The scale was raised for an editor filling the screen; the player is looking at the HUD now.
        restoreGameGuiScale();
        return true;
    }

    /**
     * Moves this editor back into the game window as a screen.
     *
     * @return {@code false} if it was not in a window of its own to begin with
     */
    public boolean dockIntoGameWindow() {
        if (osWindow == null) return false;
        var window = osWindow;
        rehosting = true;
        try {
            // The screen's init re-hosts the subtree; closing first would leave it with no host.
            hostAsScreen();
            window.close();
        } finally {
            rehosting = false;
        }
        rememberPreferredHost(false);
        return true;
    }

    /** Where it was last left, or a rectangle inset from the game window. */
    private WindowBounds initialBounds() {
        var remembered = appearance().map(AppearanceSettings::getEditorWindowBounds).orElse(null);
        if (remembered != null && remembered.width() > 0 && remembered.height() > 0) {
            return remembered.atLeastMinimum();
        }
        var gameWindow = Minecraft.getInstance().getWindow();
        var width = Math.max(ModularUIWindow.MIN_WIDTH, (int) (gameWindow.getScreenWidth() * 0.8));
        var height = Math.max(ModularUIWindow.MIN_HEIGHT, (int) (gameWindow.getScreenHeight() * 0.8));
        return new WindowBounds(Integer.MIN_VALUE, Integer.MIN_VALUE, width, height);
    }

    /** Records where the window was left, so it opens there next time. */
    void rememberBounds(@Nullable WindowBounds bounds) {
        if (bounds == null) return;
        appearance().ifPresent(appearance -> appearance.setEditorWindowBounds(bounds));
    }

    /** Records the host to use next time, so the toggle is not a choice to redo every session. */
    void rememberPreferredHost(boolean preferOsWindow) {
        appearance().ifPresent(appearance -> appearance.setPreferOsWindow(preferOsWindow));
    }

    /** Whether the title bar toggle last left this editor in a window of its own. */
    public boolean prefersOsWindow() {
        return appearance().map(AppearanceSettings::isPreferOsWindow).orElse(false);
    }

    private Optional<AppearanceSettings> appearance() {
        if (currentEditor == null) return Optional.empty();
        return currentEditor.editorSettings.getSettings(AppearanceSettings.ID)
                .filter(AppearanceSettings.class::isInstance)
                .map(AppearanceSettings.class::cast);
    }

    /** Whether the editor fills its host: its own rectangle in the game window, the monitor otherwise. */
    public boolean isShowingMaximized() {
        return isInOsWindow() ? osWindow.isMaximized() : isMaximized();
    }

    /** What the maximize button and a double click on the title bar do, in either host. */
    public void toggleMaximized() {
        if (isInOsWindow()) {
            osWindow.toggleMaximized();
        } else if (isMaximized()) {
            retoreWindow();
        } else {
            maximizeWindow();
        }
    }

    public boolean hasMultipleEditors() {
        return editors.size() > 1;
    }

    public void showEditor(Editor editor) {
        if (currentEditor == editor) return;
        if (currentEditor != null) {
            currentEditor.setDisplay(false);
        }
        currentEditor = editor;
        editor.setDisplay(true);
        // The window is dragged by a title bar, and the one on screen is this editor's.
        if (osWindow != null) {
            osWindow.syncDragArea();
        }
        editor.mainView.layout(layout -> {
            layout.marginTop(hasMultipleEditors() ? 14 : 0);
        });
        editorButtonContainer.setDisplay(hasMultipleEditors());
        for (var entry : editors.entrySet()) {
            var isCurrent = entry.getKey() == currentEditor;
            entry.getValue().style(style -> {
                style.setPipelineState(StyleOrigin.DEFAULT);
                style.backgroundTexture(isCurrent ? ColorPattern.SLATE_PLUM.rectTexture() : ColorPattern.DARK_GRAY.rectTexture());
                style.setPipelineState(StyleOrigin.INLINE);
            }).addClass(isCurrent ? "__editor-window_active__" : "__editor-window_inactive__")
                    .removeClass(isCurrent ? "__editor-window_inactive__" : "__editor-window_active__");
        }
    }

    public Editor createNewEditor(Supplier<Editor> editorCreator) {
        var newEditor = editorCreator.get();
        newEditor._setEditorWindowInternal(this);
        // init window buttons
        newEditor.buttonContainer.addChildAt(new Button().noText()
                .addPreIcon(DynamicTexture.of(() -> isShowingMaximized() ? Icons.WINDOW_RESTORE : Icons.WINDOW_MAXIMIZE))
                .setOnClick(e -> toggleMaximized())
                .addClass("__white_icon__")
                .layout(layout -> layout.height(12)), 0);
        if (windowID != null) {
            newEditor.buttonContainer.addChildAt(new Button().noText()
                    .addPreIcon(Icons.WINDOW_MINIMIZE)
                    .setOnClick(e -> minimizeWindow())
                    .addClass("__white_icon__")
                    .layout(layout -> layout.height(12)), 0);
        }
        // Moves between the game window and a window of its own. Hidden where none can be opened.
        newEditor.buttonContainer.addChildAt(new Button().noText()
                .addPreIcon(DynamicTexture.of(() -> isInOsWindow() ? Icons.SCREEN : Icons.FLOAT))
                .setOnClick(e -> {
                    if (isInOsWindow()) {
                        dockIntoGameWindow();
                    } else if (popOutToOsWindow()) {
                        rememberPreferredHost(true);
                    }
                })
                .addClass("__white_icon__")
                .layout(layout -> layout.height(12))
                .addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = HoverTooltips.create(
                        Component.translatable(isInOsWindow()
                                ? "editor.window.dock_into_game" : "editor.window.open_in_window")))
                .addEventListener(UIEvents.TICK, e ->
                        e.target.setDisplay(isInOsWindow() || OsWindowManager.isAvailable()))
                .setId(HOST_TOGGLE_ID), 0);
        newEditor.topPlaceholder.addEventListener(UIEvents.DOUBLE_CLICK, e -> {
            if (newEditor.getWindow() != this) return;
            toggleMaximized();
        });
        newEditor.topPlaceholder.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            // In an OS window ModularUIWindow takes the press, and there is no rectangle to drag.
            if (newEditor.getWindow() == this && !isInOsWindow() && !isMaximized()) {
                e.target.startDrag(new Vector2f(windowLeft, windowTop), null);
            }
        });
        newEditor.topPlaceholder.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, e -> {
            if (newEditor.getWindow() == this && e.dragHandler.getDraggingObject() instanceof Vector2f pos) {
                windowLeft = pos.x + e.x - e.dragStartX;
                windowTop = pos.y + e.y - e.dragStartY;
                window.layout(layout -> layout
                        .left(windowLeft)
                        .top(windowTop)
                );
            }
        });
        // editor button
        var button = createEditorButton(newEditor);
        editorButtonContainer.addChild(button);
        // show editor
        editorContainer.addChildAt(newEditor, editors.size());
        editors.put(newEditor, button);
        showEditor(newEditor);
        return newEditor;
    }

    /**
     * Removes the specified {@link Editor} from the {@code EditorWindow}. Notes, it won't save the dirty project of the editor.
     * To save the project, use {@link Editor#exit()} instead.
     *
     * @param editor the {@link Editor} to be removed from the {@code EditorWindow}.
     */
    public void removeEditor(Editor editor) {
        var button = editors.remove(editor);
        if (button != null) {
            editorButtonContainer.removeChild(button);
        }
        if (editors.isEmpty()) {
            currentEditor = null;
            hideHost();
        } else {
            showEditor(editors.lastEntry().getKey());
        }
    }

    /**
     * Closes the current editor in the {@code EditorWindow}.
     * <p>
     * If there is a current editor, it will invoke its {@code exit} method,
     * passing the {@code close} method as a callback to be executed after
     * the editor has been closed. The {@code exit} method manages the
     * editor's closure lifecycle, ensuring any required cleanup or saving prompts.
     * <p>
     * If no current editor exists, no action is taken.
     *
     * @see Editor#exit(Runnable) for details on how the editor closes
     */
    public void closeWindow() {
        if (currentEditor != null) {
            currentEditor.exit(this::closeWindow);
        }
    }

    /**
     * Whether this window can be minimized at all.
     *
     * <p>It needs an id: minimized windows are parked in a map keyed by it and re-opened through
     * {@link #open}, so a window without one would close with no way back. That is why the title bar
     * only shows the minimize button when there is an id.
     */
    public boolean canMinimize() {
        return windowID != null;
    }

    public void minimizeWindow() {
        // Guarded rather than left to the caller: the button is hidden for an id-less window, but a
        // keymap action or a script has no way to know that, and the map this parks the window in is a
        // ConcurrentHashMap - a null key there is an NPE from somewhere far away from the cause.
        if (!canMinimize()) return;
        if (EditorWindow.MINIMIZED_WINDOWS.containsKey(windowID)) return;
        EditorWindow.MINIMIZED_WINDOWS.put(windowID, this);
        hideHost();
    }

    /** Takes down whatever this editor is shown in: its screen, or its own window. */
    private void hideHost() {
        if (osWindow != null) {
            // No scale to restore: in a window of its own the editor never changed it, so restoring
            // would undo a change the player made themselves.
            osWindow.close();
            return;
        }
        restoreGameGuiScale();
        if (getModularUI() != null && ModularUIClientAccess.getScreen(getModularUI()) != null) {
            ModularUIClientAccess.getScreen(getModularUI()).onClose();
        }
    }

    /** Puts the game's gui scale back to what it was before a screen-hosted editor changed it. */
    private void restoreGameGuiScale() {
        if (!LDLib2.isClient()) return;
        var minecraft = Minecraft.getInstance();
        var guiScale = minecraft.options.guiScale();
        if (guiScale.get() != initialScreenScale) {
            guiScale.set(initialScreenScale);
            minecraft.resizeGui();
        }
    }

    public void maximizeWindow() {
        if (maximized) return;
        layout(layout -> layout.widthPercent(100).heightPercent(100));
        window.layout(layout -> layout
                .positionType(TaffyPosition.RELATIVE)
                .paddingAll(0)
                .left(0)
                .top(0)
                .widthPercent(100)
                .heightPercent(100)
        );
        maximized = true;

        var mui = getModularUI();
        var minecraft = Minecraft.getInstance();
        if (mui != null && ModularUIClientAccess.getScreen(mui) != null) {
            ModularUIClientAccess.getScreen(mui).init(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
        }
    }

    public void retoreWindow() {
        // In an OS window the editor IS the window; a rectangle inside it has nothing behind it.
        if (isInOsWindow()) return;
        if (!maximized) return;
        // at least 1px to display xei.
        layout(layout -> layout.width(1).height(1));
        window.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .paddingAll(3)
                .left(windowLeft)
                .top(windowTop)
                .width(windowWidth)
                .height(windowHeight)
        );
        var minecraft = Minecraft.getInstance();
        maximized = false;

        var mui = getModularUI();
        if (mui != null && ModularUIClientAccess.getScreen(mui) != null) {
            ModularUIClientAccess.getScreen(mui).init(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
        }
    }

    protected UIElement createEditorButton(Editor editor) {
        return new UIElement().layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
            layout.heightPercent(100);
            layout.alignItems(AlignItems.CENTER);
            layout.flex(1);
        }).style(style -> {
            style.setPipelineState(StyleOrigin.DEFAULT);
            style.backgroundTexture(currentEditor == editor ? ColorPattern.SLATE_PLUM.rectTexture() : ColorPattern.DARK_GRAY.rectTexture());
            style.setPipelineState(StyleOrigin.INLINE);
        }).addClass("__editor-window_editor-button__").moveInlineAsDefault().addChildren(
                new TextElement().setText(editor.getTitle()).textStyle(style -> style
                                .textAlignVertical(Vertical.CENTER)
                                .textAlignHorizontal(Horizontal.CENTER)
                                .textWrap(TextWrap.HOVER_ROLL)
                        )
                        .layout(layout -> {
                            layout.heightPercent(100);
                            layout.flex(1);
                        }).addEventListener(UIEvents.TICK, e -> {
                            if (e.target.getModularUI().getTickCounter() % 20 ==0) {
                                var currentTitle = editor.getTitle();
                                if (e.target instanceof TextElement text && !text.getText().equals(currentTitle)) {
                                    text.setText(currentTitle);
                                }
                            }
                        }).setOverflowVisible(false),
                new Button().noText().buttonStyle(style -> {
                    style.baseTexture(Icons.CLOSE);
                    style.hoverTexture(Icons.CLOSE.copy().setColor(ColorPattern.GRAY.color));
                    style.pressedTexture(Icons.CLOSE);
                }).setOnClick(e -> {
                    showEditor(editor);
                    editor.exit();
                    e.stopPropagation();
                }).layout(layout -> {
                    layout.height(9);
                    layout.setAspectRatio(1);
                    layout.marginRight(2);
                })
        ).addEventListener(UIEvents.MOUSE_DOWN, e -> showEditor(editor));
    }

    @Override
    protected void drawBackgroundAdditional(@NotNull IGUIContext guiContext) {
        if (!(guiContext instanceof GUIContext context)) return;
        super.drawBackgroundAdditional(context);
        if (window.isSelfOrChildHover() && !isResizing && !isMaximized()) {
            WindowDragHelper.drawResizeIcon(context, window, 4);
        }
    }

    public static class ClientSupport {
        public static void notifyOpenToast() {
            Minecraft.getInstance().getToastManager().addToast(new SystemToast(
                    new SystemToast.SystemToastId(1000L),
                    Component.translatable("editor.minimized.title"),
                    Component.translatable("editor.minimized.tips")
            ));
        }

        public static void restoreGuiScale(EditorWindow editorWindow) {
            if (editorWindow.currentEditor != null && LDLib2.isClient()) {
                editorWindow.currentEditor.editorSettings.getSettings(AppearanceSettings.ID).ifPresent(settings -> {
                    if (settings instanceof AppearanceSettings appearanceSettings) {
                        var scale = appearanceSettings.getScreenScale();
                        var minecraft = Minecraft.getInstance();
                        var guiScale = minecraft.options.guiScale();
                        if (guiScale.get() != scale) {
                            guiScale.set(scale);
                            minecraft.resizeGui();
                        }
                    }
                });
            }
        }
    }
}
