package com.lowdragmc.lowdraglib2.gui.ui.utils;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.TextWrap;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaGutter;
import org.appliedenergistics.yoga.YogaOverflow;

import java.util.function.Function;

@FunctionalInterface
public interface UIElementProvider<T> extends Function<T, UIElement> {

    UIElement createUI(T value);

    @Override
    default UIElement apply(T value) {
        return createUI(value);
    }

    static <T> UIElementProvider<T> text(Function<T, Component> textMapper) {
        return candidate -> new Label()
                .textStyle(style -> style
                        .textWrap(TextWrap.HOVER_ROLL)
                        .textAlignHorizontal(Horizontal.LEFT)
                        .textAlignVertical(Vertical.CENTER))
                .setText(textMapper.apply(candidate))
                .setOverflow(YogaOverflow.HIDDEN);
    }

    static <T> UIElementProvider<T> iconText(
            Function<T, IGuiTexture> iconMapper,
            Function<T, Component> textMapper) {
        return node -> {
            var container = new UIElement().layout(layout -> {
                layout.setFlexDirection(YogaFlexDirection.ROW);
                layout.setGap(YogaGutter.ALL, 2);
                layout.setHeight(10);
            }).addChildren();
            var icon = new UIElement().layout(layout -> {
                layout.setAspectRatio(1);
                layout.setHeightPercent(100);
            }).style(style -> style.backgroundTexture(iconMapper.apply(node)));
            var label = new TextElement()
                    .textStyle(style -> style.textWrap(TextWrap.HOVER_ROLL).textAlignVertical(Vertical.CENTER))
                    .setText(textMapper.apply(node)).layout(layout -> {
                        layout.setHeightPercent(100);
                        layout.setFlex(1);
                    }).setOverflow(YogaOverflow.HIDDEN);
            return container.addChildren(icon, label);
        };
    }
}
