package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ColorSelector;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.client.gui.GuiGraphics;
import org.appliedenergistics.yoga.YogaEdge;
import org.joml.Vector2f;
import org.joml.Vector4f;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class HDRColorConfigurator extends ValueConfigurator<Vector4f> {
    public final ColorSelector colorSelector;
    public final NumberConfigurator intensityConfigurator;
    public final UIElement dialog;
    public final UIElement colorPreview;

    public HDRColorConfigurator(String name, Supplier<Vector4f> supplier, Consumer<Vector4f> onUpdate, @Nonnull Vector4f defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        setCopiable(value -> value);

        if (value == null) {
            value = defaultValue;
        }


        this.dialog = new UIElement();
        this.colorSelector = new ColorSelector();
        this.intensityConfigurator = new NumberConfigurator("HDR", () -> supplier.get().w,
                intensity -> updateValueActively(new Vector4f(
                        ColorUtils.red(colorSelector.getColor()), ColorUtils.green(colorSelector.getColor()), ColorUtils.blue(colorSelector.getColor()),
                        intensity.floatValue())), value.w, forceUpdate);
        this.intensityConfigurator.setType(ConfigNumber.Type.FLOAT);
        this.colorSelector.setOnColorChangeListener(color -> updateValueActively(new Vector4f(
                ColorUtils.red(color), ColorUtils.green(color), ColorUtils.blue(color),
                Optional.ofNullable(this.intensityConfigurator.getValue()).map(Number::floatValue).orElse(1f))));
        this.colorSelector.alphaSlider.setDisplay(false);


        inlineContainer.addChildren(colorPreview = new UIElement().layout(layout -> {
            layout.height(14);
            layout.paddingAll(3);
        }).style(style -> style.backgroundTexture(Sprites.RECT_RD_SOLID))
                .addChildren(new UIElement()
                        .layout(layout -> layout.heightPercent(100))
                        .style(style -> style.backgroundTexture(this::drawColorPreview))
                        .addEventListener(UIEvents.MOUSE_DOWN, this::onClick)));

        this.colorSelector.setColor(ColorUtils.color(1, value.x, value.y, value.z), false);

        this.dialog.style(style -> style.zIndex(1).backgroundTexture(Sprites.BORDER));
        this.dialog.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.widthPercent(100);
            layout.maxWidth(150);
            layout.minWidth(100);
            layout.paddingAll(4);
        });
        this.dialog.setFocusable(true);
        this.dialog.setEnforceFocus(e -> hide());
        this.dialog.addEventListener(UIEvents.LAYOUT_CHANGED, e -> {
            this.updateDialogPosition();
            e.currentElement.adaptPositionToScreen();
        });
        this.dialog.addChildren(this.colorSelector, this.intensityConfigurator);
    }

    @Override
    protected void onValueUpdatePassively(Vector4f newValue) {
        if (newValue == null) newValue = defaultValue;
        if (newValue.equals(value)) return;
        super.onValueUpdatePassively(newValue);
        this.colorSelector.setColor(ColorUtils.color(1, newValue.x, newValue.y, newValue.z), false);
        this.intensityConfigurator.onValueUpdatePassively(newValue.w);
    }

    protected void updateDialogPosition() {
        var mui = getModularUI();
        if (mui != null) {
            var root = mui.ui.rootElement;
            var worldPos = this.localToWorld(new Vector2f(getPositionX(), getPositionY() + getSizeHeight()));
            var pos = root.worldToLocalLayoutOffset(worldPos);
            this.dialog.layout(layout -> {
                layout.left(pos.x);
                layout.top(pos.y);
                layout.width(Math.max(this.getSizeWidth(), 50));
            });
        }
    }

    public void show() {
        var parent = this.dialog.getParent();
        if (parent != null) {
            return;
        }
        var mui = getModularUI();
        if (mui != null) {
            var root = mui.ui.rootElement;
            root.addChild(dialog);
            this.updateDialogPosition();
            this.dialog.focus();
        }
    }

    public void hide() {
        var parent = this.dialog.getParent();
        if (parent != null) {
            this.dialog.blur();
            parent.removeChild(this.dialog);
        }
    }

    protected void onClick(UIEvent event) {
        if (this.dialog.getParent() != null) {
            hide();
        } else {
            show();
        }
    }

    protected void drawColorPreview(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width, float height, float partialTicks) {
        var hdr = value == null ? defaultValue : value;
        var color = ColorUtils.color(1, hdr.x, hdr.y, hdr.z);
        DrawerHelper.drawSolidRect(graphics, x, y, width, height, color);
        DrawerHelper.drawSolidRect(graphics, x - 1, y, 1, height, color);
        DrawerHelper.drawSolidRect(graphics, x + width, y, 1, height, color);
        DrawerHelper.drawSolidRect(graphics, x, y - 1, width, 1, color);
        DrawerHelper.drawSolidRect(graphics, x, y + height, width, 1, color);
        var textTexture = new TextTexture("HDR: %.1f".formatted(intensityConfigurator.value.floatValue()));
        textTexture.setType(TextTexture.TextType.ROLL);
        textTexture.setWidth((int) width);
        textTexture.draw(graphics, mouseX, mouseY, x, y + 1, width, height, partialTicks);
    }

}
