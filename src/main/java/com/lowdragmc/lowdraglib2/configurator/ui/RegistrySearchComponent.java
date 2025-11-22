package com.lowdragmc.lowdraglib2.configurator.ui;

import com.google.common.base.Predicates;
import com.lowdragmc.lowdraglib2.gui.texture.FluidStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RegistrySearchComponent<T> extends SearchComponentConfigurator<T> {
    public final Registry<T> registry;
    @Setter @Accessors(chain = true)
    private Predicate<T> filter = Predicates.alwaysTrue();

    public RegistrySearchComponent(String name, Supplier<T> supplier, Consumer<T> onUpdate,T defaultValue, boolean forceUpdate, Registry<T> registry, UIElementProvider<T> uiProvider) {
        super(name, supplier, onUpdate, new SearchComponentConfigurator.ISearchConfigurator<>() {
            @Override
            public T defaultValue() {
                return defaultValue;
            }

            @Override
            public void search(String word, IResultHandler<T> searchHandler) {}

            @Override
            public String resultText(T value) {
                return Optional.ofNullable(registry.getKey(value)).map(Objects::toString).orElse("unknown");
            }

            @Override
            public UIElementProvider<T> candidateUIProvider() {
                return uiProvider;
            }
        }, forceUpdate);
        this.registry = registry;
    }

    @Override
    public void search(String word, IResultHandler<T> searchHandler) {
        var lowerWord = word.toLowerCase();
        for (var key : registry.keySet()) {
            if (Thread.currentThread().isInterrupted()) return;
            if (!filter.test(registry.get(key))) continue;
            if (key.toString().toLowerCase().contains(lowerWord)) {
                searchHandler.acceptResult(registry.get(key));
            }
        }
    }

    public static class Item extends RegistrySearchComponent<net.minecraft.world.item.Item> {
        public Item(String name, Supplier<net.minecraft.world.item.Item> supplier, Consumer<net.minecraft.world.item.Item> onUpdate, net.minecraft.world.item.Item defaultValue, boolean forceUpdate) {
            super(name, supplier, onUpdate, defaultValue, forceUpdate, BuiltInRegistries.ITEM, UIElementProvider.iconText(
                    item -> new ItemStackTexture(item.asItem()),
                    item -> Component.translatable(item.getDescriptionId())
            ));
        }
    }

    public static class Block extends RegistrySearchComponent<net.minecraft.world.level.block.Block> {
        public Block(String name, Supplier<net.minecraft.world.level.block.Block> supplier, Consumer<net.minecraft.world.level.block.Block> onUpdate, net.minecraft.world.level.block.Block defaultValue, boolean forceUpdate) {
            super(name, supplier, onUpdate, defaultValue, forceUpdate, BuiltInRegistries.BLOCK, UIElementProvider.iconText(
                    block -> new ItemStackTexture(block.asItem()),
                    block -> Component.translatable(block.getDescriptionId())
            ));
        }
    }

    public static class Fluid extends RegistrySearchComponent<net.minecraft.world.level.material.Fluid> {
        public Fluid(String name, Supplier<net.minecraft.world.level.material.Fluid> supplier, Consumer<net.minecraft.world.level.material.Fluid> onUpdate, net.minecraft.world.level.material.Fluid defaultValue, boolean forceUpdate) {
            super(name, supplier, onUpdate, defaultValue, forceUpdate, BuiltInRegistries.FLUID, UIElementProvider.iconText(
                    fluid -> {
                        var bucket = fluid.getBucket();
                        if (bucket != Items.AIR) return new ItemStackTexture(bucket);
                        if (fluid == Fluids.EMPTY) return IGuiTexture.EMPTY;
                        return new FluidStackTexture(fluid);
                    },
                    fluid -> Component.translatable(fluid.getFluidType().getDescriptionId())
            ));
            setFilter(fluid -> fluid != Fluids.EMPTY && fluid.isSource(fluid.defaultFluidState()));
        }
    }

}
