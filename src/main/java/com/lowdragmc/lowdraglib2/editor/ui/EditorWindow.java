package com.lowdragmc.lowdraglib2.editor.ui;

import com.google.common.collect.Maps;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.ArrayUtils;
import org.appliedenergistics.yoga.*;
import org.joml.Vector2f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class EditorWindow extends UIElement {
    public static final ResourceLocation DEFAULT_ID = LDLib2.id("default");
    private record DragBorder(YogaEdge[] edges, float left, float top, float width, float height) { }
    private static final YogaEdge[] EDGES = new YogaEdge[]{YogaEdge.LEFT, YogaEdge.RIGHT, YogaEdge.TOP, YogaEdge.BOTTOM};
    private static final Map<ResourceLocation, EditorWindow> MINIMIZED_WINDOWS = Maps.newConcurrentMap();

    public final UIElement window = new UIElement();
    public final UIElement editorButtonContainer = new UIElement();
    public final UIElement editorContainer = new UIElement();
    @Nullable
    public final ResourceLocation windowID;

    // runtime
    private int initialScreenScale;
    @Getter
    private boolean maximized = true;
    private float windowWidth = 300;
    private float windowHeight = 200;
    private float windowLeft = -150;
    private float windowTop = -100;
    @Getter
    @Nullable
    private Editor currentEditor;
    @Getter
    private final LinkedHashMap<Editor, UIElement> editors = new LinkedHashMap<>();

    public static EditorWindow openDefault(Supplier<Editor> editorCreator) {
        return open(DEFAULT_ID, editorCreator);
    }

    public static EditorWindow open(ResourceLocation windowID, Supplier<Editor> editorCreator) {
        var editorWindow = MINIMIZED_WINDOWS.remove(windowID);
        if (editorWindow != null && LDLib2.isClient()) {
            Minecraft.getInstance().getToasts().addToast(new SystemToast(
                    new SystemToast.SystemToastId(1000L),
                    Component.translatable("editor.minimized.title"),
                    Component.translatable("editor.minimized.tips")
            ));
            return editorWindow;
        }
        return new EditorWindow(windowID, editorCreator);
    }

    public EditorWindow(Supplier<Editor> editorCreator) {
        this(null, editorCreator);
    }

    public EditorWindow(@Nullable ResourceLocation windowID, Supplier<Editor> editorCreator) {
        this.windowID = windowID;

        if (LDLib2.isClient()) {
            var minecraft = Minecraft.getInstance();
            initialScreenScale = minecraft.options.guiScale().get();
        }

        getLayout().setWidthPercent(100);
        getLayout().setHeightPercent(100);

        this.editorButtonContainer.layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.setPosition(YogaEdge.TOP, 15);
            layout.setWidthPercent(100);
            layout.setGap(YogaGutter.ALL, 1);
            layout.setHeight(14);
        }).setDisplay(false).style(style -> style.backgroundTexture(ColorPattern.BLACK.rectTexture()));
        this.editorButtonContainer.addClass("__editor-window_editor-button-container__").moveInlineAsDefault();

        this.editorContainer.getLayout().widthPercent(100).flex(1);
        this.editorContainer.addClass("__editor-window_editor-container__").moveInlineAsDefault();
        this.window.layout(layout -> layout.widthPercent(100).heightPercent(100))
                .addChildren(this.editorContainer, this.editorButtonContainer);
        this.window.addEventListener(UIEvents.MOUSE_DOWN, this::onWindowMouseDown);
        this.window.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onWindowDrag);

        addChild(window);
        createNewEditor(editorCreator);
    }

    @Override
    protected void onRemoved() {
        if (windowID != null && MINIMIZED_WINDOWS.containsKey(windowID)) return;
        super.onRemoved();
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
        editor.mainView.layout(layout -> {
            layout.setMargin(YogaEdge.TOP, hasMultipleEditors() ? 14 : 0);
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
                .addPreIcon(DynamicTexture.of(() -> isMaximized() ? Icons.WINDOW_RESTORE : Icons.WINDOW_MAXIMIZE))
                .setOnClick(e -> {
                    if (isMaximized()) {
                        retoreWindow();
                    } else {
                        maximizeWindow();
                    }
                })
                .layout(layout -> layout.height(12)), 0);
        if (windowID != null) {
            newEditor.buttonContainer.addChildAt(new Button().noText()
                    .addPreIcon(Icons.WINDOW_MINIMIZE)
                    .setOnClick(e -> minimizeWindow())
                    .layout(layout -> layout.height(12)), 0);
        }
        newEditor.topPlaceholder.addEventListener(UIEvents.DOUBLE_CLICK, e -> {
            if (newEditor.getWindow() != this) return;
            if (isMaximized()) {
                retoreWindow();
            } else {
                maximizeWindow();
            }
        });
        newEditor.topPlaceholder.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (newEditor.getWindow() == this && !isMaximized()) {
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

            if (LDLib2.isClient()) {
                var minecraft = Minecraft.getInstance();
                var guiScale = minecraft.options.guiScale();
                if (guiScale.get() != initialScreenScale) {
                    guiScale.set(initialScreenScale);
                    minecraft.resizeDisplay();
                }
            }

            if (getModularUI() != null && getModularUI().getScreen() != null) {
                getModularUI().getScreen().onClose();
            }
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

    public void minimizeWindow() {
        if (EditorWindow.MINIMIZED_WINDOWS.containsKey(windowID)) return;
        EditorWindow.MINIMIZED_WINDOWS.put(windowID, this);
        if (getModularUI() != null && getModularUI().getScreen() != null) {
            getModularUI().getScreen().onClose();
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
        if (mui != null && mui.getScreen() != null) {
            mui.getScreen().init(minecraft, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
        }
    }

    public void retoreWindow() {
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
        if (mui != null && mui.getScreen() != null) {
            mui.getScreen().init(minecraft, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
        }
    }

    protected UIElement createEditorButton(Editor editor) {
        return new UIElement().layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
            layout.setHeightPercent(100);
            layout.alignItems(AlignItems.CENTER);
            layout.setFlex(1);
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
                            layout.setHeightPercent(100);
                            layout.setFlex(1);
                        }).addEventListener(UIEvents.TICK, e -> {
                            if (e.target.getModularUI().getTickCounter() % 20 ==0) {
                                var currentTitle = editor.getTitle();
                                if (e.target instanceof TextElement text && !text.getText().equals(currentTitle)) {
                                    text.setText(currentTitle);
                                }
                            }
                        }).setOverflow(YogaOverflow.HIDDEN),
                new Button().noText().buttonStyle(style -> {
                    style.baseTexture(Icons.REMOVE);
                    style.hoverTexture(Icons.REMOVE.copy().setColor(ColorPattern.GRAY.color));
                    style.pressedTexture(Icons.REMOVE);
                }).setOnClick(e -> {
                    showEditor(editor);
                    editor.exit();
                    e.stopPropagation();
                }).layout(layout -> {
                    layout.setHeight(9);
                    layout.setAspectRatio(1);
                    layout.setMargin(YogaEdge.RIGHT, 2);
                })
        ).addEventListener(UIEvents.MOUSE_DOWN, e -> showEditor(editor));
    }

    protected boolean isMouseOverWindowBorder(YogaEdge edge, float mx, float my) {
        if (window.isMouseOver(mx, my)) {
            var border = 4;
            var w = window.getSizeWidth();
            var h = window.getSizeHeight();
            var x = window.getPositionX();
            var y = window.getPositionY();

            return switch (edge) {
                case LEFT -> mx <= x + border;
                case RIGHT -> mx >= x + w - border;
                case TOP -> my <= y + border;
                case BOTTOM -> my >= y + h - border;
                default -> false;
            };
        }
        return false;
    }

    protected void onWindowMouseDown(UIEvent event) {
        if (!isMaximized()) {
            var edges = Arrays.stream(EDGES).filter(edge  -> isMouseOverWindowBorder(edge, event.x, event.y))
                    .toArray(YogaEdge[]::new);
            if (edges.length == 0) return;
            var icon = Icons.ARROW_LEFT_RIGHT;
            if (ArrayUtils.contains(edges, YogaEdge.TOP) || ArrayUtils.contains(edges, YogaEdge.BOTTOM)) {
                icon = Icons.ARROW_UP_DOWN;
            }
            var width = icon.spriteSize.width;
            var height = icon.spriteSize.height;
            this.window.startDrag(new DragBorder(edges, windowLeft, windowTop,
                            windowWidth, windowHeight), icon)
                    .setDragTexture(- width / 2f, -height / 2f, width, height);
            event.stopPropagation();
        }
    }

    protected void onWindowDrag(UIEvent event) {
        if (!isMaximized() && event.dragHandler.getDraggingObject() instanceof DragBorder(
                YogaEdge[] edges, float left, float top, float width, float height
        )) {
            var mx = event.x - event.dragStartX;
            var my = event.y - event.dragStartY;
            for (YogaEdge edge : edges) {
                switch (edge) {
                    case LEFT -> {
                        if (width - mx < 200) return;
                        windowLeft = left + mx;
                        windowWidth = width - mx;
                    }
                    case RIGHT -> {
                        if (width + mx < 200) return;
                        windowWidth = width + mx;
                    }
                    case TOP -> {
                        if (height - my < 150) return;
                        windowTop = top + my;
                        windowHeight = height - my;
                    }
                    case BOTTOM -> {
                        if (height + my < 150) return;
                        windowHeight = height  + my;
                    }
                }
            }
            window.layout(layout -> layout.left(windowLeft).top(windowTop).width(windowWidth).height(windowHeight));
        }
    }

    @Override
    public void drawBackgroundAdditional(@Nonnull GUIContext guiContext) {
        super.drawBackgroundAdditional(guiContext);
        if (!isMaximized()) {
            for (var edge : EDGES) {
                if (isMouseOverWindowBorder(edge, guiContext.localMouseX, guiContext.localMouseY)) {
                    guiContext.postRendering(ctx -> {
                        var icon = (edge == YogaEdge.TOP || edge == YogaEdge.BOTTOM) ? Icons.ARROW_UP_DOWN : Icons.ARROW_LEFT_RIGHT;
                        var width = icon.spriteSize.width;
                        var height = icon.spriteSize.height;
                        ctx.drawTexture(icon,
                                ctx.localMouseX - width / 2f,
                                ctx.localMouseY - height / 2f,
                                width,
                                height);
                    });
                    return;
                }
            }
        }
    }
}
