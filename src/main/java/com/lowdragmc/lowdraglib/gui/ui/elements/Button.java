package com.lowdragmc.lowdraglib.gui.ui.elements;

import com.lowdragmc.lowdraglib.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib.gui.ui.style.ButtonStyle;
import com.lowdragmc.lowdraglib.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
public class Button extends TextElement {
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
        setTextureByState(state);
    }

    public Button buttonStyle(Consumer<ButtonStyle> style) {
        style.accept(buttonStyle);
        onStyleChanged();
        setTextureByState(state);
        return this;
    }

    @Override
    public void applyStyle(Map<String, StyleValue<?>> values) {
        super.applyStyle(values);
        buttonStyle.applyStyles(values);
    }

    protected void setTextureByState(State state) {
        this.state = state;
        switch (state) {
            case DEFAULT -> getStyle().backgroundTexture(getButtonStyle().defaultTexture());
            case HOVERED -> getStyle().backgroundTexture(getButtonStyle().hoverTexture());
            case PRESSED -> getStyle().backgroundTexture(getButtonStyle().pressedTexture());
        }
    }

    protected void onMouseDown(UIEvent event) {
        // Handle button click
        if (event.button == 0) {
            Widget.playButtonClickSound();
            if (onClick != null) {
                onClick.accept(event);
            }
            // pressed state
            setTextureByState(State.PRESSED);
        }
    }

    protected void onMouseUp(UIEvent event) {
        setTextureByState(State.HOVERED);
    }

    protected void onMouseEnter(UIEvent event) {
        setTextureByState(State.HOVERED);
    }

    protected void onMouseLeave(UIEvent event) {
        setTextureByState(State.DEFAULT);
    }

}
