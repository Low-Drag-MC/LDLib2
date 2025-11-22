package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.UISoundUtils;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@RemapPrefixForJS("kjs$")
@Accessors(chain = true)
@LDLRegister(name = "button", group = "basic", registry = "ldlib2:ui_element")
public class Button extends UIElement {
    @Configurable(name = "ButtonStyle")
    public class ButtonStyle extends Style {
        private static final Property<?>[] PROPERTIES = new Property[] {
                PropertyRegistry.BASE_BACKGROUND,
                PropertyRegistry.HOVER_BACKGROUND,
                PropertyRegistry.PRESSED_BACKGROUND,
        };

        public ButtonStyle() {
            super(Button.this);
            setDefault(PropertyRegistry.BASE_BACKGROUND, Sprites.RECT_RD);
            setDefault(PropertyRegistry.HOVER_BACKGROUND, Sprites.RECT_RD_LIGHT);
            setDefault(PropertyRegistry.PRESSED_BACKGROUND, Sprites.RECT_RD_DARK);
        }

        @Override
        protected Property<?>[] getProperties() {
            return PROPERTIES;
        }

        public ButtonStyle baseTexture(IGuiTexture texture) {
            set(PropertyRegistry.BASE_BACKGROUND, texture);
            return this;
        }

        public IGuiTexture baseTexture() {
            return getValueSave(PropertyRegistry.BASE_BACKGROUND);
        }

        public ButtonStyle hoverTexture(IGuiTexture texture) {
            set(PropertyRegistry.HOVER_BACKGROUND, texture);
            return this;
        }

        public IGuiTexture hoverTexture() {
            return getValueSave(PropertyRegistry.HOVER_BACKGROUND);
        }

        public ButtonStyle pressedTexture(IGuiTexture texture) {
            set(PropertyRegistry.PRESSED_BACKGROUND, texture);
            return this;
        }

        public IGuiTexture pressedTexture() {
            return getValueSave(PropertyRegistry.PRESSED_BACKGROUND);
        }
    }
    public enum State {
        DEFAULT,
        HOVERED,
        PRESSED
    }

    public final TextElement text = new TextElement();
    @Getter
    private final ButtonStyle buttonStyle = new ButtonStyle();
    @Nullable
    @Setter
    private Consumer<UIEvent> onClick = null;

    // runtime
    @Getter
    private State state = State.DEFAULT;

    public Button() {
        super();
        getLayout().setFlexDirection(YogaFlexDirection.ROW);
        getLayout().setHeight(14);
        getLayout().setPadding(YogaEdge.ALL, 2);
        getLayout().setJustifyContent(YogaJustify.CENTER);

        text.addClass("__button_text__");
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
        internalSetup();
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

    public Button addPreIcon(IGuiTexture icon) {
        addChildAt(new UIElement().layout(layout -> layout.setHeightPercent(100).setAspectRatio(1f))
                .style(style -> style.backgroundTexture(icon)),
                0);
        return this;
    }

    public Button addPostIcon(IGuiTexture icon) {
        addChild(new UIElement().layout(layout -> layout.setHeightPercent(100).setAspectRatio(1f))
                        .style(style -> style.backgroundTexture(icon)));
        return this;
    }

    public Button buttonStyle(Consumer<ButtonStyle> style) {
        style.accept(buttonStyle);
        return this;
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        // draw button texture
        var texture = isActive() ? switch (state) {
            case DEFAULT -> getButtonStyle().baseTexture();
            case HOVERED -> getButtonStyle().hoverTexture();
            case PRESSED -> getButtonStyle().pressedTexture();
        } : getButtonStyle().baseTexture();
        guiContext.drawTexture(texture, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
        super.drawBackgroundAdditional(guiContext);
    }

    protected void setButtonState(State state) {
        this.state = state;
    }

    protected void onMouseDown(UIEvent event) {
        // Handle button click
        if (event.button == 0 && isActive()) {
            UISoundUtils.playButtonClickSound();
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
