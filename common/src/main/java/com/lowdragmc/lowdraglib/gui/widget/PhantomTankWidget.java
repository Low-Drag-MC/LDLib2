package com.lowdragmc.lowdraglib.gui.widget;

import com.google.common.collect.Lists;
import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.editor.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib.gui.ingredient.IGhostIngredientTarget;
import com.lowdragmc.lowdraglib.gui.ingredient.Target;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.side.fluid.FluidTransferHelper;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.side.fluid.IFluidStorage;
import com.lowdragmc.lowdraglib.side.fluid.IFluidTransfer;
import dev.emi.emi.api.stack.EmiStack;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;

import javax.annotation.Nullable;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@LDLRegister(name = "phantom_tank_slot", group = "widget.container")
public class PhantomTankWidget extends TankWidget implements IGhostIngredientTarget, IConfigurableWidget {

    private Consumer<FluidStack> fluidStackUpdater;

    public PhantomTankWidget() {
        super();
        this.allowClickFilled = false;
        this.allowClickDrained = false;
    }

    public PhantomTankWidget(IFluidStorage fluidTank, int x, int y) {
        super(fluidTank, x, y, false, false);
    }

    public PhantomTankWidget(@Nullable IFluidStorage fluidTank, int x, int y, int width, int height) {
        super(fluidTank, x, y, width, height, false, false);
    }

    public PhantomTankWidget(IFluidTransfer fluidTank, int tank, int x, int y) {
        super(fluidTank, tank, x, y, false, false);
    }

    public PhantomTankWidget(@Nullable IFluidTransfer fluidTank, int tank, int x, int y, int width, int height) {
        super(fluidTank, tank, x, y, width, height, false, false);
    }

    public PhantomTankWidget setIFluidStackUpdater(Consumer<FluidStack> fluidStackUpdater) {
        this.fluidStackUpdater = fluidStackUpdater;
        return this;
    }

    @ConfigSetter(field = "allowClickFilled")
    public PhantomTankWidget setAllowClickFilled(boolean v) {
        // you cant modify it
        return this;
    }

    @ConfigSetter(field = "allowClickDrained")
    public PhantomTankWidget setAllowClickDrained(boolean v) {
        // you cant modify it
        return this;
    }

    public static FluidStack drainFrom(Object ingredient) {
        if (ingredient instanceof Ingredient ing) {
            var items = ing.getItems();
            if (items.length > 0) {
                ingredient = items[0];
            }
        }
        if (ingredient instanceof ItemStack itemStack) {
            var handler = FluidTransferHelper.getFluidTransfer(new ItemStackTransfer(itemStack), 0);
            if (handler != null) {
                return handler.drain(Long.MAX_VALUE, true);
            }
        }
        return FluidStack.empty();
    }

