package com.lowdragmc.lowdraglib.configurator.ui;

import com.lowdragmc.lowdraglib.editor.Icons;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib.gui.ui.elements.Label;
import lombok.Getter;
import org.appliedenergistics.yoga.YogaDisplay;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaGutter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Configurator extends UIElement {
    public final UIElement lineContainer;
    public final Label label;
    public final UIElement inlineContainer;
    public final UIElement tip;
    @Getter
    protected final List<Consumer<Configurator>> listeners = new ArrayList<>();

    public Configurator() {
        this("");
    }

    public Configurator(String name) {
        this.lineContainer = new UIElement();
        this.label = new Label();
        this.inlineContainer = new UIElement();
        this.tip = new UIElement();

        addChild(this.lineContainer.layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setGap(YogaGutter.ALL, 1);
        }).addChildren(
                this.label.textStyle(textStyle -> textStyle.adaptiveWidth(true).textAlignVertical(Vertical.CENTER)).setText(name).layout(layout -> {
                    layout.setHeight(14);
                }),
                this.inlineContainer.layout(layout -> layout.setFlex(1)),
                this.tip.layout(layout -> {
                    layout.setMargin(YogaEdge.TOP, 2f);
                    layout.setWidth(10);
                    layout.setHeight(10);
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

    /**
     * Add a listener to this configurator
     */
    public void addListener(Consumer<Configurator> listener) {
        listeners.add(listener);
    }

    public final void notifyChanges() {
        notifyChanges(this);
    }

    public void notifyChanges(Configurator source) {
        listeners.forEach(listener -> listener.accept(source));
        if (getParent() instanceof Configurator configurator) {
            configurator.notifyChanges(source);
        }
    }

}
