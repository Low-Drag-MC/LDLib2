package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import org.appliedenergistics.yoga.*;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ToggleSelectorConfigurator<T> extends ValueConfigurator<T> {
    public final List<T> candidates;
    public final List<Toggle> toggles;
    public final Toggle.ToggleGroup group;

    public ToggleSelectorConfigurator(String name,
                                      Supplier<@Nullable T> supplier, Consumer<@Nullable T> onUpdate,
                                      @Nullable T defaultValue,
                                      boolean forceUpdate,
                                      List<T> candidates,
                                      Function<T, String> nameMapping,
                                      Function<T, IGuiTexture> iconProvider) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        this.candidates = candidates;
        this.toggles = new ArrayList<>();
        this.group = new Toggle.ToggleGroup();
        if (value == null) value = defaultValue;
        inlineContainer.layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
            layout.wrap(FlexWrap.WRAP);
        });
        initToggles(nameMapping, iconProvider);
    }

    public ToggleSelectorConfigurator<T> initToggles(Function<T, String> nameMapping,
                                                         Function<T, IGuiTexture> iconProvider) {
        inlineContainer.clearAllChildren();
        toggles.clear();
        for (T candidate : this.candidates) {
            var toggle = new Toggle().noText();
            toggle.layout(layout -> {
                layout.paddingAll(0);
            });
            toggle.setToggleGroup(this.group);
            toggle.setOn(Objects.equals(candidate, value), false);
            toggle.setOnToggleChanged(isOn -> {
                if (isOn) {
                    updateValueActively(candidate);
                }
            });
            toggle.toggleStyle(toggleStyle -> {
                toggleStyle.setPipelineState(StyleOrigin.DEFAULT);
                toggleStyle.baseTexture(Sprites.RECT_SOLID);
                toggleStyle.hoverTexture(new GuiTextureGroup(Sprites.RECT_SOLID, ColorPattern.WHITE.borderTexture(-1)));
                toggleStyle.markTexture(Sprites.RECT_DARK);
                toggleStyle.setPipelineState(StyleOrigin.INLINE);
            });
            toggle.toggleButton.layout(layout -> {
                layout.paddingAll(1);
            });
            toggle.markIcon.layout(layout -> {
                layout.paddingAll(1);
                layout.alignItems(AlignItems.CENTER);
                layout.justifyContent(AlignContent.CENTER);
            });
            toggle.markIcon.addChild(new UIElement().layout(layout -> {
                layout.widthPercent(100);
                layout.heightPercent(100);
            }).style(style -> style.backgroundTexture(iconProvider.apply(candidate))).addClass("__icon__"));
            toggle.style(style -> style.tooltips(nameMapping.apply(candidate)));
            toggles.add(toggle);
        }
        inlineContainer.addChildren(toggles.toArray(new Toggle[0]));
        return this;
    }

    @Override
    protected void onValueUpdatePassively(T newValue) {
        if (newValue == null) newValue = defaultValue;
        if (Objects.equals(newValue, value)) return;
        super.onValueUpdatePassively(newValue);
        for (int i = 0; i < candidates.size(); i++) {
            var toggle = toggles.get(i);
            toggle.setOn(Objects.equals(candidates.get(i), newValue), false);
        }
    }

}
