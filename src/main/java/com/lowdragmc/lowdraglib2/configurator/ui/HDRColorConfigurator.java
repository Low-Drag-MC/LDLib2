package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ColorSelector;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import com.lowdragmc.lowdraglib2.math.HDRColor;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import dev.vfyjxf.taffy.style.TaffyPosition;
import org.joml.Vector2f;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Editor for an {@link HDRColor}: an LDR color picker plus a separate {@code intensity} field, the
 * way Unity's HDR color field works.
 *
 * <p>{@code showAlpha} controls whether the picker's alpha slider is available. It must be
 * <b>false</b> for "HDR emission offset" values (material {@code hdr} fields, shader {@code hdr}/
 * {@code emission} uniforms) where alpha carries no meaning — pinning alpha to 1 there is what makes
 * {@link HDRColor#toVector4fOpaque()} numerically identical to the old {@code rgb * a} encoding.
 */
public class HDRColorConfigurator extends ValueConfigurator<HDRColor> {
    /** Label for the intensity field; shared so other HDR editors (e.g. gradient stops) match. */
    public static final String INTENSITY_LABEL = "ldlib.gui.editor.name.hdr_intensity";
    public final ColorSelector colorSelector;
    public final NumberConfigurator intensityConfigurator;
    public final UIElement dialog;
    public final UIElement colorPreview;
    protected final boolean showAlpha;
    /** The "HDR: x.x" overlay on the swatch. Kept around rather than rebuilt every frame. */
    protected final TextTexture intensityLabel = new TextTexture("").setType(TextTexture.TextType.ROLL);
    private float labelIntensity = Float.NaN;
    private int labelWidth = -1;

    public HDRColorConfigurator(String name, Supplier<HDRColor> supplier, Consumer<HDRColor> onUpdate,
                                @Nonnull HDRColor defaultValue, boolean forceUpdate) {
        this(name, supplier, onUpdate, defaultValue, forceUpdate, true);
    }

    public HDRColorConfigurator(String name, Supplier<HDRColor> supplier, Consumer<HDRColor> onUpdate,
                                @Nonnull HDRColor defaultValue, boolean forceUpdate, boolean showAlpha) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        this.showAlpha = showAlpha;
        setCopiable(HDRColor::copy);

        if (value == null) {
            value = defaultValue.copy();
        }

        this.dialog = new UIElement();
        this.colorSelector = new ColorSelector();
        this.intensityConfigurator = new NumberConfigurator(INTENSITY_LABEL, () -> current().getIntensity(),
                intensity -> updateValueActively(withPickerColor(colorSelector.getColor(), intensity.floatValue())),
                value.getIntensity(), forceUpdate);
        this.intensityConfigurator.setType(ConfigNumber.Type.FLOAT);
        this.intensityConfigurator.setRange(0, Float.MAX_VALUE);
        this.colorSelector.setOnColorChangeListener(color ->
                updateValueActively(withPickerColor(color, current().getIntensity())));
        this.colorSelector.alphaSlider.setDisplay(showAlpha);

        var preview = new UIElement();
        inlineContainer.addChildren(colorPreview = preview.layout(layout -> {
            layout.setPipelineState(StyleOrigin.DEFAULT);
            layout.height(14);
            layout.paddingAll(3);
            layout.setPipelineState(StyleOrigin.INLINE);
        }).style(style -> {
            style.setPipelineState(StyleOrigin.DEFAULT);
            style.backgroundTexture(Sprites.RECT_RD_SOLID);
            // hover highlight, same as ColorConfigurator's swatch
            style.setPipelineState(StyleOrigin.IMPORTANT);
            style.overlayTexture(DynamicTexture.of(() -> preview.isSelfOrChildHover() ? Sprites.RECT_RD_T_SOLID : IGuiTexture.EMPTY));
            style.setPipelineState(StyleOrigin.INLINE);
        }).addClass("configurator_preview_bg").addChildren(new UIElement()
                .layout(layout -> layout.heightPercent(100))
                .style(style -> style.backgroundTexture(GuiTexture.of(this::drawColorPreview)))
                .addEventListener(UIEvents.MOUSE_DOWN, this::onClick)));

        this.colorSelector.setColor(value.baseARGB(), false);

        this.dialog.style(style -> {
            style.setPipelineState(StyleOrigin.DEFAULT);
            style.zIndex(1).backgroundTexture(Sprites.BORDER);
            style.setPipelineState(StyleOrigin.INLINE);
        });
        this.dialog.layout(layout -> {
            layout.setPipelineState(StyleOrigin.DEFAULT);
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.widthPercent(100);
            layout.maxWidth(150);
            layout.minWidth(100);
            layout.paddingAll(4);
            layout.setPipelineState(StyleOrigin.INLINE);
        });
        this.dialog.setFocusable(true);
        this.dialog.setEnforceFocus(e -> hide());
        this.dialog.addEventListener(UIEvents.LAYOUT_CHANGED, e -> {
            this.updateDialogPosition();
            e.currentElement.adaptPositionToScreen();
        });
        this.dialog.addClass("panel_bg").addChildren(this.colorSelector, this.intensityConfigurator);
    }

    /** The live value, never null. */
    protected HDRColor current() {
        return value == null ? defaultValue : value;
    }

    /** Build a new value from the picker's packed ARGB plus an intensity, honouring {@link #showAlpha}. */
    protected HDRColor withPickerColor(int argb, float intensity) {
        return new HDRColor(ColorUtils.red(argb), ColorUtils.green(argb), ColorUtils.blue(argb),
                showAlpha ? ColorUtils.alpha(argb) : 1f, intensity);
    }

    @Override
    protected void onValueUpdatePassively(HDRColor newValue) {
        if (newValue == null) newValue = defaultValue;
        if (newValue.equals(value)) return;
        super.onValueUpdatePassively(newValue);
        this.colorSelector.setColor(newValue.baseARGB(), false);
        this.intensityConfigurator.onValueUpdatePassively(newValue.getIntensity());
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

    protected void drawColorPreview(GUIContext context, float x, float y, float width, float height) {
        var hdr = current();
        var color = hdr.toARGB();
        DrawerHelperClient.drawSolidRect(context, x, y, width, height, color);
        DrawerHelperClient.drawSolidRect(context, x - 1, y, 1, height, color);
        DrawerHelperClient.drawSolidRect(context, x + width, y, 1, height, color);
        DrawerHelperClient.drawSolidRect(context, x, y - 1, width, 1, color);
        DrawerHelperClient.drawSolidRect(context, x, y + height, width, 1, color);
        if (labelIntensity != hdr.getIntensity()) {
            labelIntensity = hdr.getIntensity();
            intensityLabel.updateText("HDR: %.1f".formatted(labelIntensity)); // re-splits at the current width
        }
        if (labelWidth != (int) width) {
            labelWidth = (int) width;
            intensityLabel.setWidth(labelWidth);
        }
        context.drawTexture(intensityLabel, x, y + 1, width, height);
    }
}
