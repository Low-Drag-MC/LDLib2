package com.lowdragmc.lowdraglib.gui.ui.elements;

import com.lowdragmc.lowdraglib.editor.ColorPattern;
import com.lowdragmc.lowdraglib.editor.Icons;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib.gui.ui.style.Style;
import com.lowdragmc.lowdraglib.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib.gui.ui.styletemplate.Sprites;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
public class Toggle extends UIElement {
    @Accessors(chain = true, fluent = true)
    public static class ToggleStyle extends Style {
        @Getter
        @Setter
        private IGuiTexture baseTexture = Sprites.RECT_DARK;
        @Getter
        @Setter
        private IGuiTexture hoverTexture = new GuiTextureGroup(Sprites.RECT_DARK, ColorPattern.WHITE.borderTexture(-1));
        @Getter
        @Setter
        private IGuiTexture unmarkTexture = IGuiTexture.EMPTY;
        @Getter
        @Setter
        private IGuiTexture markTexture = Icons.CHECK_SPRITE;

        public ToggleStyle(UIElement holder) {
            super(holder);
        }
    }
    public final Button toggleButton;
    public final UIElement markIcon;
    public final Label toggleLabel;
    @Getter
    private final ToggleStyle toggleStyle = new ToggleStyle(this);
    @Getter
    private boolean isOn = false;
    @Nullable
    @Setter
    private BooleanConsumer onToggleChanged = null;

    public Toggle() {
        getLayout().setFlexDirection(YogaFlexDirection.ROW);
        getLayout().setAlignItems(YogaAlign.CENTER);
        getLayout().setPadding(YogaEdge.ALL, 2);
        getLayout().setHeight(14);

        this.toggleButton = new Button();
        this.toggleButton
                .setOnClick(this::onToggleClick)
                .buttonStyle(style -> style
                        .defaultTexture(toggleStyle.baseTexture())
                        .hoverTexture(toggleStyle.hoverTexture())
                        .pressedTexture(toggleStyle.hoverTexture()))
                .setText("")
                .layout(layout -> {
                    layout.setWidth(12);
                    layout.setHeight(12);
                })
                .addChild(this.markIcon = new UIElement()
                        .layout(layout -> {
                            layout.setWidthPercent(100);
                            layout.setHeightPercent(100);
                        })
                        .style(style -> style.backgroundTexture(toggleStyle.unmarkTexture())));
        this.toggleLabel = new Label();
        this.toggleLabel
                .textStyle(style -> style
                        .textAlignHorizontal(Horizontal.LEFT)
                        .textAlignVertical(Vertical.CENTER))
                .layout(layout -> {
                    layout.setHeightPercent(100);
                    layout.setFlexGrow(1);
                    layout.setMargin(YogaEdge.LEFT, 2);
                });
        this.toggleLabel.setText("Toggle");
        addChildren(toggleButton, toggleLabel);
    }

    public Toggle toggleStyle(Consumer<ToggleStyle> style) {
        style.accept(toggleStyle);
        onStyleChanged();
        return this;
    }

    @Override
    public void applyStyle(Map<String, StyleValue<?>> values) {
        super.applyStyle(values);
        toggleStyle.applyStyles(values);
    }

    @Override
    public boolean isInternalElement(UIElement child) {
        if (child == toggleButton || child == toggleLabel) {
            return true;
        }
        return super.isInternalElement(child);
    }

    protected void onToggleClick(UIEvent event) {
        setOn(!isOn, true);
    }

    public Toggle setOn(boolean on) {
        return setOn(on, true);
    }

    public Toggle setOn(boolean on, boolean notifyChange) {
        isOn = on;
        markIcon.getStyle().backgroundTexture(isOn ? toggleStyle.markTexture() : toggleStyle.unmarkTexture());
        if (onToggleChanged != null && notifyChange) {
            onToggleChanged.accept(isOn);
        }
        return this;
    }

}
