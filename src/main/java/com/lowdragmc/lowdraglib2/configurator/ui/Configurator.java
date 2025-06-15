package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.editor_outdated.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
import net.minecraft.MethodsReturnNonnullByDefault;
import org.appliedenergistics.yoga.*;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Configurator extends UIElement {
    /**
     * The {@code configurator.change} is sent when a change is made by a configurator.
     * The {@link UIEvent#target} refers to the {@link Configurator} that triggered the change.
     */
    public static final String CHANGE_EVENT = "configurator.change";
    public final UIElement lineContainer;
    public final Label label;
    public final UIElement inlineContainer;
    public final UIElement tip;

    public Configurator() {
        this("");
    }

    public Configurator(String name) {
        this.lineContainer = new UIElement();
        this.label = new Label();
        this.inlineContainer = new UIElement();
        this.tip = new UIElement();

        getLayout().setGap(YogaGutter.ALL, 1);

        addChild(this.lineContainer.layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setGap(YogaGutter.ALL, 2);
        }).addChildren(
                this.label.textStyle(textStyle -> textStyle.adaptiveWidth(true).textAlignVertical(Vertical.CENTER)).setText(name).layout(layout -> {
                    layout.setHeight(14);
                }),
                this.inlineContainer.layout(layout -> layout.setFlex(1)),
                this.tip.layout(layout -> {
                    layout.setWidth(14);
                    layout.setHeight(14);
                }).style(style -> style.backgroundTexture(Icons.HELP))));
        if (name.isEmpty()) {
            this.label.setDisplay(YogaDisplay.NONE);
        }
        this.tip.setDisplay(YogaDisplay.NONE);
    }

    public Configurator setLabel(String name) {
        this.label.setText(name);
        this.label.setDisplay(name.isEmpty() ? YogaDisplay.NONE : YogaDisplay.FLEX);
        return this;
    }

    public Configurator setTips(String... tips) {
        this.tip.style(style -> style.appendTooltipsString(tips));
        this.tip.setDisplay(tips.length > 0 ? YogaDisplay.FLEX : YogaDisplay.NONE);
        return this;
    }

    public Configurator addInlineChild(UIElement child) {
        this.inlineContainer.addChild(child);
        return this;
    }

    public Configurator addInlineChildren(UIElement... children) {
        this.inlineContainer.addChildren(children);
        return this;
    }

    public Configurator addInlineChildAt(UIElement child, int index) {
        this.inlineContainer.addChildAt(child, index);
        return this;
    }

    @Override
    public Configurator addChildAt(@Nullable UIElement child, int index) {
        return (Configurator) super.addChildAt(child, index);
    }

    @Override
    public Configurator addChild(@Nullable UIElement child) {
        return (Configurator) super.addChild(child);
    }

    @Override
    public Configurator addChildren(UIElement... children) {
        return (Configurator) super.addChildren(children);
    }

    public final void notifyChanges() {
        notifyChanges(this);
    }

    public void notifyChanges(Configurator source) {
        var event = UIEvent.create(CHANGE_EVENT);
        event.target = source;
        UIEventDispatcher.dispatchEvent(event);
    }

}
