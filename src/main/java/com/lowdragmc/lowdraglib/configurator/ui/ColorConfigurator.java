package com.lowdragmc.lowdraglib.configurator.ui;

import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.elements.ColorSelector;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import net.minecraft.client.gui.GuiGraphics;
import org.appliedenergistics.yoga.YogaDisplay;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaPositionType;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ColorConfigurator extends ValueConfigurator<Integer> {
    public final ColorSelector colorSelector;
    public final UIElement colorPreview;

    public ColorConfigurator(String name, Supplier<Integer> supplier, Consumer<Integer> onUpdate, @Nonnull Integer defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        setFocusable(true);
        addEventListener(UIEvents.BLUR, this::onBlur, true);

        if (value == null) {
            value = defaultValue;
        }

        this.colorSelector = new ColorSelector();
        this.colorSelector.style(style -> style.zIndex(1).backgroundTexture(Sprites.BORDER)).setDisplay(YogaDisplay.NONE);
        this.colorSelector.layout(layout -> {
            layout.setPositionType(YogaPositionType.ABSOLUTE);
            layout.setWidthPercent(100);
            layout.setMaxWidth(150);
            layout.setMinWidth(100);
            layout.setPadding(YogaEdge.ALL, 4);
        });
        this.colorSelector.setOnColorChangeListener(color -> {
            this.value = color;
            updateValue();
        });

        inlineContainer.addChildren(colorPreview = new UIElement().layout(layout -> {
            layout.setHeight(14);
            layout.setPadding(YogaEdge.ALL, 3);
        }).style(style -> style.backgroundTexture(Sprites.RECT_RD_SOLID))
                .addChildren(new UIElement()
                        .layout(layout -> layout.setHeightPercent(100))
                        .style(style -> style.backgroundTexture(this::drawColorPreview))
                        .addEventListener(UIEvents.MOUSE_DOWN, this::onClick)), colorSelector);

        this.colorSelector.setColor(value, false);
    }

    @Override
    protected void onValueUpdatePassively(Integer newValue) {
        if (newValue == null) newValue = defaultValue;
        if (newValue.equals(value)) return;
        super.onValueUpdatePassively(newValue);
        this.colorSelector.setColor(newValue, false);
    }

    public void show() {
        this.colorSelector.setDisplay(YogaDisplay.FLEX);
    }

    public void hide() {
        this.colorSelector.setDisplay(YogaDisplay.NONE);
    }

    protected void onBlur(UIEvent event) {
        if (event.relatedTarget != null && this.colorSelector.isAncestor(event.relatedTarget)) { // focus on children
            return;
        }

        if (event.target == this) { // lose focus
            if (isChildHover()) {
                focus();
            } else {
                hide();
            }
        } else { // child lose focus
            if (event.relatedTarget == null && isChildHover()) {
                focus();
            } else {
                hide();
            }
        }

    }

    protected void onClick(UIEvent event) {
        if (this.colorSelector.isDisplayed()) {
            hide();
            blur();
        } else {
            show();
            focus();
        }
    }

    protected void drawColorPreview(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, float width, float height, float partialTicks) {
        int color = value == null ? defaultValue : value;
        graphics.drawManaged(() -> {
            DrawerHelper.drawSolidRect(graphics, (int) x, (int) y, (int) width, (int) height, color);
            DrawerHelper.drawSolidRect(graphics, (int) x - 1, (int) y, (int) 1, (int) height, color);
            DrawerHelper.drawSolidRect(graphics, (int) x + (int) width, (int) y, (int) 1, (int) height, color);
            DrawerHelper.drawSolidRect(graphics, (int) x, (int) y - 1, (int) width, (int) 1, color);
            DrawerHelper.drawSolidRect(graphics, (int) x, (int) y + (int) height, (int) width, (int) 1, color);
        });
    }

}
