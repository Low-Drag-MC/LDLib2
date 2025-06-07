package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ColorSelector;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import net.minecraft.client.gui.GuiGraphics;
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

        if (value == null) {
            value = defaultValue;
        }

        this.colorSelector = new ColorSelector();
        this.colorSelector.style(style -> style.zIndex(1).backgroundTexture(Sprites.BORDER));
        this.colorSelector.layout(layout -> {
            layout.setPositionType(YogaPositionType.ABSOLUTE);
            layout.setWidthPercent(100);
            layout.setMaxWidth(150);
            layout.setMinWidth(100);
            layout.setPadding(YogaEdge.ALL, 4);
        });
        this.colorSelector.setOnColorChangeListener(this::updateValueActively);
        this.colorSelector.setFocusable(true);
        this.colorSelector.setEnforceFocus(e -> hide());
        this.colorSelector.addEventListener(UIEvents.LAYOUT_CHANGED, e -> colorSelector.adaptPositionToScreen());

        inlineContainer.addChildren(colorPreview = new UIElement().layout(layout -> {
            layout.setHeight(14);
            layout.setPadding(YogaEdge.ALL, 3);
        }).style(style -> style.backgroundTexture(Sprites.RECT_RD_SOLID))
                .addChildren(new UIElement()
                        .layout(layout -> layout.setHeightPercent(100))
                        .style(style -> style.backgroundTexture(this::drawColorPreview))
                        .addEventListener(UIEvents.MOUSE_DOWN, this::onClick)));

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
        var parent = this.colorSelector.getParent();
        if (parent != null) {
            return;
        }
        var mui = getModularUI();
        if (mui != null) {
            var root = mui.ui.rootElement;
            root.addChild(colorSelector.layout(layout -> {
                var x = colorPreview.getPositionX();
                var y = colorPreview.getPositionY();
                layout.setPosition(YogaEdge.LEFT, x - root.getLayoutX());
                layout.setPosition(YogaEdge.TOP, y - root.getLayoutY());
                layout.setWidth(colorPreview.getSizeWidth());
            }));
            this.colorSelector.focus();
        }
    }

    public void hide() {
        var parent = this.colorSelector.getParent();
        if (parent != null) {
            this.colorSelector.blur();
            parent.removeChild(this.colorSelector);
        }
    }

    protected void onClick(UIEvent event) {
        if (this.colorSelector.getParent() != null) {
            hide();
        } else {
            show();
        }
    }

    protected void drawColorPreview(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, float width, float height, float partialTicks) {
        int color = value == null ? defaultValue : value;
        graphics.drawManaged(() -> {
            DrawerHelper.drawSolidRect(graphics, x, y, width, height, color, false);
            DrawerHelper.drawSolidRect(graphics, x - 1, y, 1, height, color, false);
            DrawerHelper.drawSolidRect(graphics, x + width, y, 1, height, color, false);
            DrawerHelper.drawSolidRect(graphics, x, y - 1, width, 1, color, false);
            DrawerHelper.drawSolidRect(graphics, x, y + height, width, 1, color, false);
        });
    }

}
