package com.lowdragmc.lowdraglib.gui.ui.elements;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib.gui.ui.style.Style;
import com.lowdragmc.lowdraglib.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import org.appliedenergistics.yoga.YogaEdge;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
public class Button extends TextElement {
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
    @Getter
    private final ButtonStyle buttonStyle = new ButtonStyle(this);
    @Nullable
    @Setter
    private Consumer<UIEvent> onClick = null;
    @Getter
    private State state = State.DEFAULT;

    public Button() {
        super();
        getLayout().setWidth(60);
        getLayout().setHeight(20);
        getTextStyle()
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER);

        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.MOUSE_UP, this::onMouseUp);
        addEventListener(UIEvents.MOUSE_ENTER, this::onMouseEnter, true);
        addEventListener(UIEvents.MOUSE_LEAVE, this::onMouseLeave, true);
        setText("Button");
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
        var texture = switch (state) {
            case DEFAULT -> getButtonStyle().defaultTexture();
            case HOVERED -> getButtonStyle().hoverTexture();
            case PRESSED -> getButtonStyle().pressedTexture();
        };
        texture.draw(graphics, mouseX, mouseY, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), partialTicks);
        super.drawBackgroundAdditional(graphics, mouseX, mouseY, partialTicks);
    }

    protected void setButtonState(State state) {
        this.state = state;
    }

    protected void onMouseDown(UIEvent event) {
        // Handle button click
        if (event.button == 0) {
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
