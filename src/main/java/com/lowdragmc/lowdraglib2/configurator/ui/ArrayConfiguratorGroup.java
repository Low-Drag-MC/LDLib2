package com.lowdragmc.lowdraglib2.configurator.ui;

import com.google.common.base.Predicates;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import org.appliedenergistics.yoga.YogaEdge;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * @author KilaBash
 * @date 2022/12/2
 * @implNote ArrayConfigurator
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ArrayConfiguratorGroup<T> extends ConfiguratorGroup {
    @FunctionalInterface
    public interface IConfiguratorProvider<T> extends BiFunction<Supplier<T>, Consumer<T>, Configurator> {
        Configurator getConfigurator(Supplier<T> getter, Consumer<T> setter);

        @Override
        @Deprecated
        default Configurator apply(Supplier<T> tSupplier, Consumer<T> tConsumer) {
            return getConfigurator(tSupplier, tConsumer);
        }
    }

    @FunctionalInterface
    public interface IAddDefault<T> extends Supplier<T> { }

    public final UIElement buttonGroup;
    public final Button addButton;
    public final Button removeButton;
    public final Supplier<List<T>> source;
    public final IConfiguratorProvider<T> configuratorProvider;
    protected IAddDefault<T> addDefault;
    @Setter @Accessors(chain = true)
    protected Consumer<List<T>> onUpdate;
    protected Consumer<T> onAdd, onRemove;
    @Setter
    protected BiConsumer<Integer, T> onReorder;
    @Setter
    protected boolean forceUpdate;
    protected boolean canAdd = true, canReorder = true;
    protected Predicate<T> canRemove = Predicates.alwaysTrue();
    @Getter
    @Nullable
    protected ItemConfigurator selected;
    @Setter @Accessors(chain = true)
    @Nullable
    protected Consumer<T> onSelectedChanged;

    public ArrayConfiguratorGroup(String name, boolean isCollapse, Supplier<List<T>> source,
                                  IConfiguratorProvider<T> configuratorProvider,
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
            layout.flexDirection(FlexDirection.ROW);
            layout.alignSelf(AlignItems.FLEX_END);
            layout.paddingAll(3f);
        }).setDisplay(!isCollapse)
                .style(style -> style.backgroundTexture(Sprites.BORDER_RT1))
                .moveInlineAsDefault()
                .addClass("__array-configurator_button-group__");

        addButton.setOnClick(this::onAdd).setText("+").textStyle(textStyle -> textStyle.textShadow(false)).layout(layout -> {
            layout.width(12);
            layout.height(12);
        }).setDisplay(false);
        removeButton.setOnClick(this::onRemove).setText("-").textStyle(textStyle -> textStyle.textColor(ColorPattern.WHITE.color).textShadow(false)
        ).layout(layout -> {
            layout.width(12);
            layout.height(12);
        }).setActive(false);
        removeButton.setDisplay(false);

        addChild(buttonGroup.addChildren(addButton, removeButton));
    }

    @Override
    public void screenTick() {
        super.screenTick();
        if (forceUpdate) {
            var current = source.get();
            var items = configurators.stream().filter(ItemConfigurator.class::isInstance)
                    .map(ItemConfigurator.class::cast)
                    .collect(Collectors.toList());
            // remove overflow
            while (items.size() > current.size()) {
                removeConfigurator(items.getLast());
                if (selected == items.removeLast()) {
                    setSelected(null);
                }
            }
            // check items
            for (int i = 0; i < current.size(); i++) {
                var expected = current.get(i);
                if (items.size() > i) {
                    var item = items.get(i);
                    if (!Objects.equals(item.object, expected)) {
                        var index = configurators.indexOf(item);
                        removeConfigurator(item);
                        var newItem = new ItemConfigurator(expected, configuratorProvider);
                        addConfiguratorAt(newItem, index);
                        if (selected == item) {
                            setSelected(newItem);
                        }
                    }
                } else {
                    addConfigurator(new ItemConfigurator(expected, configuratorProvider));
                }
            }
        }
    }

    protected void onRemove(UIEvent event) {
        if (selected != null && canRemove.test(selected.object)) {
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
                addConfiguratorAt(configurator, index + 1);
            } else {
                addConfigurator(configurator);
            }
            notifyListUpdate();
        }
    }

    public ArrayConfiguratorGroup<T> setAddDefault(@Nullable IAddDefault<T> addDefault) {
        this.addDefault = addDefault;
        addButton.setDisplay((addDefault != null && canAdd));
        return this;
    }

    public ArrayConfiguratorGroup<T> setCanAdd(boolean canAdd) {
        this.canAdd = canAdd;
        addButton.setDisplay((addDefault != null && canAdd));
        return this;
    }

    public ArrayConfiguratorGroup<T> setCanRemove(boolean canRemove) {
        return setCanRemove(canRemove ? Predicates.alwaysTrue() : Predicates.alwaysFalse());
    }

    public ArrayConfiguratorGroup<T> setCanRemove(Predicate<T> canRemove) {
        this.canRemove = canRemove;
        return this;
    }

    public ArrayConfiguratorGroup<T> setCanReorder(boolean canReorder) {
        this.canReorder = canReorder;
        return this;
    }

    @Override
    public ArrayConfiguratorGroup<T> setCollapse(boolean collapse) {
        super.setCollapse(collapse);
        if (buttonGroup != null) {
            buttonGroup.setDisplay(!collapse);
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
        if (onSelectedChanged != null) {
            onSelectedChanged.accept(selected == null ? null : selected.object);
        }
        removeButton.setActive(this.selected != null);
        removeButton.setDisplay((this.selected != null && canRemove.test(this.selected.object)));
    }

    public class ItemConfigurator extends Configurator {
        public T object;
        public Configurator inner;

        public ItemConfigurator(T object, BiFunction<Supplier<T>, Consumer<T>, Configurator> provider) {
            super("=");
            label.layout(layout -> {
                layout.marginLeft(1f);
                layout.alignSelf(AlignItems.CENTER);
            }).style(style -> style.tooltips("ldlib.gui.editor.tips.drag_item"));
            getLayout().paddingLeft(2f);
            this.object = object;
            inner = provider.apply(this::getter, this::setter);
            inlineContainer.addChild(inner);
            this.addEventListener(UIEvents.MOUSE_DOWN, this::onItemMouseDown, true);
            this.label.addEventListener(UIEvents.MOUSE_DOWN, this::onLabelMouseDown);
            this.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onDragSourceUpdate);
            addClass("__array-configurator_item_unselected__");
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
                        addConfiguratorAt(this, index + 1);
                        if (onReorder != null) {
                            onReorder.accept(index + 1, object);
                        }
                        notifyListUpdate();
                    } else {
                        addConfiguratorAt(this, index);
                        if (onReorder != null) {
                            onReorder.accept(index, object);
                        }
                        notifyListUpdate();
                    }
                }
            } else if (selfIndex != 0) {
                removeConfigurator(this);
                addConfiguratorAt(this, 0);
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

        private void onItemMouseDown(UIEvent event) {
            if (event.button == 0) {
                // select this configurator
                ArrayConfiguratorGroup.this.setSelected(this);
            }
        }

        private void setter(T t) {
            object = t;
            notifyListUpdate();
        }

        private T getter() {
            return object;
        }

        private void setSelected(boolean selected) {
            this.style(style -> {
                style.setPipelineState(StyleOrigin.DEFAULT);
                style.backgroundTexture(selected ? Sprites.RECT_DARK : IGuiTexture.EMPTY);
                style.setPipelineState(StyleOrigin.INLINE);
            });
            if (selected) {
                addClass("__array-configurator_item_selected__");
                removeClass("__array-configurator_item_unselected__");
            } else {
                addClass("__array-configurator_item_unselected__");
                removeClass("__array-configurator_item_selected__");
            }
        }
    }

}
