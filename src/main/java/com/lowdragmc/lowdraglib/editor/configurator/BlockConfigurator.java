package com.lowdragmc.lowdraglib.editor.configurator;

import com.lowdragmc.lowdraglib.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.SearchComponentWidget;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BlockConfigurator extends ValueConfigurator<Block> implements SearchComponentWidget.IWidgetSearch<Block> {
    protected SearchComponentWidget<Block> searchComponent;
    protected ImageWidget image;

    public BlockConfigurator(String name, Supplier<Block> supplier, Consumer<Block> onUpdate, @Nonnull Block defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        if (value == null) {
            value = defaultValue;
        }
    }

    @Override
    protected void onValueUpdate(Block newValue) {
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
                o -> o instanceof Block || o instanceof BlockState,
                o -> image.setImage(ColorPattern.GREEN.rectTexture().setRadius(5)),
                o -> image.setImage(ColorPattern.T_GRAY.rectTexture().setRadius(5)),
                o -> {
                    if (o instanceof BlockState state) {
                        onValueUpdate(state.getBlock());
                        updateValue();
                    } else if (o instanceof Block block) {
                        onValueUpdate(block);
                        updateValue();
                    }
                    image.setImage(ColorPattern.T_GRAY.rectTexture().setRadius(5));
                });
        addWidget(searchComponent = new SearchComponentWidget<>(leftWidth + 3, 2, width - leftWidth - 6 - rightWidth, 10, this));
        searchComponent.setIconProvider(block -> new ItemStackTexture(block.asItem()));
        searchComponent.setShowUp(true);
        searchComponent.setCapacity(5);
        searchComponent.setCurrent(value);
        var textFieldWidget = searchComponent.textFieldWidget;
        textFieldWidget.setClientSideWidget();
        textFieldWidget.setBordered(false);
    }


    @Override
    public String resultDisplay(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    @Override
    public void selectResult(Block block) {
        onValueUpdate(block);
        updateValue();
    }

    @Override
    public void search(String word, Consumer<Block> find) {
        var wordLower = word.toLowerCase();
        for (var blockEntry : BuiltInRegistries.BLOCK.entrySet()) {
            if (Thread.currentThread().isInterrupted()) return;
            var block = blockEntry.getValue();
            var id = blockEntry.getKey().location();
            if (id.toString().contains(wordLower)) {
                find.accept(block);
            } else {
                var name = LocalizationUtils.format(block.getDescriptionId());
                if (name.toLowerCase().contains(wordLower)) {
                    find.accept(block);
                }
            }
        }
    }
}
