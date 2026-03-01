package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Tooltips;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.layout.LayoutProperties;
import com.lowdragmc.lowdraglib2.gui.ui.style.animation.Transition;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaOverflow;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@RemapPrefixForJS("kjs$")
@Configurable(name = "BasicStyle")
public class BasicStyle extends Style {
    private final static Property<?>[] PROPERTIES = {
            PropertyRegistry.BACKGROUND,
            PropertyRegistry.OVERLAY,
            PropertyRegistry.TOOLTIPS,
            PropertyRegistry.Z_INDEX,
            PropertyRegistry.OPACITY,
            PropertyRegistry.OVERFLOW_CLIP,
            PropertyRegistry.TRANSFORM_2D,
            PropertyRegistry.TRANSITION,
            PropertyRegistry.COLOR,
    };

    public BasicStyle(UIElement holder) {
        super(holder);
    }

    public static void init() {
        PropertyRegistry.Z_INDEX.addListener(BasicStyle::onPropertyChanged);
        PropertyRegistry.TRANSFORM_2D.addListener(BasicStyle::onPropertyChanged);
    }

    private static <T> void onPropertyChanged(UIElement element, Property<T> property, @Nullable T oldValue, @Nullable T newValue) {
        if (property == PropertyRegistry.Z_INDEX) {
            if (element.getParent() != null) {
                element.getParent().clearSortedChildrenCache();
            }
        }
        if (property == PropertyRegistry.TRANSFORM_2D) {
            element.clearPoseCache();
        }
    }

    @Override
    protected Property<?>[] getProperties() {
        return PROPERTIES;
    }

    public IGuiTexture backgroundTexture() {
        return getValueSave(PropertyRegistry.BACKGROUND);
    }

    public BasicStyle backgroundTexture(IGuiTexture backgroundTexture) {
        set(PropertyRegistry.BACKGROUND, backgroundTexture);
        return this;
    }

    public BasicStyle background(IGuiTexture backgroundTexture) {
        return backgroundTexture(backgroundTexture);
    }

    public IGuiTexture overlayTexture() {
        return getValueSave(PropertyRegistry.OVERLAY);
    }

    public BasicStyle overlayTexture(IGuiTexture backgroundTexture) {
        set(PropertyRegistry.OVERLAY, backgroundTexture);
        return this;
    }

    public BasicStyle overlay(IGuiTexture backgroundTexture) {
        return overlayTexture(backgroundTexture);
    }

    public Tooltips tooltips() {
        return getValueSave(PropertyRegistry.TOOLTIPS);
    }

    public BasicStyle tooltips(Tooltips tooltips) {
        set(PropertyRegistry.TOOLTIPS, tooltips);
        return this;
    }

    @HideFromJS
    public BasicStyle tooltips(Component... tooltips) {
        tooltips(Tooltips.of(tooltips));
        return this;
    }

    @HideFromJS
    public BasicStyle tooltips(String... tooltips) {
        tooltips(Tooltips.of(tooltips));
        return this;
    }

    public BasicStyle kjs$tooltips(Component... tooltips) {
        return tooltips(tooltips);
    }

    public BasicStyle appendTooltips(Component... tooltips) {
        tooltips(tooltips().append(tooltips));
        return this;
    }

    public BasicStyle appendTooltipsString(String... tooltips) {
        tooltips(tooltips().append(Arrays.stream(tooltips).map(Component::translatable).toArray(Component[]::new)));
        return this;
    }

    public int zIndex() {
        return getValueSave(PropertyRegistry.Z_INDEX);
    }

    public BasicStyle zIndex(int zIndex) {
        set(PropertyRegistry.Z_INDEX, zIndex);
        return this;
    }

    public float opacity() {
        return getValueSave(PropertyRegistry.OPACITY);
    }

    public BasicStyle opacity(float opacity) {
        set(PropertyRegistry.OPACITY, opacity);
        return this;
    }

    public IGuiTexture overflowClip() {
        return getValueSave(PropertyRegistry.OVERFLOW_CLIP);
    }

    public BasicStyle overflowClip(IGuiTexture overflowClip) {
        set(PropertyRegistry.OVERFLOW_CLIP, overflowClip);
        return this;
    }

    public Transform2D transform2D() {
        return getValueSave(PropertyRegistry.TRANSFORM_2D);
    }

    public BasicStyle transform2D(Transform2D transform2D) {
        set(PropertyRegistry.TRANSFORM_2D, transform2D);
        return this;
    }

    public Transition transition() {
        return getValueSave(PropertyRegistry.TRANSITION);
    }

    public BasicStyle transition(Transition transition) {
        set(PropertyRegistry.TRANSITION, transition);
        return this;
    }

    /**
     * Returns the tint color (ARGB) applied multiplicatively to background and overlay textures.
     * -1 (0xFFFFFFFF) means no tint. Can be animated via CSS transitions.
     */
    public int color() {
        return getValueSave(PropertyRegistry.COLOR);
    }

    public BasicStyle color(int color) {
        set(PropertyRegistry.COLOR, color);
        return this;
    }

    public boolean overflowVisible() {
        return getValueSave(LayoutProperties.OVERFLOW) == YogaOverflow.VISIBLE;
    }

    public BasicStyle overflowVisible(boolean transition) {
        set(LayoutProperties.OVERFLOW, transition ? YogaOverflow.VISIBLE : YogaOverflow.HIDDEN);
        return this;
    }
}
