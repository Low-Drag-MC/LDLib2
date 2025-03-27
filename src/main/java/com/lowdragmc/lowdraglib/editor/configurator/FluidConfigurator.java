package com.lowdragmc.lowdraglib.editor.configurator;

import com.lowdragmc.lowdraglib.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.SearchComponentWidget;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FluidConfigurator extends ValueConfigurator<Fluid> implements SearchComponentWidget.IWidgetSearch<Fluid> {
    protected SearchComponentWidget<Fluid> searchComponent;
    protected ImageWidget image;

    public FluidConfigurator(String name, Supplier<Fluid> supplier, Consumer<Fluid> onUpdate, @Nonnull Fluid defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        if (value == null) {
            value = defaultValue;
        }
    }

    @Override
    protected void onValueUpdate(Fluid newValue) {
        if (newValue == null) newValue = defaultValue;
        if (value == newValue) return;
        super.onValueUpdate(newValue);
        searchComponent.setCurrentString(BuiltInRegistries.FLUID.getKey(value == null ? defaultValue : value).toString());
    }

    @Override
    public void init(int width) {
        super.init(width);
        addWidget(image = new ImageWidget(leftWidth, 2, width - leftWidth - 3 - rightWidth, 10, ColorPattern.T_GRAY.rectTexture().setRadius(5)));
        image.setDraggingConsumer(
                o -> o instanceof Fluid || o instanceof FluidStack,
                o -> image.setImage(ColorPattern.GREEN.rectTexture().setRadius(5)),
                o -> image.setImage(ColorPattern.T_GRAY.rectTexture().setRadius(5)),
                o -> {
                    if (o instanceof FluidStack fluidStack) {
                        onValueUpdate(fluidStack.getFluid());
                        updateValue();
                    } else if (o instanceof Fluid fluid) {
                        onValueUpdate(fluid);
                        updateValue();
                    }
                    image.setImage(ColorPattern.T_GRAY.rectTexture().setRadius(5));
                });
        addWidget(searchComponent = new SearchComponentWidget<>(leftWidth + 3, 2, width - leftWidth - 6 - rightWidth, 10, this));
        searchComponent.setShowUp(true);
        searchComponent.setCapacity(5);
        var textFieldWidget = searchComponent.textFieldWidget;
        textFieldWidget.setClientSideWidget();
        textFieldWidget.setCurrentString(value == null ? BuiltInRegistries.FLUID.getKey(defaultValue) : BuiltInRegistries.FLUID.getKey(value));
        textFieldWidget.setBordered(false);
    }


    @Override
    public String resultDisplay(Fluid fluid) {
        return BuiltInRegistries.FLUID.getKey(fluid).toString();
    }

    @Override
    public void selectResult(Fluid value) {
        onValueUpdate(value);
        updateValue();
    }

    @Override
    public void search(String word, Consumer<Fluid> find) {
        var wordLower = word.toLowerCase();
        for (var entry : BuiltInRegistries.FLUID.entrySet()) {
            if (Thread.currentThread().isInterrupted()) return;
            var fluid = entry.getValue();
            var id = entry.getKey().location();
            if (id.toString().contains(wordLower)) {
                find.accept(fluid);
            }
        }
    }
}
