package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import org.appliedenergistics.yoga.*;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ToggleSelectorConfigurator<T> extends ValueConfigurator<T> {
    public final List<T> candidates;
    public final List<Toggle> toggles;
    public final Toggle.ToggleGroup group;

    public ToggleSelectorConfigurator(String name,
                                      Supplier<T> supplier, Consumer<T> onUpdate,
                                      @Nonnull T defaultValue,
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
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setWrap(YogaWrap.WRAP);
        });
        for (T candidate : this.candidates) {
            var toggle = new Toggle().noText();
            toggle.layout(layout -> {
                layout.setPadding(YogaEdge.ALL, 0);
            });
            toggle.setToggleGroup(this.group);
            toggle.setOn(candidate.equals(value), false);
            toggle.setOnToggleChanged(isOn -> {
                if (isOn) {
                    updateValueActively(candidate);
                }
            });
            toggle.toggleStyle(toggleStyle -> {
                toggleStyle.baseTexture(Sprites.RECT_SOLID);
                toggleStyle.hoverTexture(new GuiTextureGroup(Sprites.RECT_SOLID, ColorPattern.WHITE.borderTexture(-1)));
                toggleStyle.markTexture(Sprites.RECT_DARK);
            });
            toggle.toggleButton.layout(layout -> {
                layout.setPadding(YogaEdge.ALL, 1);
            });
            toggle.markIcon.layout(layout -> {
                layout.setPadding(YogaEdge.ALL, 1);
                layout.setAlignItems(YogaAlign.CENTER);
                layout.setJustifyContent(YogaJustify.CENTER);
            });
            toggle.markIcon.addChild(new UIElement().layout(layout -> {
                layout.setWidthPercent(100);
                layout.setHeightPercent(100);
            }).style(style -> style.backgroundTexture(iconProvider.apply(candidate))));
            toggle.style(style -> style.setTooltips(nameMapping.apply(candidate)));
            toggles.add(toggle);
        }
        inlineContainer.addChildren(toggles.toArray(new Toggle[0]));
    }

    @Override
    protected void onValueUpdatePassively(T newValue) {
        if (newValue == null) newValue = defaultValue;
        if (newValue.equals(value)) return;
        super.onValueUpdatePassively(newValue);
        for (int i = 0; i < candidates.size(); i++) {
            var toggle = toggles.get(i);
            toggle.setOn(candidates.get(i).equals(newValue), false);
        }
    }

}
