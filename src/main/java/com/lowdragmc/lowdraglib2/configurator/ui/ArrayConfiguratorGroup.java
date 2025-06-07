package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import lombok.Getter;
import lombok.Setter;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaDisplay;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/2
 * @implNote ArrayConfigurator
 */
public class ArrayConfiguratorGroup<T> extends ConfiguratorGroup {
    public final UIElement buttonGroup;
    public final Button addButton;
    public final Button removeButton;
    public final Supplier<List<T>> source;
    public final BiFunction<Supplier<T>, Consumer<T>, Configurator> configuratorProvider;
    protected Supplier<T> addDefault;
    @Setter
    protected Consumer<List<T>> onUpdate;
    protected Consumer<T> onAdd, onRemove;
    @Setter
    protected BiConsumer<Integer, T> onReorder;
    @Setter
    protected boolean forceUpdate;
    protected boolean canRemove = true, canAdd = true, canReorder = true;
    @Getter
    @Nullable
    protected ItemConfigurator selected;

    public ArrayConfiguratorGroup(String name, boolean isCollapse, Supplier<List<T>> source,
                                  BiFunction<Supplier<T>, Consumer<T>, Configurator> configuratorProvider,
                                  boolean forceUpdate) {
        super(name, isCollapse);
        this.buttonGroup = new UIElement();
        this.addButton = new Button();
        this.removeButton = new Button();

        this.configuratorProvider = configuratorProvider;
        this.source = source;
        this.forceUpdate = forceUpdate;
        for (T object : source.get()) {
            addConfigurators(new ItemConfigurator(object, configuratorProvider));
        }

        buttonGroup.layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setAlignSelf(YogaAlign.FLEX_END);
            layout.setPadding(YogaEdge.ALL, 3f);
        }).setDisplay(isCollapse ? YogaDisplay.NONE : YogaDisplay.FLEX)
                .style(style -> style.backgroundTexture(Sprites.BORDER_RT1));

        addButton.setOnClick(this::onAdd).setText("+").textStyle(textStyle -> textStyle.textShadow(false)).layout(layout -> {
            layout.setWidth(12);
            layout.setHeight(12);
        }).setDisplay(YogaDisplay.NONE);
        removeButton.setOnClick(this::onRemove).setText("-").textStyle(textStyle -> textStyle.textColor(ColorPattern.GRAY.color).textShadow(false)
        ).layout(layout -> {
            layout.setWidth(12);
            layout.setHeight(12);
        }).setActive(false);

        addChild(buttonGroup.addChildren(addButton, removeButton));
    }

    protected void onRemove(UIEvent event) {
        if (selected != null && canRemove) {
            if (onRemove != null) {
                onRemove.accept(selected.object);
            }
            removeConfigurator(selected);
            setSelected(null);
            notifyListUpdate();
        }
    }

    protected void onAdd(UIEvent event) {
        if (addDefault != null && canAdd) {
            T object = addDefault.get();
            if (onAdd != null) {
                onAdd.accept(object);
            }
            ItemConfigurator configurator = new ItemConfigurator(object, configuratorProvider);
            if (selected != null) {
                var items = configurators.stream()
                        .filter(ItemConfigurator.class::isInstance)
                        .map(ItemConfigurator.class::cast)
                        .toList();
                var index = items.indexOf(selected);
                addConfigurator(configurator, index + 1);
            } else {
                addConfigurator(configurator);
            }
            notifyListUpdate();
        }
    }

    public ArrayConfiguratorGroup<T> setAddDefault(Supplier<T> addDefault) {
        this.addDefault = addDefault;
        addButton.setDisplay((addDefault != null && canAdd) ? YogaDisplay.FLEX : YogaDisplay.NONE);
        return this;
    }

    public ArrayConfiguratorGroup<T> setCanAdd(boolean canAdd) {
        this.canAdd = canAdd;
        addButton.setDisplay((addDefault != null && canAdd) ? YogaDisplay.FLEX : YogaDisplay.NONE);
        return this;
    }

    public ArrayConfiguratorGroup<T> setCanRemove(boolean canRemove) {
        this.canRemove = canRemove;
        removeButton.setDisplay(canRemove ? YogaDisplay.FLEX : YogaDisplay.NONE);
        return this;
    }

    @Override
    public ArrayConfiguratorGroup<T> setCollapse(boolean collapse) {
        super.setCollapse(collapse);
        if (buttonGroup != null) {
            buttonGroup.setDisplay(collapse ? YogaDisplay.NONE : YogaDisplay.FLEX);
        }
        return this;
    }

    public void notifyListUpdate() {
        if (onUpdate != null) {
            onUpdate.accept(configurators.stream()
                    .filter(ItemConfigurator.class::isInstance)
                    .map(ItemConfigurator.class::cast)
                    .map(c -> (T) c.object)
                    .toList());
        }
        notifyChanges();
    }

    public void setSelected(@Nullable ItemConfigurator selected) {
        if (this.selected == selected) {
            return;
        }
        if (this.selected != null) {
            this.selected.setSelected(false);
        }
        this.selected = selected;
        if (selected != null) {
            selected.setSelected(true);
        }
        removeButton.textStyle(textStyle -> {
            textStyle.textColor(selected != null ? ColorPattern.WHITE.color : ColorPattern.GRAY.color);
        }).setActive(this.selected != null);
    }

    public class ItemConfigurator extends Configurator {
        public T object;
        public Configurator inner;

        public ItemConfigurator(T object, BiFunction<Supplier<T>, Consumer<T>, Configurator> provider) {
            super("=");
            getLayout().setPadding(YogaEdge.LEFT, 2f);
            this.object = object;
            inner = provider.apply(this::getter, this::setter);
            inlineContainer.addChild(inner);
            this.addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown, true);
            this.label.addEventListener(UIEvents.MOUSE_DOWN, this::onLabelMouseDown);
            this.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onDragSourceUpdate);
        }

        private void onDragSourceUpdate(UIEvent event) {
            var items = configurators.stream()
                    .filter(ItemConfigurator.class::isInstance)
                    .map(ItemConfigurator.class::cast)
                    .toList();
            ItemConfigurator after = null;
            for (var configurator : items) {
                if (configurator == this) continue;
                if (configurator.getPositionY() + configurator.getSizeHeight() / 2 < event.y) {
                    after = configurator;
                } else {
                    break;
                }
            }
            var selfIndex = configurators.indexOf(this);
            if (after != null) {
                var index = configurators.indexOf(after);
                if (index + 1 == selfIndex){
                    // do nothing
                } else {
                    removeConfigurator(this);
                    if (index < selfIndex) {
                        addConfigurator(this, index + 1);
                        if (onReorder != null) {
                            onReorder.accept(index + 1, object);
                        }
                        notifyListUpdate();
                    } else {
                        addConfigurator(this, index);
                        if (onReorder != null) {
                            onReorder.accept(index, object);
                        }
                        notifyListUpdate();
                    }
                }
            } else if (selfIndex != 0) {
                removeConfigurator(this);
                addConfigurator(this, 0);
                if (onReorder != null) {
                    onReorder.accept(0, object);
                }
                notifyListUpdate();
            }
        }

        private void onLabelMouseDown(UIEvent event) {
            // prepare for drag
            if (canReorder) {
                startDrag(null, null);
            }
        }

        private void onMouseDown(UIEvent event) {
            // select this configurator
            ArrayConfiguratorGroup.this.setSelected(this);
        }

        private void setter(T t) {
            object = t;
            notifyListUpdate();
        }

        private T getter() {
            return object;
        }

        private void setSelected(boolean selected) {
            this.style(style -> style.backgroundTexture(selected ? Sprites.RECT_DARK : IGuiTexture.EMPTY));
        }
    }

}
