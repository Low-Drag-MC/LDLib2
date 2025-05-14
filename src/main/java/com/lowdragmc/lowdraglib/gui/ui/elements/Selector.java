package com.lowdragmc.lowdraglib.gui.ui.elements;

import com.lowdragmc.lowdraglib.editor.Icons;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib.gui.ui.style.Style;
import com.lowdragmc.lowdraglib.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib.gui.ui.styletemplate.Sprites;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import org.appliedenergistics.yoga.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Selector extends UIElement {
    @Accessors(chain = true, fluent = true)
    public static class SelectorStyle extends Style {
        @Getter
        @Setter
        private IGuiTexture focusOverlay = Sprites.RECT_RD_T_SOLID;
        @Getter
        @Setter
        private int maxItemCount = 5;

        public SelectorStyle(UIElement holder) {
            super(holder);
        }
    }
    public final UIElement display;
    public final Label label;
    public final UIElement buttonIcon;
    public final UIElement dialog;
    @Getter
    private final SelectorStyle selectorStyle = new SelectorStyle(this);

    // runtime
    @Getter
    private boolean isOpen = false;

    public Selector() {
        getLayout().setHeight(14);
        getStyle().backgroundTexture(Sprites.RECT_RD_LIGHT);
        setFocusable(true);
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.BLUR, this::onBlur);

        this.label = new Label();
        this.label
                .textStyle(style -> style
                        .textAlignHorizontal(Horizontal.LEFT)
                        .textAlignVertical(Vertical.CENTER))
                .layout(layout -> {
                    layout.setHeightPercent(100);
                    layout.setFlexGrow(1);
                });
        this.label.setText("---");

        this.buttonIcon = new UIElement();
        this.buttonIcon
                .layout(layout -> {
                    layout.setWidth(14);
                    layout.setHeight(14);
                    layout.setMargin(YogaEdge.LEFT, 2);
                })
                .style(style -> style.backgroundTexture(Icons.DOWN_ARROW_NO_BAR));
        this.display = new UIElement()
                .layout(layout -> {
                    layout.setFlexDirection(YogaFlexDirection.ROW);
                    layout.setAlignItems(YogaAlign.CENTER);
                    layout.setPadding(YogaEdge.ALL, 2);
                    layout.setPadding(YogaEdge.LEFT, 4);
                    layout.setHeightPercent(100);
                    layout.setWidthPercent(100);
                })
                .addChildren(label, buttonIcon);

        this.dialog = new UIElement();
        this.dialog
                .setId("selector#dialog")
                .layout(layout -> {
                    layout.setWidthPercent(40);
                    layout.setHeight(60);
                    layout.setPositionType(YogaPositionType.ABSOLUTE);
                    layout.setPositionPercent(YogaEdge.TOP, 100);
                })
                .setDisplay(YogaDisplay.NONE)
                .style(style -> style.zIndex(1).backgroundTexture(Sprites.RECT_DARK))
                .stopInteractionEventsPropagation();

        addChildren(display, dialog);
    }

    ///  events
    protected void onMouseDown(UIEvent event) {
        if (event.button == 0) {
            if (isOpen) {
                hide();
                blur();
            } else {
                show();
                focus();
            }
        }
    }

    protected void onBlur(UIEvent event) {
        if (isChildHover()) {
            focus();
        } else {
            hide();
        }
    }

    public Selector selectorStyle(Consumer<SelectorStyle> style) {
        style.accept(getSelectorStyle());
        onStyleChanged();
        return this;
    }

    @Override
    public void applyStyle(Map<String, StyleValue<?>> values) {
        super.applyStyle(values);
        selectorStyle.applyStyles(values);
    }

    /// Logic
    public void show() {
        if (this.isOpen) {
            return;
        }
        this.isOpen = true;
        this.dialog.setDisplay(YogaDisplay.FLEX);
    }

    public void hide() {
        if (!this.isOpen) {
            return;
        }
        this.isOpen = false;
        this.dialog.setDisplay(YogaDisplay.NONE);
    }


    /// rendering
    @Override
    public void drawBackgroundOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawBackgroundOverlay(graphics, mouseX, mouseY, partialTicks);
        if (isChildHover() || isFocused()) {
            getSelectorStyle().focusOverlay().draw(graphics, mouseX, mouseY, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), partialTicks);
        }
    }
}
