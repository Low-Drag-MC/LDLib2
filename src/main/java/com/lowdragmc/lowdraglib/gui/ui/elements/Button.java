package com.lowdragmc.lowdraglib.gui.ui.elements;

import com.lowdragmc.lowdraglib.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class Button extends TextElement {
    public enum State {
        DEFAULT,
        HOVERED,
        PRESSED
    }
    @Nullable
    @Setter
    private Consumer<UIEvent> onClick = null;
    @Getter
    private State state = State.DEFAULT;

    public Button() {
        super();
        getStyle().backgroundTexture(Sprites.RECT_RD)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER);
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.MOUSE_UP, this::onMouseUp);
        addEventListener(UIEvents.MOUSE_ENTER, this::onMouseEnter);
        addEventListener(UIEvents.MOUSE_LEAVE, this::onMouseLeave);
        setText("Button");
    }

    protected void setState(State state) {
        if (this.state == state) return;
        this.state = state;
        switch (state) {
            case DEFAULT -> style(style -> style.backgroundTexture(Sprites.RECT_RD));
            case HOVERED -> style(style -> style.backgroundTexture(Sprites.RECT_RD_LIGHT));
            case PRESSED -> style(style -> style.backgroundTexture(Sprites.RECT_RD_DARK));
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
            setState(State.PRESSED);
        }
    }

    protected void onMouseUp(UIEvent event) {
        setState(State.HOVERED);
    }

    protected void onMouseEnter(UIEvent event) {
        setState(State.HOVERED);
    }

    protected void onMouseLeave(UIEvent event) {
        setState(State.DEFAULT);
    }

}
