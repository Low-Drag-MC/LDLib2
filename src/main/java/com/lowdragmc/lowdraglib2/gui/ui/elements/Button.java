package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.widget.Widget;
import dev.latvian.mods.rhino.util.HideFromJS;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
public class Button extends UIElement {
    @Accessors(chain = true, fluent = true)
    public static class ButtonStyle extends Style {
        @Getter
        @Setter
        private IGuiTexture defaultTexture = Sprites.RECT_RD;
        @Getter
        @Setter
        private IGuiTexture hoverTexture = Sprites.RECT_RD_LIGHT;
        @Getter
        @Setter
        private IGuiTexture pressedTexture = Sprites.RECT_RD_DARK;

        public ButtonStyle(UIElement holder) {
            super(holder);
        }
    }
    public enum State {
        DEFAULT,
        HOVERED,
        PRESSED
    }
    public final TextElement text = new TextElement();
    @Getter
    private final ButtonStyle buttonStyle = new ButtonStyle(this);
    @Nullable
    @Setter
    private Consumer<UIEvent> onClick = null;
    @Getter
    private State state = State.DEFAULT;

    public Button() {
        super();
        getLayout().setFlexDirection(YogaFlexDirection.ROW);
        getLayout().setHeight(14);
        getLayout().setPadding(YogaEdge.ALL, 2);
        getLayout().setJustifyContent(YogaJustify.CENTER);

        text.getLayout().setHeightPercent(100);
        text.getLayout().setMargin(YogaEdge.HORIZONTAL, 2);
        text.getTextStyle()
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .adaptiveWidth(true);

        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.MOUSE_UP, this::onMouseUp);
        addEventListener(UIEvents.MOUSE_ENTER, this::onMouseEnter, true);
        addEventListener(UIEvents.MOUSE_LEAVE, this::onMouseLeave, true);
        setText("Button");

        addChild(text);
    }

    public Button textStyle(Consumer<TextElement.TextStyle> style) {
        text.textStyle(style);
        return this;
    }

    public Button noText() {
        text.setDisplay(YogaDisplay.NONE);
        return this;
    }

    @HideFromJS
    public Button setText(Component text) {
        this.text.setText(text);
        return this;
    }

    @HideFromJS
    public Button setText(String text) {
        this.text.setText(text);
        return this;
    }

    public Button setText(String text, boolean translate) {
        this.text.setText(text, translate);
        return this;
    }

    public Button buttonStyle(Consumer<ButtonStyle> style) {
        style.accept(buttonStyle);
        onStyleChanged();
        return this;
    }

    @Override
    public void applyStyle(Map<String, StyleValue<?>> values) {
        super.applyStyle(values);
        buttonStyle.applyStyles(values);
    }

    @Override
    public void drawBackgroundAdditional(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // draw button texture
        var texture = isActive() ? switch (state) {
            case DEFAULT -> getButtonStyle().defaultTexture();
            case HOVERED -> getButtonStyle().hoverTexture();
            case PRESSED -> getButtonStyle().pressedTexture();
        } : getButtonStyle().defaultTexture();
        texture.draw(graphics, mouseX, mouseY, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), partialTicks);
        super.drawBackgroundAdditional(graphics, mouseX, mouseY, partialTicks);
    }

    protected void setButtonState(State state) {
        this.state = state;
    }

    protected void onMouseDown(UIEvent event) {
        // Handle button click
        if (event.button == 0 && isActive()) {
            Widget.playButtonClickSound();
            if (onClick != null) {
                onClick.accept(event);
            }
            // pressed state
            setButtonState(State.PRESSED);
        }
    }

    protected void onMouseUp(UIEvent event) {
        setButtonState(State.HOVERED);
    }

    protected void onMouseEnter(UIEvent event) {
        setButtonState(State.HOVERED);
    }

    protected void onMouseLeave(UIEvent event) {
        setButtonState(State.DEFAULT);
    }

}
