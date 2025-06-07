package com.lowdragmc.lowdraglib2.editor_outdated.configurator;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib2.gui.widget.SearchComponentWidget;
import com.lowdragmc.lowdraglib2.utils.LocalizationUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ItemConfigurator extends ValueConfigurator<Item> implements SearchComponentWidget.IWidgetSearch<Item> {
    protected SearchComponentWidget<Item> searchComponent;
    protected ImageWidget image;

    public ItemConfigurator(String name, Supplier<Item> supplier, Consumer<Item> onUpdate, @Nonnull Item defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        if (value == null) {
            value = defaultValue;
        }
    }

    @Override
    protected void onValueUpdate(Item newValue) {
        if (newValue == null) newValue = defaultValue;
        if (value == newValue) return;
        super.onValueUpdate(newValue);
        searchComponent.setCurrent(value == null ? defaultValue : value);
    }

    @Override
    public void init(int width) {
        super.init(width);
        addWidget(image = new ImageWidget(leftWidth, 2, width - leftWidth - 3 - rightWidth, 10, ColorPattern.T_GRAY.rectTexture().setRadius(5)));
        image.setDraggingConsumer(
                o -> o instanceof ItemLike || o instanceof ItemStack,
                o -> image.setImage(ColorPattern.GREEN.rectTexture().setRadius(5)),
                o -> image.setImage(ColorPattern.T_GRAY.rectTexture().setRadius(5)),
                o -> {
                    if (o instanceof ItemStack itemStack) {
                        onValueUpdate(itemStack.getItem());
                        updateValue();
                    } else if (o instanceof ItemLike item) {
                        onValueUpdate(item.asItem());
                        updateValue();
                    }
                    image.setImage(ColorPattern.T_GRAY.rectTexture().setRadius(5));
                });
        addWidget(searchComponent = new SearchComponentWidget<>(leftWidth + 3, 2, width - leftWidth - 6 - rightWidth, 10, this));
        searchComponent.setIconProvider(item -> new ItemStackTexture(new ItemStack(item)));
        searchComponent.setShowUp(true);
        searchComponent.setCapacity(5);
        searchComponent.setCurrent(value);
        var textFieldWidget = searchComponent.textFieldWidget;
        textFieldWidget.setClientSideWidget();
        textFieldWidget.setBordered(false);
    }


    @Override
    public String resultDisplay(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    @Override
    public void selectResult(Item item) {
        onValueUpdate(item);
        updateValue();
    }

    @Override
    public void search(String word, Consumer<Item> find) {
        var wordLower = word.toLowerCase();
        for (var itemEntry : BuiltInRegistries.ITEM.entrySet()) {
            if (Thread.currentThread().isInterrupted()) return;
            var item = itemEntry.getValue();
            var id = itemEntry.getKey().location();
            if (id.toString().contains(wordLower)) {
                find.accept(item);
            } else {
                var name = LocalizationUtils.format(item.getDescriptionId());
                if (name.toLowerCase().contains(wordLower)) {
                    find.accept(item);
                }
            }
        }
    }
}
