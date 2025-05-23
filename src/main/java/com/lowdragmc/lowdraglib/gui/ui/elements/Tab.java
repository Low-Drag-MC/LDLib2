package com.lowdragmc.lowdraglib.gui.ui.elements;

import com.google.common.util.concurrent.Runnables;
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
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaEdge;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
public class Tab extends TextElement {
    @Accessors(chain = true, fluent = true)
    public static class TabStyle extends Style {
        @Getter @Setter
        private IGuiTexture baseTexture = Sprites.TAB_DARK;
        @Getter @Setter
        private IGuiTexture hoverTexture = Sprites.TAB_WHITE;
        @Getter @Setter
        private IGuiTexture selectedTexture = Sprites.TAB;

        public TabStyle(UIElement holder) {
            super(holder);
        }
    }

    @Getter
    private final TabStyle tabStyle = new TabStyle(this);
    @Setter
    private Runnable onTabSelected = Runnables.doNothing();
    @Setter
    private Runnable onTabUnselected = Runnables.doNothing();
    // runtime
    private boolean isSelected = false;
    private boolean isHovered = false;

    public Tab() {
        getLayout().setHeight(16);
        getLayout().setPadding(YogaEdge.ALL, 3);

        textStyle(textStyle -> {
            textStyle.textAlignHorizontal(Horizontal.CENTER);
            textStyle.textAlignVertical(Vertical.CENTER);
            textStyle.adaptiveWidth(true);
        });

        addEventListener(UIEvents.MOUSE_ENTER, this::onMouseEnter, true);
        addEventListener(UIEvents.MOUSE_LEAVE, this::onMouseLeave, true);
    }

    public Tab tabStyle(Consumer<TabStyle> tabStyle) {
        tabStyle.accept(this.tabStyle);
        onStyleChanged();
        return this;
    }

    @Override
    public void applyStyle(Map<String, StyleValue<?>> values) {
        super.applyStyle(values);
        tabStyle.applyStyles(values);
    }

    @Override
    public Tab setText(String text, boolean translate) {
        return (Tab) super.setText(text, translate);
    }

    @Override
    public Tab setText(String text) {
        return (Tab) super.setText(text);
    }

    @Override
    public Tab setText(Component text) {
        return (Tab) super.setText(text);
    }

    @Override
    public Tab textStyle(Consumer<TextStyle> style) {
        return (Tab) super.textStyle(style);
    }

    public void setSelected(boolean selected) {
        if (isSelected == selected) {
            return;
        }
        this.isSelected = selected;
        if (selected) {
            onTabSelected.run();
        } else {
            onTabUnselected.run();
        }
    }

    /// events
    protected void onMouseEnter(UIEvent event) {
        isHovered = true;
    }

    protected void onMouseLeave(UIEvent event) {
        isHovered = false;
    }

    /// rendering
    @Override
    public void drawBackgroundAdditional(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // draw button texture
        var texture = tabStyle.baseTexture;
        if (isSelected) {
            texture = tabStyle.selectedTexture;
        } else if (isHovered) {
            texture = tabStyle.hoverTexture;
        }
        texture.draw(graphics, mouseX, mouseY, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), partialTicks);
        super.drawBackgroundAdditional(graphics, mouseX, mouseY, partialTicks);
    }

}
