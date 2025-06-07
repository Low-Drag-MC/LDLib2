package com.lowdragmc.lowdraglib.gui.ui;

import com.lowdragmc.lowdraglib.editor_outdated.Icons;
import com.lowdragmc.lowdraglib.gui.ColorPattern;
import com.lowdragmc.lowdraglib.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib.gui.ui.elements.*;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib.gui.ui.style.value.TextWrap;
import com.lowdragmc.lowdraglib.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib.gui.util.FileNode;
import com.lowdragmc.lowdraglib.gui.util.TreeNode;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.Util;
import org.appliedenergistics.yoga.*;
import org.appliedenergistics.yoga.style.StyleSizeLength;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.io.File;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Dialog extends UIElement {
    public final UIElement overlay;
    public final UIElement titleBar;
    public final UIElement contentContainer;
    public final UIElement buttonContainer;
    private boolean autoClose = true;
    @Nullable
    @Setter @Accessors(chain = true)
    private Runnable onClose;

    public Dialog() {
        this.titleBar = new UIElement();
        this.contentContainer = new UIElement();
        this.buttonContainer = new UIElement();
        this.setFocusable(true);
        this.getLayout().setPositionType(YogaPositionType.ABSOLUTE);
        this.getLayout().setWidthPercent(100);
        this.getLayout().setHeightPercent(100);
        this.getLayout().setJustifyContent(YogaJustify.CENTER);
        this.getLayout().setAlignItems(YogaAlign.CENTER);
        this.getStyle().zIndex(1);

        this.overlay = new UIElement().layout(layout -> {
            layout.setAlignItems(YogaAlign.CENTER);
            layout.setJustifyContent(YogaJustify.CENTER);
            layout.setWidth(150);
        });

        this.titleBar.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setAlignItems(YogaAlign.CENTER);
            layout.setPadding(YogaEdge.ALL, 5);
        }).style(style -> style.backgroundTexture(Sprites.BORDER1_RT1));

        this.contentContainer.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setAlignItems(YogaAlign.CENTER);
            layout.setJustifyContent(YogaJustify.CENTER);
            layout.setPadding(YogaEdge.ALL, 4);
            layout.setGap(YogaGutter.ALL, 2);
        }).style(style -> style.backgroundTexture(Sprites.RECT_SOLID));

        this.buttonContainer.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setAlignItems(YogaAlign.CENTER);
            layout.setJustifyContent(YogaJustify.CENTER);
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setPadding(YogaEdge.ALL, 4);
            layout.setGap(YogaGutter.ALL, 2);
        }).style(style -> style.backgroundTexture(Sprites.RECT_SOLID));

        overlay.addChildren(titleBar, contentContainer, buttonContainer);

        addChild(overlay);

        stopInteractionEventsPropagation();
        addEventListener(UIEvents.BLUR, this::onBlur);
        addEventListener(UIEvents.KEY_DOWN, this::keyDown);
    }

    protected void keyDown(UIEvent event) {
        if (autoClose && event.keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
        }
    }

    protected void onBlur(UIEvent event) {
        if (event.relatedTarget != null && this.isAncestorOf(event.relatedTarget)) { // focus on children
            return;
        }

        if (event.target == this) { // lose focus
            if (isChildHover() && event.relatedTarget == null) {
                focus();
            } else {
                if(autoClose) {
                    close();
                }
            }
        } else { // child lose focus
            if (event.relatedTarget == null && isChildHover()) {
                focus();
            } else {
                if(autoClose) {
                    close();
                }
            }
        }
    }

    /**
     * Closes the dialog and removes it from its parent if it has one.
     */
    public void close(){
        if (this.getParent() != null) {
            this.getParent().removeChild(this);
        }
        if (onClose != null) {
            onClose.run();
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
     * Shows the dialog as a child of the specified UIElement parent.
     * This will add the dialog to the parent's children and focus it.
     * NOTE: ypu should always call this method to show the dialog after creating it,
     *
     * @param parent the UIElement that will be the parent of this dialog
     */
    public Dialog show(UIElement parent) {
        parent.addChild(this);
        focus();
        return this;
    }

    /**
     * Sets the width of the dialog. by default, it will be 150px.
     */
    public Dialog width(StyleSizeLength width) {
        overlay.layout(layout -> layout.setWidth(width));
        return this;
    }

    /**
     * Draw a dark background behind the dialog.
     */
    public Dialog darkenBackground() {
        this.style(style -> style.backgroundTexture(ColorPattern.T_BLACK.rectTexture()));
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
        dialog.addContent(textField.layout(layout -> layout.setWidthPercent(100)));
        dialog.addButton(new Button()
                .setOnClick(e -> {
                    result.accept(textField.getText());
                    dialog.close();
                })
                .setText("ldlib.gui.tips.confirm"));
        dialog.addButton(new Button()
                .setOnClick(e -> dialog.close())
                .setText("ldlib.gui.tips.cancel"));
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
                .setText(info).layout(layout -> layout.setWidthPercent(100)));
        dialog.addButton(new Button().setOnClick(e -> dialog.close()).setText("ldlib.gui.tips.confirm"));
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
                .setText(info).layout(layout -> layout.setWidthPercent(100)));
        dialog.addButton(new Button()
                .setOnClick(e -> {
                    if (onClosed != null) {
                        onClosed.accept(true);
                    }
                    dialog.close();
                })
                .setText("ldlib.gui.tips.confirm"));
        dialog.addButton(new Button()
                .setOnClick(e -> {
                    if (onClosed != null) {
                        onClosed.accept(false);
                    }
                    dialog.close();
                })
                .setText("ldlib.gui.tips.cancel"));
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
    public static Dialog showFileDialog(String title, File dir, boolean isSelector, @Nullable Predicate<TreeNode<File, Void>> valid, Consumer<File> result) {
        var dialog = new Dialog();
        var textField = new TextField();
        var treeList = new TreeList<>(new FileNode(dir).setValid(valid));
        if (!dir.isDirectory()) {
            if (!dir.mkdirs()) {
                return dialog;
            }
        }
        dialog.overlay.layout(layout -> layout.setWidth(200));
        dialog.setTitle(title);
        dialog.addContent(new UIElement().layout(layout -> {
            layout.setWidthPercent(100);
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setGap(YogaGutter.ALL, 2);
        }).addChildren(textField.layout(layout -> layout.setFlex(1)), new Button().setOnClick(e -> {
            Util.getPlatform().openFile(dir.isDirectory() ? dir : dir.getParentFile());
        }).noText().layout(layout -> {
            layout.setWidth(14);
            layout.setHeight(14);
            layout.setPadding(YogaEdge.ALL, 3);
        }).addChild(new UIElement().layout(layout -> layout.setWidthPercent(100)).style(style -> style.backgroundTexture(Icons.FOLDER)))));
        dialog.addContent(new ScrollerView().addScrollViewChild(treeList
                .setOnSelectedChanged(selected -> {
                    if (selected.isEmpty()) return;
                    var first = selected.stream().findFirst().get();
                    if (isSelector) {
                        textField.setText(first.getKey().toString(), false);
                    } else if (first.getKey().isFile()) {
                        textField.setText(first.getKey().getName(), false);
                    } else {
                        textField.setText("", false);
                    }
                })
                .setOnDoubleClickNode(node -> {
                    var file = node.getKey();
                    if (isSelector && file.isFile()) {
                        dialog.close();
                        if (result != null) result.accept(file);
                    }
                })
                .setNodeUISupplier(TreeList.iconTextTemplate(
                        node -> node.getKey().isDirectory() ?
                                Icons.FOLDER :
                                Icons.getIcon(node.getKey().getName()
                                        .substring(node.getKey().getName().lastIndexOf('.') + 1)),
                        node -> node.getKey().getName()))
                .reloadList()).layout(layout -> {
                    layout.setWidthPercent(100);
                    layout.setHeight(180);
                })
        );
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
                .setText("ldlib.gui.tips.confirm"));
        dialog.addButton(new Button()
                .setOnClick(e -> dialog.close())
                .setText("ldlib.gui.tips.cancel"));
        return dialog;
    }

    /**
     * Creates a predicate that filters out nodes based on their suffixes.
     * @param suffixes the suffixes to filter out, e.g. ".txt", ".jpg"
     */
    public static Predicate<TreeNode<File, Void>> suffixFilter(String... suffixes) {
        return node -> {
            for (String suffix : suffixes) {
                if (!node.getKey().isFile() || node.getKey().getName().toLowerCase().endsWith(suffix.toLowerCase())) {
                    return true;
                }
            }
            return false;
        };
    }

}
