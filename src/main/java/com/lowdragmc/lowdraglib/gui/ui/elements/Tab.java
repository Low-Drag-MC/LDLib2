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
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaGutter;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
public class Tab extends UIElement {
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
    public final TextElement text = new TextElement();
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
        getLayout().setFlexDirection(YogaFlexDirection.ROW);

        text.layout(layout -> layout.setHeightPercent(100));
        text.textStyle(textStyle -> {
            textStyle.textAlignHorizontal(Horizontal.CENTER);
            textStyle.textAlignVertical(Vertical.CENTER);
            textStyle.adaptiveWidth(true);
        });

        addEventListener(UIEvents.MOUSE_ENTER, this::onMouseEnter, true);
        addEventListener(UIEvents.MOUSE_LEAVE, this::onMouseLeave, true);
        addChild(text);
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

    public Tab setText(String text, boolean translate) {
        this.text.setText(text, translate);
        return this;
    }

    public Tab setText(String text) {
        return setText(text, false);
    }

    public Tab setText(Component text) {
        this.text.setText(text);
        return this;
    }

    public Tab textStyle(Consumer<TextElement.TextStyle> style) {
        text.textStyle(style);
        return this;
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
