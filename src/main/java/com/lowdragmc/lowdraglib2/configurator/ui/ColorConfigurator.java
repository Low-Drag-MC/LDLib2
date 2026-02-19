package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ColorSelector;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.client.gui.GuiGraphics;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ColorConfigurator extends ValueConfigurator<Integer> {
    public final ColorSelector colorSelector;
    public final UIElement colorPreview;

    public ColorConfigurator(String name, Supplier<Integer> supplier, Consumer<Integer> onUpdate, @Nonnull Integer defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        setCopiable(value -> value);

        if (value == null) {
            value = defaultValue;
        }

        this.colorSelector = new ColorSelector();
        this.colorSelector.style(style -> {
            style.setPipelineState(StyleOrigin.DEFAULT);
            style.backgroundTexture(Sprites.RECT_SOLID);
            style.setPipelineState(StyleOrigin.INLINE);
        });
        this.colorSelector.addClass("panel_bg");
        this.colorSelector.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.widthPercent(100);
            layout.maxWidth(150);
            layout.minWidth(100);
            layout.paddingAll(4);
        });
        this.colorSelector.setOnColorChangeListener(this::updateValueActively);
        this.colorSelector.setFocusable(true);
        this.colorSelector.setEnforceFocus(e -> hide());
        this.colorSelector.addEventListener(UIEvents.LAYOUT_CHANGED, e -> colorSelector.adaptPositionToScreen());

        colorPreview = new UIElement();
        inlineContainer.addChildren(colorPreview.layout(layout -> {
            layout.setPipelineState(StyleOrigin.DEFAULT);
            layout.height(14);
            layout.paddingAll(3);
            layout.setPipelineState(StyleOrigin.INLINE);
        }).style(style -> {
            style.setPipelineState(StyleOrigin.DEFAULT);
            style.backgroundTexture(Sprites.RECT_RD_SOLID);
            style.setPipelineState(StyleOrigin.IMPORTANT);
            style.overlayTexture(DynamicTexture.of(() -> colorPreview.isSelfOrChildHover() ? Sprites.RECT_RD_T_SOLID : IGuiTexture.EMPTY));
            style.setPipelineState(StyleOrigin.INLINE);
        }).addClass("configurator_preview_bg").addChildren(new UIElement()
                .layout(layout -> layout.heightPercent(100))
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
                var worldMouse = colorPreview.getWorldMouse(colorPreview.getPositionX(), colorPreview.getPositionY());
                var layoutOffset = root.worldToLocalLayoutOffset(worldMouse);
                layout.left(layoutOffset.x);
                layout.top(layoutOffset.y);
                layout.width(colorPreview.getSizeWidth());
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

    protected void drawColorPreview(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width, float height, float partialTicks) {
        int color = value == null ? defaultValue : value;
        DrawerHelper.drawSolidRect(graphics, x, y, width, height, color);
        DrawerHelper.drawSolidRect(graphics, x - 1, y, 1, height, color);
        DrawerHelper.drawSolidRect(graphics, x + width, y, 1, height, color);
        DrawerHelper.drawSolidRect(graphics, x, y - 1, width, 1, color);
        DrawerHelper.drawSolidRect(graphics, x, y + height, width, 1, color);
    }
}