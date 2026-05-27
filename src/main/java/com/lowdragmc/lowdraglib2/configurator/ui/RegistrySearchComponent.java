package com.lowdragmc.lowdraglib2.configurator.ui;

import com.google.common.base.Predicates;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.texture.FluidStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.integration.xei.emi.LDLibEMIPlugin;
import com.lowdragmc.lowdraglib2.integration.xei.jei.LDLibJEIPlugin;
import com.lowdragmc.lowdraglib2.integration.xei.rei.LDLibREIPlugin;
import com.lowdragmc.lowdraglib2.utils.LocalizationUtils;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import dev.emi.emi.api.stack.FluidEmiStack;
import dev.emi.emi.api.stack.ItemEmiStack;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.neoforge.NeoForgeTypes;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RegistrySearchComponent<T> extends SearchComponentConfigurator<T> {
    public final Registry<T> registry;
    @Setter @Accessors(chain = true)
    protected Predicate<T> filter = Predicates.alwaysTrue();
    @Setter @Accessors(chain = true)
    protected Function<T, String> translator = null;

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
        if (this.registry == null) return;
        var lowerWord = word.toLowerCase();
        for (var key : registry.keySet()) {
            if (Thread.currentThread().isInterrupted()) return;
            var value = registry.get(key);
            if (!filter.test(value)) continue;
            if (key.toString().toLowerCase().contains(lowerWord)) {
                searchHandler.acceptResult(value);
                continue;
            }
            // translate key to translatable component
            if (translator != null && translator.apply(value).toLowerCase().contains(lowerWord)) {
                searchHandler.acceptResult(value);
            }
        }
    }

    public static class Item extends RegistrySearchComponent<net.minecraft.world.item.Item> {
        public Item(String name, Supplier<net.minecraft.world.item.Item> supplier, Consumer<net.minecraft.world.item.Item> onUpdate, net.minecraft.world.item.Item defaultValue, boolean forceUpdate) {
            super(name, supplier, onUpdate, defaultValue, forceUpdate, BuiltInRegistries.ITEM, UIElementProvider.iconText(
                    item -> new ItemStackTexture(item.asItem()),
                    item -> Component.translatable(item.getDescriptionId())
            ));

            if (LDLib2.isJeiLoaded()) {
                RegistrySearchComponent.JEISupport.ghostItem(this, itemStack -> filter.test(itemStack.getItem()),
                        itemStack -> setValue(itemStack.getItem(), true));
            }
            if (LDLib2.isReiLoaded()) {
                RegistrySearchComponent.REISupport.ghostItem(this, itemStack -> filter.test(itemStack.getItem()),
                        itemStack -> setValue(itemStack.getItem(), true));
            }
            if (LDLib2.isEmiLoaded()) {
                RegistrySearchComponent.EMISupport.ghostItem(this, itemStack -> filter.test(itemStack.getItem()),
                        itemStack -> setValue(itemStack.getItem(), true));
            }

            setTranslator(item -> LocalizationUtils.format(item.getDescriptionId()));
        }
    }

    public static class Block extends RegistrySearchComponent<net.minecraft.world.level.block.Block> {
        public Block(String name, Supplier<net.minecraft.world.level.block.Block> supplier, Consumer<net.minecraft.world.level.block.Block> onUpdate, net.minecraft.world.level.block.Block defaultValue, boolean forceUpdate) {
            super(name, supplier, onUpdate, defaultValue, forceUpdate, BuiltInRegistries.BLOCK, UIElementProvider.iconText(
                    block -> new ItemStackTexture(block.asItem()),
                    block -> Component.translatable(block.getDescriptionId())
            ));

            if (LDLib2.isJeiLoaded()) {
                RegistrySearchComponent.JEISupport.ghostBlock(this, block -> filter.test(block),
                        block -> setValue(block, true));
            }
            if (LDLib2.isReiLoaded()) {
                RegistrySearchComponent.REISupport.ghostBlock(this, block -> filter.test(block),
                        block -> setValue(block, true));
            }
            if (LDLib2.isEmiLoaded()) {
                RegistrySearchComponent.EMISupport.ghostBlock(this, block -> filter.test(block),
                        block -> setValue(block, true));
            }

            setTranslator(block -> LocalizationUtils.format(block.getDescriptionId()));
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

            if (LDLib2.isJeiLoaded()) {
                RegistrySearchComponent.JEISupport.ghostFluid(this, fluidStack -> filter.test(fluidStack.getFluid()),
                        fluidStack -> setValue(fluidStack.getFluid(), true));
            }
            if (LDLib2.isReiLoaded()) {
                RegistrySearchComponent.REISupport.ghostFluid(this, fluidStack -> filter.test(fluidStack.getFluid()),
                        fluidStack -> setValue(fluidStack.getFluid(), true));
            }
            if (LDLib2.isEmiLoaded()) {
                RegistrySearchComponent.EMISupport.ghostFluid(this, fluidStack -> filter.test(fluidStack.getFluid()),
                        fluidStack -> setValue(fluidStack.getFluid(), true));
            }

            setTranslator(fluid -> LocalizationUtils.format(fluid.getFluidType().getDescriptionId()));
        }
    }

    public static class EntityType extends RegistrySearchComponent<net.minecraft.world.entity.EntityType<?>> {
        public EntityType(String name, Supplier<net.minecraft.world.entity.EntityType<?>> supplier, Consumer<net.minecraft.world.entity.EntityType<?>> onUpdate, net.minecraft.world.entity.EntityType<?> defaultValue, boolean forceUpdate) {
            super(name, supplier, onUpdate, defaultValue, forceUpdate, BuiltInRegistries.ENTITY_TYPE, UIElementProvider.iconText(
                    entityType -> {
                        var egg = SpawnEggItem.byId(entityType);
                        if (egg != null) return new ItemStackTexture(egg.asItem());
                        return IGuiTexture.EMPTY;
                    },
                    net.minecraft.world.entity.EntityType::getDescription
            ));

            if (LDLib2.isJeiLoaded()) {
                RegistrySearchComponent.JEISupport.ghostItem(this, itemStack ->
                                Optional.ofNullable(getTypeFromEgg(itemStack)).map(filter::test).orElse(false),
                        itemStack -> Optional.ofNullable(getTypeFromEgg(itemStack)).filter(filter)
                                .ifPresent(entityType -> setValue(entityType, true)));
            }
            if (LDLib2.isReiLoaded()) {
                RegistrySearchComponent.REISupport.ghostItem(this, itemStack ->
                                Optional.ofNullable(getTypeFromEgg(itemStack)).map(filter::test).orElse(false),
                        itemStack -> Optional.ofNullable(getTypeFromEgg(itemStack)).filter(filter)
                                .ifPresent(entityType -> setValue(entityType, true)));
            }
            if (LDLib2.isEmiLoaded()) {
                RegistrySearchComponent.EMISupport.ghostItem(this, itemStack ->
                                Optional.ofNullable(getTypeFromEgg(itemStack)).map(filter::test).orElse(false),
                        itemStack -> Optional.ofNullable(getTypeFromEgg(itemStack)).filter(filter)
                                .ifPresent(entityType -> setValue(entityType, true)));
            }

            setTranslator(entityType -> LocalizationUtils.format(entityType.getDescriptionId()));
        }

        @Nullable
        public net.minecraft.world.entity.EntityType<?> getTypeFromEgg(ItemStack itemStack) {
            if (itemStack.getItem() instanceof SpawnEggItem eggItem) {
                return eggItem.getType(itemStack);
            }
            return null;
        }
    }

    public static class JEISupport {
        public static void ghostItem(UIElement element, Predicate<ItemStack> filter, Consumer<ItemStack> setter) {
            LDLibJEIPlugin.ghostIngredient(element, VanillaTypes.ITEM_STACK,
                    ingredient -> filter.test(ingredient.getIngredient()),
                    setter);
        }

        public static void ghostFluid(UIElement element, Predicate<FluidStack> filter, Consumer<FluidStack> setter) {
            LDLibJEIPlugin.ghostIngredient(element, NeoForgeTypes.FLUID_STACK,
                    ingredient -> filter.test(ingredient.getIngredient()),
                    setter);
        }

        public static void ghostBlock(UIElement element, Predicate<net.minecraft.world.level.block.Block> filter, Consumer<net.minecraft.world.level.block.Block> setter) {
            ghostItem(element,itemStack -> {
                if (itemStack.getItem() instanceof BlockItem blockItem) {
                    return filter.test(blockItem.getBlock());
                }
                return false;
            }, itemStack -> {
                if (itemStack.getItem() instanceof BlockItem blockItem) {
                    setter.accept(blockItem.getBlock());
                }
            });
        }
    }

    // region XEI Supports
    public static class REISupport {
        public static void ghostItem(UIElement element, Predicate<ItemStack> filter, Consumer<ItemStack> setter) {
            LDLibREIPlugin.draggableStackBounds(element,
                    VanillaEntryTypes.ITEM,
                    stack -> filter.test(stack.getValue()));
            LDLibREIPlugin.acceptDraggableStack(element,
                    VanillaEntryTypes.ITEM,
                    stack -> filter.test(stack.getValue()),
                    stack -> setter.accept(stack.getValue()));
        }

        public static void ghostFluid(UIElement element, Predicate<FluidStack> filter, Consumer<FluidStack> setter) {
            LDLibREIPlugin.draggableStackBounds(element,
                    VanillaEntryTypes.FLUID,
                    stack -> filter.test(FluidStackHooksForge.toForge(stack.getValue())));
            LDLibREIPlugin.acceptDraggableStack(element,
                    VanillaEntryTypes.FLUID,
                    stack -> filter.test(FluidStackHooksForge.toForge(stack.getValue())),
                    stack -> setter.accept(FluidStackHooksForge.toForge(stack.getValue())));
        }

        public static void ghostBlock(UIElement element, Predicate<net.minecraft.world.level.block.Block> filter, Consumer<net.minecraft.world.level.block.Block> setter) {
            ghostItem(element,itemStack -> {
                if (itemStack.getItem() instanceof BlockItem blockItem) {
                    return filter.test(blockItem.getBlock());
                }
                return false;
            }, itemStack -> {
                if (itemStack.getItem() instanceof BlockItem blockItem) {
                    setter.accept(blockItem.getBlock());
                }
            });
        }

    }

    public static class EMISupport {
        public static void ghostItem(UIElement element, Predicate<ItemStack> filter, Consumer<ItemStack> setter) {
            LDLibEMIPlugin.renderDragHandler(element,
                    dragged -> dragged instanceof ItemEmiStack item && filter.test(item.getItemStack()));
            LDLibEMIPlugin.dropStackHandler(element,
                    dragged -> dragged instanceof ItemEmiStack item && filter.test(item.getItemStack()),
                    dragged -> {
                        if (dragged instanceof ItemEmiStack item) {
                            setter.accept(item.getItemStack());
                        }
                    });
        }

        public static void ghostFluid(UIElement element, Predicate<FluidStack> filter, Consumer<FluidStack> setter) {
            LDLibEMIPlugin.renderDragHandler(element,
                    dragged -> dragged instanceof FluidEmiStack fluid && filter.test(new FluidStack(
                            ((net.minecraft.world.level.material.Fluid) fluid.getKey()).builtInRegistryHolder(),
                            Math.max(1000, (int) fluid.getAmount()),
                            fluid.getComponentChanges())));
            LDLibEMIPlugin.dropStackHandler(element,
                    dragged -> dragged instanceof FluidEmiStack fluid && filter.test(new FluidStack(
                            ((net.minecraft.world.level.material.Fluid) fluid.getKey()).builtInRegistryHolder(),
                            Math.max(1000, (int) fluid.getAmount()),
                            fluid.getComponentChanges())),
                    dragged -> {
                        if (dragged instanceof FluidEmiStack fluid) {
                            var fluidstack = new FluidStack(
                                    ((net.minecraft.world.level.material.Fluid) fluid.getKey()).builtInRegistryHolder(),
                                    Math.max(1000, (int) fluid.getAmount()),
                                    fluid.getComponentChanges());
                            setter.accept(fluidstack);
                        }
                    });
        }

        public static void ghostBlock(UIElement element, Predicate<net.minecraft.world.level.block.Block> filter, Consumer<net.minecraft.world.level.block.Block> setter) {
            ghostItem(element,itemStack -> {
                if (itemStack.getItem() instanceof BlockItem blockItem) {
                    return filter.test(blockItem.getBlock());
                }
                return false;
            }, itemStack -> {
                if (itemStack.getItem() instanceof BlockItem blockItem) {
                    setter.accept(blockItem.getBlock());
                }
            });
        }
    }
}