    @Override
    @Environment(EnvType.CLIENT)
    public List<Target> getPhantomTargets(Object ingredient) {
        if (LDLib.isReiLoaded() && ingredient instanceof dev.architectury.fluid.FluidStack fluidStack) {
            ingredient = FluidStack.create(fluidStack.getFluid(), fluidStack.getAmount(), fluidStack.getTag());
        }
        if (LDLib.isEmiLoaded() && ingredient instanceof EmiStack fluidEmiStack) {
            Fluid fluid = fluidEmiStack.getKeyOfType(Fluid.class);
            if (fluid == null) {
                Item item = fluidEmiStack.getKeyOfType(Item.class);
                ingredient = item == null ? null : new ItemStack(item, (int)fluidEmiStack.getAmount());
                if (ingredient instanceof ItemStack itemStack) {
                    itemStack.setTag(fluidEmiStack.getNbt());
                }
            } else {
                ingredient = FluidStack.create(fluid, fluidEmiStack.getAmount() == 0L ? 1000L : fluidEmiStack.getAmount(), fluidEmiStack.getNbt());
            }
        }
        if (LDLib.isJeiLoaded() && ingredient instanceof ITypedIngredient<?> typedIngredient) {
            ingredient = PhantomFluidWidget.checkJEIIngredient(typedIngredient.getIngredient());
        }
        if (!(ingredient instanceof FluidStack) && drainFrom(ingredient).isEmpty()) {
            return Collections.emptyList();
        }

        Rect2i rectangle = toRectangleBox();
        return Lists.newArrayList(new Target() {
            @Nonnull
            @Override
            public Rect2i getArea() {
                return rectangle;
            }

            @Override
            public void accept(@Nonnull Object ingredient) {
                FluidStack ingredientStack;
                if (LDLib.isReiLoaded() && ingredient instanceof dev.architectury.fluid.FluidStack fluidStack) {
                    ingredient = FluidStack.create(fluidStack.getFluid(), fluidStack.getAmount(), fluidStack.getTag());
                }
                if (LDLib.isEmiLoaded() && ingredient instanceof EmiStack fluidEmiStack) {
                    var fluid = fluidEmiStack.getKeyOfType(Fluid.class);
                    if (fluid == null) {
                        Item item = fluidEmiStack.getKeyOfType(Item.class);
                        ingredient = item == null ? null : new ItemStack(item, (int)fluidEmiStack.getAmount());
                        if (ingredient instanceof ItemStack itemStack) {
                            itemStack.setTag(fluidEmiStack.getNbt());
                        }
                    } else {
                        ingredient = FluidStack.create(fluid, fluidEmiStack.getAmount() == 0L ? 1000L : fluidEmiStack.getAmount(), fluidEmiStack.getNbt());
                    }
                }
                if (LDLib.isJeiLoaded()) {
                    ingredient = PhantomFluidWidget.checkJEIIngredient(ingredient);
                }
                if (ingredient instanceof FluidStack fluidStack)
                    ingredientStack = fluidStack;
                else
                    ingredientStack = drainFrom(ingredient);

                if (ingredientStack != null && !ingredientStack.isEmpty()) {
                    CompoundTag tagCompound = ingredientStack.saveToTag(new CompoundTag());
                    writeClientAction(2, buffer -> buffer.writeNbt(tagCompound));
                }

                if (isClientSideWidget && fluidTank != null) {
                    fluidTank.drain(fluidTank.getTankCapacity(tank), false);
                    if (ingredientStack != null) {
                        fluidTank.fill(ingredientStack.copy(), false);
                    }
                    if (fluidStackUpdater != null) {
                        fluidStackUpdater.accept(ingredientStack);
                    }
                }
            }
        });
    }

    @Override
    public void handleClientAction(int id, FriendlyByteBuf buffer) {
        if (id == 1) {
            handlePhantomClick();
        } else if (id == 2) {
            FluidStack fluidStack;
            fluidStack = FluidStack.loadFromTag(buffer.readNbt());
            if (fluidTank == null) return;
            fluidTank.drain(fluidTank.getTankCapacity(tank), false);
            if (fluidStack != null) {
                fluidTank.fill(fluidStack.copy(), false);
            }
            if (fluidStackUpdater != null) {
                fluidStackUpdater.accept(fluidStack);
            }
        }
    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOverElement(mouseX, mouseY)) {
            if (isClientSideWidget) {
                handlePhantomClick();
            } else {
                writeClientAction(1, buffer -> { });
            }
            return true;
        }
        return false;
    }

    private void handlePhantomClick() {
        if (fluidTank == null) return;
        ItemStack itemStack = gui.getModularUIContainer().getCarried().copy();
        if (!itemStack.isEmpty()) {
            itemStack.setCount(1);
            var handler = FluidTransferHelper.getFluidTransfer(gui.entityPlayer, gui.getModularUIContainer());
            if (handler != null) {
                FluidStack resultFluid = handler.drain(Integer.MAX_VALUE, true);
                fluidTank.drain(fluidTank.getTankCapacity(tank), false);
                fluidTank.fill(resultFluid.copy(), false);
                if (fluidStackUpdater != null) {
                    fluidStackUpdater.accept(resultFluid);
                }
            }
        } else {
            fluidTank.drain(fluidTank.getTankCapacity(tank), false);
            if (fluidStackUpdater != null) {
                fluidStackUpdater.accept(null);
            }
        }
    }

}
