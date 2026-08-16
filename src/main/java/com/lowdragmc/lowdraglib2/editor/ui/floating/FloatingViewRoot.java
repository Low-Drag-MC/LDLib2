package com.lowdragmc.lowdraglib2.editor.ui.floating;

import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.EditorHost;
import com.lowdragmc.lowdraglib2.editor.ui.SplittableWindow;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The contents of a floating window: a title bar we draw ourselves, and a dock tree under it.
 *
 * <p>The chrome is ours rather than the operating system's because the native one is not free. A
 * decorated window's move and resize gestures run in a nested modal event loop inside
 * {@code glfwPollEvents}, which Minecraft calls every frame — so for as long as the user holds the
 * title bar, the entire game is frozen. Drawing the bar here and moving the window through
 * {@code glfwSetWindowPos} sidesteps that, and has the side benefit of looking identical to the
 * in-game panel used when a native window cannot be opened.
 *
 * <p>The dock tree is a real, immortal {@link SplittableWindow}, so a floating window is a fully
 * fledged dock host: it has tabs, it can be split, and views can be dragged between its panes with
 * the code that already does that in the editor.
 */
public class FloatingViewRoot extends UIElement implements EditorHost {

    public static final int TITLE_BAR_HEIGHT = 15;

    /**
     * The editor whose views this window holds, so a floated {@code View} can still find it.
     */
    @Getter
    @Setter
    @Nullable
    private Editor hostedEditor;

    public final UIElement titleBar = new UIElement();
    /**
     * Spacer that pushes the window buttons to the right. The whole title bar moves the window, so
     * this is purely layout.
     */
    public final UIElement dragHandle = new UIElement();
    public final Label titleLabel = new Label();
    public final Button maximizeButton = new Button();
    public final Button closeButton = new Button();
    public final UIElement content = new UIElement();
    public final SplittableWindow rootWindow = new SplittableWindow().setImmortal(true);

    public FloatingViewRoot() {
        getLayout().widthPercent(100);
        getLayout().heightPercent(100);
        getStyle().backgroundTexture(Sprites.RECT_SOLID);
        addClass("__floating-window__");

        titleLabel.setText(Component.empty());
        // The label fills the bar's height, so without an explicit vertical alignment the text sits
        // at the very top of it rather than on the bar's centre line.
        titleLabel.textStyle(textStyle -> {
            textStyle.adaptiveWidth(true);
            textStyle.textAlignHorizontal(Horizontal.LEFT);
            textStyle.textAlignVertical(Vertical.CENTER);
        });
        titleLabel.layout(layout -> layout.heightPercent(100));

        dragHandle.layout(layout -> {
            layout.flex(1);
            layout.heightPercent(100);
        }).addClass("__floating-window_drag-handle__");

        titleBar.layout(layout -> {
            layout.widthPercent(100);
            layout.height(TITLE_BAR_HEIGHT);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(2);
            layout.paddingHorizontal(6);
            layout.paddingVertical(1);
        }).style(style -> style.backgroundTexture(ColorPattern.BLACK.rectTexture()))
                .addClass("__floating-window_title-bar__")
                .addChildren(titleLabel, dragHandle, maximizeButton, closeButton);

        maximizeButton.noText()
                .addPreIcon(DynamicTexture.of(() -> maximized ? Icons.WINDOW_RESTORE : Icons.WINDOW_MAXIMIZE))
                .addClass("__white_icon__")
                .layout(layout -> layout.height(12));
        closeButton.noText()
                .addPreIcon(Icons.WINDOW_CLOSE)
                .addClass("__white_icon__")
                .layout(layout -> layout.height(12));

        content.layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
        }).addClass("__floating-window_content__").addChild(rootWindow);

        addChildren(titleBar, content);
        titleBar.moveInlineAsDefault();
        content.moveInlineAsDefault();
        moveInlineAsDefault();
    }

    /**
     * Mirrors the native maximized state so the button can draw the right icon. Set by the window,
     * read by {@link #maximizeButton}'s dynamic texture.
     */
    private boolean maximized;

    public void setMaximized(boolean maximized) {
        this.maximized = maximized;
    }

    public void setTitle(Component title) {
        titleLabel.setText(title);
    }
}
