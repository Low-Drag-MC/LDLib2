package com.lowdragmc.lowdraglib2.editor.ui;

import com.google.common.util.concurrent.Runnables;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.editor.settings.AppearanceSettings;
import com.lowdragmc.lowdraglib2.editor.settings.BehaviorSettings;
import com.lowdragmc.lowdraglib2.editor.settings.EditorSettings;
import com.lowdragmc.lowdraglib2.editor.ui.menu.FileMenu;
import com.lowdragmc.lowdraglib2.editor.ui.menu.ViewMenu;
import com.lowdragmc.lowdraglib2.editor.ui.view.HistoryView;
import com.lowdragmc.lowdraglib2.editor.ui.view.InspectorView;
import com.lowdragmc.lowdraglib2.editor.ui.view.ResourceView;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Menu;
import com.lowdragmc.lowdraglib2.gui.ui.event.CommandEvents;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.gui.util.TreeNode;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDimension;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.*;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Deque;
import java.util.function.Supplier;

@Getter
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class Editor extends UIElement {
    public final UIElement top;
    public final UIElement icon;
    public final UIElement menuContainer;
    public final UIElement topPlaceholder;
    public final UIElement buttonContainer;

    public final Button closeButton;

    public final FileMenu fileMenu;
    public final ViewMenu viewMenu;

    public final UIElement mainView;

    public static final String ANCHOR_ROOT = "root";
    public static final String ANCHOR_LEFT = "left";
    public static final String ANCHOR_RIGHT = "right";
    public static final String ANCHOR_CENTER = "center";
    public static final String ANCHOR_BOTTOM = "bottom";

    public final SplittableWindow rootWindow;
    public SplittableWindow leftWindow;
    public SplittableWindow rightWindow;
    public SplittableWindow centerWindow;
    public SplittableWindow bottomWindow;

    public final InspectorView inspectorView;
    public final ResourceView resourceView;
    public final HistoryView historyView;

    public final EditorSettings editorSettings;

    // runtime
    @Getter
    @Nullable
    private EditorWindow window;
    @Getter
    @Nullable
    private IProject currentProject;
    @Getter
    @Nullable
    protected File currentProjectFile;

    /**
     * Tracks the fallback location for each view added via {@link #placeView}, so the editor can
     * re-place views during layout restoration.
     */
    protected final Map<View, Supplier<ViewContainer>> viewFallbacks = new LinkedHashMap<>();
    /**
     * Last loaded layout for the current project type. Consulted by {@link #placeView} so that
     * runtime additions after the initial restore also land in their saved slots when possible.
     */
    @Nullable
    protected EditorLayout savedLayout;

    public Editor() {
        getLayout().widthPercent(100);
        getLayout().heightPercent(100);

        addClass("__editor__");

        // top bar
        this.top = new UIElement();
        this.icon = new UIElement();
        this.menuContainer = new UIElement();
        this.topPlaceholder = new UIElement();
        this.buttonContainer = new UIElement();

        this.closeButton = new Button();

        // view
        this.historyView = new HistoryView(this);
        this.inspectorView = new InspectorView(this);
        this.resourceView = new ResourceView(this);

        // view container
        this.mainView = new UIElement();

        // menu
        this.fileMenu = new FileMenu(this);
        this.viewMenu = new ViewMenu(this);

        this.editorSettings = createSettings();

        rootWindow = new SplittableWindow().setImmortal(true);
        rootWindow.setAnchorId(ANCHOR_ROOT);
        var split1 = rootWindow
                .splitStyle(style -> style.percentage(80).minPercentage(5).maxPercentage(95))
                .splitNew(YogaEdge.LEFT);
        rightWindow = split1.getSecond().setImmortal(true);
        rightWindow.setAnchorId(ANCHOR_RIGHT);
        var split2 = split1.getFirst()
                .splitStyle(style -> style.percentage(75).minPercentage(5).maxPercentage(95))
                .splitNew(YogaEdge.TOP);
        bottomWindow = split2.getSecond().setImmortal(true);
        bottomWindow.setAnchorId(ANCHOR_BOTTOM);
        var split3 = split2.getFirst()
                .splitStyle(style -> style.percentage(28).minPercentage(5).maxPercentage(95))
                .splitNew(YogaEdge.LEFT);
        centerWindow = split3.getSecond().setImmortal(true);
        centerWindow.setAnchorId(ANCHOR_CENTER);
        leftWindow = split3.getFirst().setImmortal(true);
        leftWindow.setAnchorId(ANCHOR_LEFT);

        addChildren(
                top.layout(layout -> {
                    layout.paddingAll(1);
                    layout.widthPercent(100);
                    layout.height(15);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.gapAll(2);
                }).style(style -> style.backgroundTexture(Sprites.RECT_SOLID))
                .addChildren(
                        icon.layout(layout -> {
                            layout.width(11);
                            layout.height(11);
                            layout.marginAll(1);
                            layout.marginHorizontal(5);
                        }).style(style -> style.backgroundTexture(new SpriteTexture())),
                        menuContainer.layout(layout -> {
                            layout.heightPercent(100);
                            layout.flexDirection(FlexDirection.ROW);
                            layout.gapAll(2);
                        }).addClass("__editor_top-menu-container__"),
                        topPlaceholder.layout(layout -> layout.flex(1))
                                .addClass("__editor_top-placeholder__"), // placeholder
                        buttonContainer.layout(layout -> {
                            layout.flexDirection(FlexDirection.ROW);
                            layout.alignItems(AlignItems.CENTER);
                            layout.gapAll(2);
                            layout.marginRight(1);
                        }).addChildren(
                                closeButton.noText().addPreIcon(Icons.WINDOW_CLOSE).layout(layout -> layout.height(12))
                                        .addClass("__white_icon__")
                        ).addClass("__editor_top_button-container__")
                ),
                mainView.layout(layout -> {
                    layout.widthPercent(100);
                    layout.flex(1);
                }).addChild(rootWindow)
        );

        closeButton.setOnClick(e -> close());

        top.addClass("__editor_top__").moveInlineAsDefault();
        mainView.addClass("__editor_main__").moveInlineAsDefault();

        /// internal components
        initEditorSettings();
        editorSettings.loadAllSettingsFromFile();
        editorSettings.applyCurrentSettings();

        initMenus();
        onPrepareInspectorView();
        onPrepareHistoryView();
        onPrepareResourceView();

        /// events
        addEventListener(UIEvents.VALIDATE_COMMAND, this::onValidateCommand);
        addEventListener(UIEvents.EXECUTE_COMMAND, this::onExecuteCommand);
    }

    /**
     * Used to create seperated editor.
     */
    protected abstract Editor createNewEditorInstance();

    protected void _setEditorWindowInternal(@Nullable EditorWindow window) {
        this.window = window;
    }

    /**
     * Initialize the menus here.
     */
    protected void initMenus() {
        menuContainer.addChildren(fileMenu.createMenuTab(), viewMenu.createMenuTab());
    }

    protected EditorSettings createSettings() {
        return new EditorSettings(this);
    }

    protected void initEditorSettings() {
        editorSettings.registerSettings(new AppearanceSettings(), AppearanceSettings.CODEC);
        editorSettings.registerSettings(new BehaviorSettings(), BehaviorSettings.CODEC);
    }

    protected void onPrepareInspectorView() {
        placeView(inspectorView, () -> rightWindow.getRightTop());
    }

    protected void onPrepareHistoryView() {
        placeView(historyView, () -> rightWindow.getRightTop());
    }

    protected void onPrepareResourceView() {
        placeView(resourceView, () -> bottomWindow.getLeftBottom());
    }

    /**
     * Place a view, remembering its code-supplied fallback {@link ViewContainer}.
     * If a saved layout is loaded and assigns this view (by name) to a still-existing slot,
     * the view goes there; otherwise it goes to the fallback.
     *
     * @param view     the view to place
     * @param fallback supplier of the default container, evaluated lazily against the current tree
     */
    public void placeView(View view, Supplier<ViewContainer> fallback) {
        viewFallbacks.put(view, fallback);
        ViewContainer target = null;
        if (savedLayout != null) {
            target = locateSavedSlot(view.getName(), true);
        }
        if (target == null) {
            target = fallback.get();
        }
        target.addView(view);
    }

    @Nullable
    protected ViewContainer locateSavedSlot(String viewName) {
        return locateSavedSlot(viewName, false);
    }

    /**
     * Find the {@link ViewContainer} in the current tree at the saved slot path for the given view name.
     * Returns null if the view isn't in the saved layout, or if the saved path no longer resolves
     * (e.g., the slot was pruned).
     */
    @Nullable
    protected ViewContainer locateSavedSlot(String viewName, boolean allowOneStepSplit) {
        if (savedLayout == null) return null;
        var slot = savedLayout.findSlotForView(viewName);
        if (slot == null) return null;
        var window = navigatePath(rootWindow, slot.path());
        if (window == null) {
            return allowOneStepSplit ? createOneStepSavedSlot(slot.path()) : null;
        }
        if (window.getViewContainer() != null) {
            return window.getViewContainer();
        }
        // Path resolved to an interior node — fall back to its top-left leaf.
        return window.getLeftTop();
    }

    @Nullable
    private ViewContainer createOneStepSavedSlot(String path) {
        if (savedLayout == null || path.isEmpty()) return null;
        var parentPath = path.substring(0, path.length() - 1);
        var parentWindow = navigatePath(rootWindow, parentPath);
        if (parentWindow == null || parentWindow.isSplit()) return null;
        var parentConfig = navigateConfig(savedLayout.layoutConfig(), parentPath);
        if (parentConfig == null || parentConfig.first() == null || parentConfig.second() == null) return null;

        var targetSide = path.charAt(path.length() - 1);
        var edge = parentConfig.vertical() ?
                (targetSide == 'f' ? YogaEdge.TOP : YogaEdge.BOTTOM) :
                (targetSide == 'f' ? YogaEdge.LEFT : YogaEdge.RIGHT);
        parentWindow.splitStyle(style -> style.percentage(parentConfig.percentage()));
        var split = parentWindow.splitNew(edge);
        var targetWindow = targetSide == 'f' ? split.getFirst() : split.getSecond();
        return targetWindow.getViewContainer();
    }

    @Nullable
    private static SplittableWindow.LayoutConfig navigateConfig(SplittableWindow.LayoutConfig root, String path) {
        var cur = root;
        for (int i = 0; i < path.length(); i++) {
            if (cur == null) return null;
            char c = path.charAt(i);
            cur = c == 'f' ? cur.first() : cur.second();
        }
        return cur;
    }

    @Nullable
    private static SplittableWindow navigatePath(SplittableWindow root, String path) {
        SplittableWindow cur = root;
        for (int i = 0; i < path.length(); i++) {
            if (cur == null) return null;
            char c = path.charAt(i);
            cur = c == 'f' ? cur.getFirst() : cur.getSecond();
        }
        return cur;
    }

    /**
     * Capture the current editor layout: split tree + per-leaf view contents and selection.
     */
    public EditorLayout captureLayout() {
        var slots = new ArrayList<EditorLayout.SlotEntry>();
        collectSlots(rootWindow, "", slots);
        return new EditorLayout(rootWindow.getLayoutConfig(), slots);
    }

    private static void collectSlots(SplittableWindow window, String path, List<EditorLayout.SlotEntry> out) {
        var container = window.getViewContainer();
        if (container != null) {
            var names = new ArrayList<String>();
            String selected = null;
            for (var v : container.getAllViews()) {
                names.add(v.getName());
                if (container.isViewSelected(v)) {
                    selected = v.getName();
                }
            }
            if (!names.isEmpty()) {
                out.add(new EditorLayout.SlotEntry(path, names, selected));
            }
            return;
        }
        if (window.getFirst() != null) {
            collectSlots(window.getFirst(), path + "f", out);
        }
        if (window.getSecond() != null) {
            collectSlots(window.getSecond(), path + "s", out);
        }
    }

    /**
     * Reshape the current editor tree from a saved layout, relocating known views to their saved
     * slots. Views added through the legacy {@code container.addView(view)} path (without going
     * through {@link #placeView}) are also picked up by walking the live tree, so they survive
     * the rebuild.
     */
    public void applyLayout(EditorLayout layout) {
        this.savedLayout = layout;

        // Collect all views currently in the tree (including ones added via raw addView).
        // untrackedAnchors remembers the nearest named-anchor each untracked view was living under,
        // so we can best-effort restore it if the saved layout doesn't mention that view.
        var liveViews = new ArrayList<View>();
        var liveViewsByName = new LinkedHashMap<String, Deque<View>>();
        var untrackedAnchors = new HashMap<View, String>();
        collectViewsInTree(rootWindow, null, liveViews, liveViewsByName, untrackedAnchors);

        // Rebuild rootWindow's subtree to match the saved shape, rebinding anchor references.
        var anchorRegistry = new HashMap<String, SplittableWindow>();
        anchorRegistry.put(ANCHOR_ROOT, rootWindow);
        anchorRegistry.put(ANCHOR_LEFT, leftWindow);
        anchorRegistry.put(ANCHOR_RIGHT, rightWindow);
        anchorRegistry.put(ANCHOR_CENTER, centerWindow);
        anchorRegistry.put(ANCHOR_BOTTOM, bottomWindow);

        rootWindow.rebuildFromLayoutConfig(layout.layoutConfig(), anchorRegistry);

        // Refresh anchor references from survivors (entries whose id wasn't in the saved layout are dropped).
        leftWindow = resolveVisibleAnchor(anchorRegistry, ANCHOR_LEFT);
        rightWindow = resolveVisibleAnchor(anchorRegistry, ANCHOR_RIGHT);
        centerWindow = resolveVisibleAnchor(anchorRegistry, ANCHOR_CENTER);
        bottomWindow = resolveVisibleAnchor(anchorRegistry, ANCHOR_BOTTOM);

        // Place views into saved slots.
        var placed = new java.util.HashSet<View>();
        for (var slot : layout.slots()) {
            var window = navigatePath(rootWindow, slot.path());
            if (window == null) continue;
            ViewContainer container = window.getViewContainer();
            if (container == null) {
                container = window.getLeftTop();
            }
            View selected = null;
            for (var name : slot.viewNames()) {
                var views = liveViewsByName.get(name);
                var view = views == null ? null : views.pollFirst();
                if (view == null) continue;
                container.addView(view);
                placed.add(view);
                if (name.equals(slot.selectedViewName())) {
                    selected = view;
                }
            }
            if (selected != null) {
                container.selectView(selected);
            }
        }

        // Place any view that wasn't in the saved layout:
        // - via placeView-tracked fallback when available;
        // - otherwise restore into the same named anchor it was living under (best effort).
        for (var view : liveViews) {
            if (placed.contains(view)) continue;
            var fallback = viewFallbacks.get(view);
            ViewContainer target;
            if (fallback != null) {
                target = fallback.get();
            } else {
                var anchorId = untrackedAnchors.get(view);
                var anchor = anchorId == null ? null : anchorRegistry.get(anchorId);
                target = (anchor != null) ? anchor.getLeftTop() : rootWindow.getLeftTop();
            }
            target.addView(view);
        }
        rootWindow.trimEmptySplits();
        refreshAnchorReferences();
    }

    private SplittableWindow resolveVisibleAnchor(Map<String, SplittableWindow> anchorRegistry, String anchorId) {
        var window = anchorRegistry.get(anchorId);
        return window != null ? window : rootWindow;
    }

    private void refreshAnchorReferences() {
        leftWindow = resolveVisibleAnchor(ANCHOR_LEFT, leftWindow);
        rightWindow = resolveVisibleAnchor(ANCHOR_RIGHT, rightWindow);
        centerWindow = resolveVisibleAnchor(ANCHOR_CENTER, centerWindow);
        bottomWindow = resolveVisibleAnchor(ANCHOR_BOTTOM, bottomWindow);
    }

    private SplittableWindow resolveVisibleAnchor(String anchorId, SplittableWindow fallback) {
        var window = findAnchorWindow(rootWindow, anchorId);
        return window != null ? window : fallback;
    }

    @Nullable
    private static SplittableWindow findAnchorWindow(SplittableWindow window, String anchorId) {
        if (anchorId.equals(window.getAnchorId())) {
            return window;
        }
        if (window.getFirst() != null) {
            var found = findAnchorWindow(window.getFirst(), anchorId);
            if (found != null) return found;
        }
        if (window.getSecond() != null) {
            return findAnchorWindow(window.getSecond(), anchorId);
        }
        return null;
    }

    private static void collectViewsInTree(SplittableWindow window, @Nullable SplittableWindow currentAnchor,
                                           List<View> outViews, Map<String, Deque<View>> outViewsByName,
                                           Map<View, String> outAnchors) {
        if (window.getAnchorId() != null) currentAnchor = window;
        var container = window.getViewContainer();
        if (container != null) {
            for (var view : container.getAllViews()) {
                outViews.add(view);
                outViewsByName.computeIfAbsent(view.getName(), ignored -> new ArrayDeque<>()).addLast(view);
                if (currentAnchor != null) outAnchors.put(view, currentAnchor.getAnchorId());
            }
            return;
        }
        if (window.getFirst() != null) collectViewsInTree(window.getFirst(), currentAnchor, outViews, outViewsByName, outAnchors);
        if (window.getSecond() != null) collectViewsInTree(window.getSecond(), currentAnchor, outViews, outViewsByName, outAnchors);
    }

    public Component getTitle() {
        if (currentProject == null) {
            return Component.translatable("editor.empty_editor");
        } else {
            var title = Component.translatable("editor.open_project", Component.translatable(currentProject.getName()));
            if (currentProjectFile != null) {
                title.append(" - ").append(currentProjectFile.getPath());
            }
            return title;
        }
    }

    public List<View> getAllViews() {
        return rootWindow.getAllViews();
    }

    public <T, C> Menu<T, C> openMenu(float posX, float posY, TreeNode<T, C> menuNode, UIElementProvider<T> uiProvider) {
        var menu = new Menu<>(menuNode, uiProvider);
        var mui = getModularUI();
        if (mui == null) {
            menu.layout(layout -> {
                layout.left(posX - getContentX());
                layout.top(posY - getContentY());
            });
            addChildren(menu);
        } else {
            menu.layout(layout -> {
                layout.left(posX - mui.ui.rootElement.getContentX());
                layout.top(posY - mui.ui.rootElement.getContentY());
            });
            mui.ui.rootElement.addChildren(menu);
        }
        return menu;
    }

    public void openMenu(float posX, float posY, @Nullable TreeBuilder.Menu menuBuilder) {
        if (menuBuilder == null || menuBuilder.isEmpty()) return;
        openMenu(posX, posY, menuBuilder.build(), TreeBuilder.Menu::uiProvider)
                .setHoverTextureProvider(TreeBuilder.Menu::hoverTextureProvider)
                .setOnNodeClicked(TreeBuilder.Menu::handle);
    }

    /**
     * Close the entire editor window.
     */
    public void close() {
        if (window != null) {
            window.closeWindow();
        } else {
            exit();
        }
    }

    public void exit() {
        exit(null);
    }

    /**
     * Exit the current editor and run the given runnable after the editor is closed.
     * It doesn't mean the window is closed.
     */
    public void exit(@Nullable Runnable onFinish) {
        askToSaveProject(() -> {
            if (currentProject != null) {
                EditorLayoutStore.save(currentProject.getProjectType().getName(), captureLayout());
            }
            if (window != null) {
                window.removeEditor(this);
            } else {
                if (getModularUI() != null && getModularUI().getScreen() != null) {
                    getModularUI().getScreen().onClose();
                }
            }
            if (onFinish != null) {
                onFinish.run();
            }
        });
    }

    public void openSettingsPanel() {
        var dialog = new Dialog();
        dialog.setAutoClose(false);
        dialog.width(TaffyDimension.length(350));
        dialog.setTitle("editor.settings");
        dialog.addContent(editorSettings.createSettingsPanel());

        var cancelButton = new Button();
        cancelButton.text.textStyle(textStyle -> textStyle.textColor(ColorPattern.GRAY.color));
        cancelButton.setActive(false);

        dialog.addButton(new Button()
                .setOnClick(e -> {
                    if (editorSettings.isDirty()) {
                        editorSettings.applyCurrentSettings();
                        editorSettings.saveAllSettingsToFile();
                    }
                    dialog.close();
                })
                .setText("ldlib.gui.tips.confirm").addClass("__confirm-button__"));
        dialog.addButton(new Button()
                .setOnClick(e -> {
                    editorSettings.restoreSettings();
                    editorSettings.applyCurrentSettings();
                    dialog.close();
                })
                .setText("ldlib.gui.tips.cancel"));
        dialog.addButton(cancelButton
                .setOnClick(e -> {
                    if (editorSettings.isDirty()) {
                        editorSettings.applyCurrentSettings();
                        editorSettings.saveAllSettingsToFile();
                    }
                })
                .setText("ldlib.gui.tips.apply"));
        dialog.addEventListener(UIEvents.TICK, e -> {
            var isDirty = editorSettings.isDirty();
            cancelButton.text.textStyle(textStyle -> textStyle.textColor(isDirty ? ColorPattern.WHITE.color : ColorPattern.GRAY.color));
            cancelButton.setActive(isDirty);
        });

        dialog.show(this.getModularUI());
    }

    /**
     * Check if the current project is dirty if the project file exists.
     * It will compare the current project serialized data with the saved file.
     */
    public boolean isCurrentProjectDirty() {
        if (currentProject == null) {
            return false; // No project loaded
        }
        if (currentProjectFile == null) {
            return true; // Project is dirty if it has not been saved yet
        }
        try {
            return currentProject.getProjectType().isProjectDirty(currentProject, currentProjectFile);
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Ask the user to save the current project if it is dirty.
     * @param onFinish Runnable to run after the dialog is closed, regardless of whether the project was saved or not.
     */
    public void askToSaveProject(@Nullable Runnable onFinish) {
        if (isCurrentProjectDirty()) {
            var dialog = Dialog.showCancelableCheck("ldlib.gui.editor.tips.save_project", "ldlib.gui.editor.tips.ask_to_save", doSave -> {
                if (doSave) {
                    saveProject(onFinish);
                } else {
                    if (onFinish != null) {
                        onFinish.run();
                    }
                }
            }, Runnables.doNothing());
            dialog.titleBar.addChild(new Label()
                    .textStyle(style -> style
                            .textAlignVertical(Vertical.CENTER)
                            .textAlignHorizontal(Horizontal.CENTER)
                            .textWrap(TextWrap.HOVER_ROLL))
                    .setText(Component.literal("-").append(getTitle()))
                    .setOverflowVisible(false)
                    .layout(layout -> layout.flex(1)));
            dialog.show(this.getModularUI());
            if (dialog.buttonContainer.getChildren().getFirst() instanceof Button button) {
                button.setText("ldlib.gui.editor.menu.save");
            }
            return;
        }
        if (onFinish != null) {
            onFinish.run();
        }
    }

    public void saveProject(@Nullable Runnable onFinish) {
        saveProject(onFinish, true);
    }

    /**
     * Save the current project to its file if it exists, or prompt to save as if it does not.
     * @param onFinish Runnable to run after the save operation is complete, regardless of whether it was successful or not.
     */
    public void saveProject(@Nullable Runnable onFinish, boolean showNotification) {
        if (currentProject != null) {
            if (currentProjectFile == null) {
                saveAsProject(onFinish);
            } else {
                try {
                    currentProject.getProjectType().saveProjectToFile(currentProject, currentProjectFile);
                } catch (Exception ignored) {}
                if (showNotification) {
                    if (onFinish != null) {
                        Dialog.showNotification("ldlib.gui.editor.menu.save", "ldlib.gui.compass.save_success", onFinish)
                                .show(this.getModularUI());
                    } else {
                        Dialog.showNotification("ldlib.gui.editor.menu.save_success", 2)
                                .show(this.getModularUI());
                    }
                }
            }
        }
    }

    /**
     * Save the current project as a new file.
     * @param onFinish Runnable to run after the save operation is complete, regardless of whether it was successful or not.
     */
    public void saveAsProject(@Nullable Runnable onFinish) {
        if (currentProject == null) return;
        var projectType = currentProject.getProjectType();
        var projectRoot = LDLib2.getAssetsDir();
        var defaultSaveFile = currentProjectFile == null ? projectType.getDefaultSaveFile(currentProject, projectRoot) : currentProjectFile;
        saveAsProject(defaultSaveFile, onFinish);
    }

    /**
     * Save the current project as a new file.
     * @param defaultSaveFile default file to prefill in the save dialog, can be null
     * @param onFinish Runnable to run after the save operation is complete, regardless of whether it was successful or not.
     */
    public void saveAsProject(@Nullable File defaultSaveFile, @Nullable Runnable onFinish) {
        if (currentProject != null) {
            String suffix = currentProject.getSuffix();
            var projectType = currentProject.getProjectType();
            var projectRoot = LDLib2.getAssetsDir();
            Dialog.showFileDialog("ldlib.gui.editor.tips.save_as",
                    projectType.getRootSavePath(currentProject, projectRoot),
                    false,
                    defaultSaveFile,
                    Dialog.suffixFilter(suffix), file -> {
                        if (file != null && !file.isDirectory()) {
                            if (!file.getName().endsWith(suffix)) {
                                file = new File(file.getParentFile(), file.getName() + suffix);
                            }
                            try {
                                projectType.saveProjectToFile(currentProject, file);
                                currentProjectFile = file;
                            } catch (Exception ignored) {}
                        }
                        if (onFinish != null) {
                            onFinish.run();
                        }
                    }).show(this.getModularUI());
        }
    }

    /**
     * Load a project into the editor.
     */
    public final void loadProject(IProject project, @Nullable File projectFile) {
        if (currentProject != null) {
            if (window != null) {
                Dialog.showCheckBox("Dialog.info","editor.loadProject.info", result -> {
                   if (result) {
                       window.createNewEditor(this::createNewEditorInstance).loadNewProject(project, projectFile);
                   } else {
                       closeCurrentProject(true, () -> loadNewProject(project, projectFile));
                   }
                }).show(window);
            } else {
                closeCurrentProject(true, () -> loadNewProject(project, projectFile));
            }
        } else {
            loadNewProject(project, projectFile);
        }
    }

    protected void loadNewProject(IProject project, @Nullable File projectFile) {
        currentProject = project;
        currentProjectFile = projectFile;
        savedLayout = null;
        // load project resource
        resourceView.loadResources(project.getResources());
        historyView.recordSerializableObject(Component.translatable("editor.open"), currentProject);
        project.onLoad(this);
        // Apply saved per-project-type layout (if any) now that all project-specific views are registered.
        var behaviorSettings = editorSettings.getSettings(BehaviorSettings.ID)
                .filter(BehaviorSettings.class::isInstance)
                .map(BehaviorSettings.class::cast)
                .orElse(null);
        if (behaviorSettings == null || behaviorSettings.isRestoreLayoutOnProjectOpen()) {
            EditorLayoutStore.load(project.getProjectType().getName()).ifPresent(this::applyLayout);
        }
    }


    /**
     * Close the current project and clear the views.
     */
    public final void closeCurrentProject(boolean checkSave, @Nullable Runnable onFinish) {
        if (currentProject != null) {
            if (checkSave) {
                askToSaveProject(() -> {
                    closeCurrentProject();
                    if (onFinish != null) {
                        onFinish.run();
                    }
                });
            } else {
                closeCurrentProject();
                if (onFinish != null) {
                    onFinish.run();
                }
            }
        }
    }

    protected void closeCurrentProject() {
        if (currentProject != null) {
            EditorLayoutStore.save(currentProject.getProjectType().getName(), captureLayout());
            currentProject.onClosed(this);
            currentProject = null;
            currentProjectFile = null;
        }
        inspectorView.clear();
        resourceView.clear();
        historyView.clearHistory();
    }

    protected void onValidateCommand(UIEvent event) {
        if (CommandEvents.SAVE.equals(event.command) && getCurrentProject() != null) {
            event.stopPropagation();
        }
    }

    protected void onExecuteCommand(UIEvent event) {
        if (CommandEvents.SAVE.equals(event.command) && getCurrentProject() != null) {
            if (getCurrentProjectFile() != null) {
                saveProject(null);
            } else {
                saveAsProject(null);
            }
        }
    }

    public static Editor emptyEditor() {
        return new Editor() {
            @Override
            protected Editor createNewEditorInstance() {
                return emptyEditor();
            }
        };
    }
}
