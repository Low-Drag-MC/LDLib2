package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.layout.LayoutProperties;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.FileNode;
import com.lowdragmc.lowdraglib2.gui.util.WindowDragHelper;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import dev.vfyjxf.taffy.style.*;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.style.StyleSizeLength;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

@KJSBindings
public class Dialog extends UIElement {
    public final UIElement overlay;
    public final UIElement titleBar;
    public final UIElement contentContainer;
    public final UIElement buttonContainer;
    private boolean autoClose = true;
    private boolean clickOutsideClose = false;
    @Nullable
    @Setter @Accessors(chain = true)
    private Runnable onClose;
    private boolean windowMode = false;
    private boolean isResizing;

    public Dialog() {
        this.titleBar = new UIElement().addClass("__dialog_title__");
        this.contentContainer = new UIElement().addClass("__dialog_content-container__");
        this.buttonContainer = new UIElement().addClass("__dialog_button-container__");
        this.setFocusable(true);
        this.getLayout().positionType(TaffyPosition.ABSOLUTE);
        this.getLayout().widthPercent(100);
        this.getLayout().heightPercent(100);
        this.getLayout().justifyContent(AlignContent.CENTER);
        this.getLayout().alignItems(AlignItems.CENTER);
        this.getStyle().zIndex(1);

        this.overlay = new UIElement().layout(layout -> {
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
            layout.width(150);
        }).addClass("__dialog_overlay__");

        this.titleBar.layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
            layout.gapAll(2);
            layout.setPipelineState(StyleOrigin.DEFAULT);
            layout.widthPercent(100);
            layout.alignItems(AlignItems.CENTER);
            layout.paddingAll(5);
            layout.setPipelineState(StyleOrigin.INLINE);
        }).style(style -> style.backgroundTexture(Sprites.BORDER1_RT1));

        this.contentContainer.layout(layout -> {
            layout.widthPercent(100);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
            layout.paddingAll(4);
            layout.gapAll(2);
        }).style(style -> style.backgroundTexture(Sprites.RECT_SOLID));

        this.buttonContainer.layout(layout -> {
            layout.widthPercent(100);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
            layout.flexDirection(FlexDirection.ROW);
            layout.paddingAll(4);
            layout.gapAll(2);
        }).style(style -> style.backgroundTexture(Sprites.RECT_SOLID));

        overlay.addChildren(titleBar, contentContainer, buttonContainer);

        addChild(overlay);

        stopInteractionEventsPropagation();
        addEventListener(UIEvents.BLUR, this::onBlur, true);
        addEventListener(UIEvents.KEY_DOWN, this::keyDown);
        addEventListener(UIEvents.MOUSE_DOWN, this::mouseDown);

        internalSetup();
    }

    @Override
    public String name() {
        return "dialog";
    }

    public Dialog allowInteraction() {
        getLayout().widthAuto();
        getLayout().heightAuto();
        getLayout().alignSelf(AlignItems.CENTER);

        this.addEventListener(UIEvents.MOUSE_DOWN, UIEvent::stopLaterPropagation);
        this.addEventListener(UIEvents.MOUSE_UP, UIEvent::stopLaterPropagation);
        this.addEventListener(UIEvents.CLICK, UIEvent::stopLaterPropagation);
        this.addEventListener(UIEvents.DOUBLE_CLICK, UIEvent::stopLaterPropagation);
        this.addEventListener(UIEvents.MOUSE_MOVE, UIEvent::stopLaterPropagation);
        this.addEventListener(UIEvents.MOUSE_WHEEL, UIEvent::stopLaterPropagation);
        this.addEventListener(UIEvents.DRAG_UPDATE, UIEvent::stopLaterPropagation);
        this.addEventListener(UIEvents.DRAG_PERFORM, UIEvent::stopLaterPropagation);
        return this;
    }

    protected void keyDown(UIEvent event) {
        if (autoClose && event.keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            event.stopPropagation();
        }
    }

    protected void mouseDown(UIEvent event) {
        if (clickOutsideClose && autoClose && !overlay.isSelfOrChildHover()) {
            if (isInsideDialog()) {
                return;
            }
            close();
            event.stopPropagation();
        }
    }

    protected void onBlur(UIEvent event) {
        if (event.relatedTarget != null && this.isAncestorOf(event.relatedTarget)) { // focus on children
            return;
        }
        if (isInsideDialog()) { // focus on sibling popup/menu
            return;
        }

        if (event.target == this) { // lose focus
            if (event.relatedTarget != null && !this.isAncestorOf(event.relatedTarget)) {
                if (autoClose) {
                    close();
                }
                return;
            }
            if (isSelfOrChildHover()) {
                focus();
            } else {
                if (autoClose) {
                    close();
                }
            }
        } else { // child lose focus
            if (event.relatedTarget == null && isSelfOrChildHover()) {
                focus();
            } else {
                if (autoClose) {
                    close();
                }
            }
        }
    }

    private boolean isInsideDialog() {
        var mui = getModularUI();
        if (mui == null) return false;
        var localMouse = overlay.worldToLocal(new Vector2f(mui.getLastMouseX(), mui.getLastMouseY()));
        return overlay.isIntersectWithPoint(localMouse.x, localMouse.y);
    }

    /**
     * Closes the dialog and removes it from its parent if it has one.
     */
    public void close(){
        if (this.getParent() != null) {
            this.getParent().removeChild(this);
            if (onClose != null) {
                onClose.run();
            }
        }
    }

    /**
     * Sets whether the dialog should close automatically when the escape key is pressed.
     *
     * @param autoClose true to enable auto-close, false to disable
     * @return this dialog instance for method chaining
     */
    public Dialog setAutoClose(boolean autoClose) {
        this.autoClose = autoClose;
        return this;
    }

    /**
     * Sets whether the dialog should close automatically when the mouse is clicked outside of the dialog.
     *
     * @param clickOutsideClose true to enable auto-close, false to disable
     * @return this dialog instance for method chaining
     */
    public Dialog setClickOutsideClose(boolean clickOutsideClose) {
        this.clickOutsideClose = clickOutsideClose;
        return this;
    }

    /**
     * Shows the dialog as a child of the specified UIElement parent.
     * This will add the dialog to the parent's children and focus it.
     * NOTE: ypu should always call this method to show the dialog after creating it,
     *
     * @param parent the UIElement that will be the parent of this dialog
     */
    public Dialog show(UIElement parent) {
        buttonContainer.setDisplay(!buttonContainer.getChildren().isEmpty());
        contentContainer.setDisplay(!contentContainer.getChildren().isEmpty());
        titleBar.setDisplay(!titleBar.getChildren().isEmpty());

        parent.addChild(this);
        focus();
        return this;
    }

    /**
     * Displays the dialog on the top of the specified {@link ModularUI} instance.
     *
     * If the provided {@code modularUI} is null, this method will return the current
     * dialog instance without performing any action.
     *
     * @param modularUI the {@code ModularUI} instance used to display the dialog;
     *                  it may be null, in which case no action is performed
     * @return the current {@code Dialog} instance for method chaining
     */
    public Dialog show(@Nullable ModularUI modularUI) {
        if (modularUI == null) return this;
        return show(modularUI.ui.rootElement);
    }

    /**
     * Sets the width of the dialog. by default, it will be 150px.
     */
    @Deprecated
    public Dialog width(StyleSizeLength width) {
        overlay.layout(layout -> layout.width(width));
        return this;
    }

    public Dialog width(TaffyDimension width) {
        overlay.layout(layout -> layout.setWidth(width));
        return this;
    }

    public Dialog windowMode(float worldX, float worldY) {
        return windowMode(worldX, worldY, 200, 150);
    }

    public Dialog windowMode(float worldX, float worldY, float width, float height) {
        windowMode = true;
        setClickOutsideClose(true);
        this.getLayout().justifyContent(AlignContent.FLEX_START);
        this.getLayout().alignItems(AlignItems.STRETCH);
        overlay.getLayout().width(width).height(height);
        contentContainer.getLayout().flex(1);
        // move and resize behaviour
        WindowDragHelper.setDragMove(titleBar, overlay, null, null);
        WindowDragHelper.setBorderResize(overlay, overlay, 2,
                new Vector2f(50),
                new Vector2f(Float.MAX_VALUE),
                e -> windowMode, (e, handle) -> {
                    isResizing = true;
                    return true;
                }, e -> isResizing = false);
        addEventListener(UIEvents.LAYOUT_CHANGED, e -> {
            var parent = getParent();
            if (parent != null) {
                var local = parent.worldToLocalLayoutOffset(new Vector2f(worldX, worldY));
                overlay.getLayout().left(local.x).top(local.y);
                e.currentElement.addEventListener(UIEvents.LAYOUT_CHANGED, e2 -> {
                    overlay.adaptPositionToScreen();
                });
            }
            e.currentElement.removeEventListener(UIEvents.LAYOUT_CHANGED, e.currentListener);
        });
        return this;
    }

    /**
     * Draw a dark background behind the dialog.
     */
    public Dialog darkenBackground() {
        this.style(style -> style.backgroundTexture(ColorPattern.T_BLACK.rectTexture()));
        return this;
    }

    public Dialog top() {
        this.getLayout().justifyContent(AlignContent.FLEX_START);
        this.overlay.layout(layout -> {
            layout.top(10);
        });
        return this;
    }

    public Dialog bottom() {
        this.getLayout().justifyContent(AlignContent.FLEX_END);
        this.overlay.layout(layout -> {
            layout.bottom(10);
        });
        return this;
    }

    /**
     * Sets the title of the dialog.
     */
    public Dialog setTitle(String title) {
        titleBar.clearAllChildren();
        titleBar.addChild(new Label()
                .textStyle(style -> style
                        .textAlignVertical(Vertical.CENTER)
                        .textAlignHorizontal(Horizontal.CENTER)
                        .adaptiveWidth(true))
                .setText(title));
        return this;
    }

    /**
     * Adds a content element to the dialog.
     * The content will be added to the middle of the dialog.
     */
    public Dialog addContent(UIElement content) {
        contentContainer.addChild(content);
        return this;
    }

    /**
     * Adds a button to the dialog.
     * The button will be added to the button container at the bottom of the dialog.
     */
    public Dialog addButton(UIElement button) {
        buttonContainer.addChild(button);
        return this;
    }

    /**
     * Creates a dialog for editing a string value.
     * This dialog will have a text field for input and two buttons: confirm and cancel.
     * The confirm button will call the provided result consumer with the input text.
     * Don't forget to call {@link Dialog#show(UIElement)} to display the dialog.
     *
     * @param title the title of the dialog
     * @param initial the initial text to display in the text field
     * @param predicate an optional predicate to validate the input text
     * @param result a consumer that will receive the input text when the confirm button is clicked
     */
    public static Dialog stringEditorDialog(String title, String initial, @Nullable Predicate<String> predicate, Consumer<String> result) {
        var textField = new TextField().setText(initial, false);
        if (predicate != null) {
            textField.setTextValidator(predicate);
        }
        var dialog = new Dialog();
        dialog.setTitle(title);
        dialog.addContent(textField.layout(layout -> layout.widthPercent(100)));
        dialog.addButton(new Button()
                .setOnClick(e -> {
                    result.accept(textField.getText());
                    dialog.close();
                })
                .setText("ldlib.gui.tips.confirm")
                .addClass("__confirm-button__"));
        dialog.addButton(new Button()
                .setOnClick(e -> dialog.close())
                .setText("ldlib.gui.tips.cancel")
                .addClass("__cancel-button__"));
        return dialog;
    }

    /**
     * Displays a notification dialog with a message and a progress bar that fills over a specified duration.
     * The dialog will automatically close when the progress bar completes.
     *
     * @param info the information text or message to display in the notification dialog
     * @param duration the duration (in seconds) for which the progress bar will fill before the dialog closes
     * @return the {@code Dialog} instance representing the notification
     */
    public static Dialog showNotification(String info, float duration) {
        var dialog = new Dialog();
        dialog.titleBar.setDisplay(false);
        dialog.addContent(new Label().textStyle(textStyle -> textStyle.textWrap(TextWrap.WRAP).adaptiveHeight(true))
                .setText(info).layout(layout -> layout.widthPercent(100)));
        dialog.top();
        dialog.allowInteraction();
        dialog.setAutoClose(false);

        // add progress bar
        dialog.overlay.addChildAt(new UIElement()
                .layout(layout -> layout.height(2).widthPercent(100))
                .addClass("__dialog_progress-bg__")
                .addChild(
                        new UIElement().layout(layout -> layout.heightPercent(100).widthPercent(0))
                                .style(style -> Style.defaultPipeline(style,
                                        s-> s.backgroundTexture(ColorPattern.WHITE.rectTexture())))
                                .addClass("__dialog_progress-bar__")
                                .animation(animation -> animation
                                        .duration(duration)
                                        .style(LayoutProperties.WIDTH, TaffyDimension.percent(1))
                                        .onFinished(target -> dialog.close())
                                        .start())
                ), 0
        );
        return dialog;
    }

    /**
     * Shows a notification dialog with a title and information text.
     * This dialog will have a single button to close it.
     * Don't forget to call {@link Dialog#show(UIElement)} to display the dialog.
     * @param title the title of the dialog
     * @param info the information text to display in the dialog
     * @param onClosed an optional runnable that will be called when the dialog is closed
     */
    public static Dialog showNotification(String title, String info, @Nullable Runnable onClosed) {
        var dialog = new Dialog();
        dialog.setOnClose(onClosed);
        dialog.setTitle(title);
        dialog.addContent(new Label().textStyle(textStyle -> textStyle.textWrap(TextWrap.WRAP).adaptiveHeight(true))
                .setText(info).layout(layout -> layout.widthPercent(100)));
        dialog.addButton(new Button().setOnClick(e -> dialog.close()).setText("ldlib.gui.tips.confirm").addClass("__confirm-button__"));
        return dialog;
    }

    /**
     * Shows a dialog with a title and information text, along with two buttons: confirm and cancel.
     * This dialog will call the provided BooleanConsumer with true if confirm is clicked, or false if cancel is clicked.
     * Don't forget to call {@link Dialog#show(UIElement)} to display the dialog.
     * @param title the title of the dialog
     * @param info the information text to display in the dialog
     * @param onClosed a BooleanConsumer that will be called with true if confirm is clicked, or false if cancel is clicked
     */
    public static Dialog showCheckBox(String title, String info, BooleanConsumer onClosed) {
        var dialog = new Dialog();
        dialog.setTitle(title);
        dialog.addContent(new Label().textStyle(textStyle -> textStyle.textWrap(TextWrap.WRAP).adaptiveHeight(true))
                .setText(info).layout(layout -> layout.widthPercent(100)));
        dialog.addButton(new Button()
                .setOnClick(e -> {
                    if (onClosed != null) {
                        onClosed.accept(true);
                    }
                    dialog.close();
                })
                .setText("ldlib.gui.tips.confirm").addClass("__confirm-button__"));
        dialog.addButton(new Button()
                .setOnClick(e -> {
                    if (onClosed != null) {
                        onClosed.accept(false);
                    }
                    dialog.close();
                })
                .setText("ldlib.gui.tips.reject")
                .addClass("__reject-button__"));
        return dialog;
    }

    public static Dialog showCancelableCheck(String title, String info, BooleanConsumer onClosed, Runnable onCanceled) {
        var dialog = showCheckBox(title, info, onClosed);
        dialog.addButton(new Button()
                .setOnClick(e -> {
                    if (onCanceled != null) {
                        onCanceled.run();
                    }
                    dialog.close();
                })
                .setText("ldlib.gui.tips.cancel")
                .addClass("__cancel-button__"));
        return dialog;
    }

    /**
     * Shows a file dialog for selecting or creating files.
     * This dialog will display a tree list of files and directories starting from the specified directory.
     * You can use the text field to filter or specify the file name.
     * The dialog will have a confirm button to select the file or directory, and a cancel button to close the dialog.
     * You can also provide a predicate to validate the selected file or directory.
     * Don't forget to call {@link Dialog#show(UIElement)} to display the dialog.
     * @param title the title of the dialog
     * @param dir the directory to start from, it will be created if it does not exist
     * @param isSelector if true, the dialog will allow selecting a file or directory, otherwise it will allow creating a new file in the selected directory
     * @param valid a predicate to validate the selected file or directory, can be null to allow all files
     * @param result a consumer that will receive the selected file or directory when the confirm button is clicked
     */
    public static Dialog showFileDialog(String title, File dir, boolean isSelector, @Nullable Predicate<FileNode> valid, Consumer<File> result) {
        return showFileDialog(title, dir, isSelector, null, valid, result);
    }

    /**
     * Shows a file dialog for selecting or creating files.
     * This dialog will display a tree list of files and directories starting from the specified directory.
     * You can use the text field to filter or specify the file name.
     * The dialog will have a confirm button to select the file or directory, and a cancel button to close the dialog.
     * You can also provide a predicate to validate the selected file or directory.
     * Don't forget to call {@link Dialog#show(UIElement)} to display the dialog.
     * @param title the title of the dialog
     * @param dir the directory to start from, it will be created if it does not exist
     * @param isSelector if true, the dialog will allow selecting a file or directory, otherwise it will allow creating a new file in the selected directory
     * @param defaultValue the default file or directory to select or prefill, can be null
     * @param valid a predicate to validate the selected file or directory, can be null to allow all files
     * @param result a consumer that will receive the selected file or directory when the confirm button is clicked
     */
    public static Dialog showFileDialog(String title, File dir, boolean isSelector, @Nullable File defaultValue, @Nullable Predicate<FileNode> valid, Consumer<File> result) {
        var dialog = new Dialog();
        var textField = new TextField();
        var treeList = new TreeList<FileNode>();
        if (!dir.isDirectory()) {
            if (!dir.mkdirs()) {
                return dialog;
            }
        }
        var root = new FileNode(dir).setValid(valid);
        dialog.overlay.layout(layout -> layout.width(200));
        dialog.setTitle(title);
        dialog.addContent(new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.gapAll(2);
        }).addChildren(textField.layout(layout -> layout.flex(1)), new Button().setOnClick(e -> {
            Util.getPlatform().openFile(dir.isDirectory() ? dir : dir.getParentFile());
        }).noText().layout(layout -> {
            layout.width(14);
            layout.height(14);
            layout.paddingAll(3);
        }).addChild(new UIElement().addClass("__white_icon__").layout(layout -> layout.widthPercent(100)).style(style -> style.backgroundTexture(Icons.FOLDER)))));
        treeList.setOnSelectedChanged(selected -> {
            if (selected.isEmpty()) return;
            var first = selected.stream().findFirst().get();
            if (isSelector) {
                textField.setText(first.getKey().toString(), false);
            } else if (first.getKey().isFile()) {
                textField.setText(first.getKey().getName(), false);
            } else {
                textField.setText("", false);
            }
        }).setOnDoubleClickNode(node -> {
            var file = node.getKey();
            if (isSelector && file.isFile()) {
                dialog.close();
                if (result != null) result.accept(file);
            }
        }).setNodeUISupplier(TreeList.iconTextTemplate(
                node -> node.getKey().isDirectory() ?
                        Icons.FOLDER :
                        Icons.getIcon(node.getKey().getName()
                                .substring(node.getKey().getName().lastIndexOf('.') + 1)),
                node -> Component.translatable(node.getKey().getName())))
                .setRoot(root);
        applyFileDialogDefault(treeList, textField, root, isSelector, defaultValue);
        var scrollerView = new ScrollerView().addScrollViewChild(treeList).layout(layout -> {
            layout.widthPercent(100);
            layout.height(180);
        });
        dialog.addContent(scrollerView);
        dialog.addButton(new Button()
                .setOnClick(e -> {
                    var parent = dialog.getParent();
                    dialog.close();
                    if (result == null) return;
                    if (isSelector) {
                        if (textField.getText().isEmpty()) {
                            return;
                        }
                        var file = new File(textField.getText());
                        if (file.isDirectory() || file.exists()) {
                            result.accept(file);
                        } else if (parent != null){
                            Dialog.showNotification("editor.error", "editor.file_not_found", null).show(parent);
                        }
                    } else {
                        var nodes = treeList.getSelected();
                        if (!nodes.isEmpty()) {
                            var first = nodes.stream().findFirst().get();
                            var file = first.getKey();
                            var fileName = textField.getText();
                            if (file.isFile()) {
                                file = file.getParentFile();
                            }
                            if (file.isDirectory()) {
                                result.accept(new File(file, fileName));
                            }
                        }
                    }
                })
                .setText("ldlib.gui.tips.confirm")
                .addClass("__confirm-button__"));
        dialog.addButton(new Button()
                .setOnClick(e -> dialog.close())
                .setText("ldlib.gui.tips.cancel")
                .addClass("__cancel-button__"));
        return dialog;
    }

    static void applyFileDialogDefault(TreeList<FileNode> treeList, TextField textField, FileNode root, boolean isSelector, @Nullable File defaultValue) {
        var fileDialogDefault = FileDialogDefaults.resolve(root, isSelector, defaultValue);
        var selectedNode = fileDialogDefault.selectedNode();
        if (selectedNode != null) {
            treeList.expandNodeAlongPath(selectedNode);
            selectedNode = findDisplayedFileNode(treeList, selectedNode);
            treeList.setSelected(List.of(selectedNode), false);
        }
        if (defaultValue != null) {
            textField.setText(fileDialogDefault.text(), false);
        }
    }

    private static FileNode findDisplayedFileNode(TreeList<FileNode> treeList, FileNode target) {
        for (var node : treeList.getNodeUIs().keySet()) {
            if (node.getDimension() == target.getDimension() && FileDialogDefaults.normalizeFile(node.getKey()).equals(FileDialogDefaults.normalizeFile(target.getKey()))) {
                return node;
            }
        }
        return target;
    }

    /**
     * Creates a predicate that filters out nodes based on their suffixes.
     * @param suffixes the suffixes to filter out, e.g. ".txt", ".jpg"
     */
    public static Predicate<FileNode> suffixFilter(String... suffixes) {
        return node -> {
            for (String suffix : suffixes) {
                if (!node.getKey().isFile() || node.getKey().getName().toLowerCase().endsWith(suffix.toLowerCase())) {
                    return true;
                }
            }
            return false;
        };
    }

    @Override
    public void drawBackgroundAdditional(@NotNull GUIContext guiContext) {
        super.drawBackgroundAdditional(guiContext);
        if (windowMode && !isResizing) {
            WindowDragHelper.drawResizeIcon(guiContext, overlay, 2);
        }
    }
}
