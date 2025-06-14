package com.lowdragmc.lowdraglib2.gui.widget;

import com.google.common.collect.Lists;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.editor_outdated.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib2.gui.ingredient.IGhostIngredientTarget;
import com.lowdragmc.lowdraglib2.gui.ingredient.Target;
import com.mojang.blaze3d.platform.InputConstants;
import dev.emi.emi.api.stack.EmiStack;
import lombok.Getter;
import lombok.Setter;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

@LDLRegister(name = "phantom_item_slot", group = "widget.container", registry = "ldlib2:widget")
public class PhantomSlotWidget extends SlotWidget implements IGhostIngredientTarget, IConfigurableWidget {

    @Getter
    private boolean clearSlotOnRightClick;

    @Configurable
    @ConfigNumber(range = {0, 64})
    @Setter
    @Getter
    private int maxStackSize = 64;

    public PhantomSlotWidget() {
        super();
    }

    public PhantomSlotWidget(IItemHandlerModifiable itemHandler, int slotIndex, int xPosition, int yPosition) {
        super(itemHandler, slotIndex, xPosition, yPosition, true, true);
    }

    public PhantomSlotWidget setClearSlotOnRightClick(boolean clearSlotOnRightClick) {
        this.clearSlotOnRightClick = clearSlotOnRightClick;
        return this;
    }

    @ConfigSetter(field = "canTakeItems")
    public PhantomSlotWidget setCanTakeItems(boolean v) {
        // you cant modify it
        return this;
    }

    @ConfigSetter(field = "canPutItems")
    public PhantomSlotWidget setCanPutItems(boolean v) {
        // you cant modify it
        return this;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (slotReference != null && isMouseOverElement(mouseX, mouseY) && gui != null) {
            if (isClientSideWidget && !gui.getModularUIContainer().getCarried().isEmpty()) {
                slotReference.set(gui.getModularUIContainer().getCarried());
            } else if (button == 1 && clearSlotOnRightClick && !slotReference.getItem().isEmpty()) {
                slotReference.set(ItemStack.EMPTY);
                writeClientAction(2, buf -> {
                });
            } else {
                HOVER_SLOT = slotReference;
                gui.getModularUIGui().superMouseClicked(mouseX, mouseY, button);
                HOVER_SLOT = null;
            }
            return true;
        }
        return false;
    }

    @Override
    public ItemStack slotClick(int dragType, ClickType clickTypeIn, Player player) {
        if (slotReference != null && gui != null) {
            ItemStack stackHeld = gui.getModularUIContainer().getCarried();
            return slotClickPhantom(slotReference, dragType, clickTypeIn, stackHeld);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canMergeSlot(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canTakeStack(Player player) {
        return false;
    }

    @Override
    public boolean canPutStack(ItemStack stack) {
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public List<Target> getPhantomTargets(Object ingredient) {
        if (LDLib2.isEmiLoaded() && ingredient instanceof EmiStack itemEmiStack) {
            Item item = itemEmiStack.getKeyOfType(Item.class);
            ingredient = item == null ? null : new ItemStack(item, (int)itemEmiStack.getAmount());
            if (ingredient instanceof ItemStack itemStack) {
                itemStack.applyComponents(itemEmiStack.getComponentChanges());
            }
        }
        if (LDLib2.isJeiLoaded() && ingredient instanceof ITypedIngredient<?> itemJeiStack) {
            ingredient = itemJeiStack.getItemStack().orElse(ItemStack.EMPTY);
        }
        if (!(ingredient instanceof ItemStack)) {
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
                if (LDLib2.isEmiLoaded() && ingredient instanceof EmiStack itemEmiStack) {
                    Item item = itemEmiStack.getKeyOfType(Item.class);
                    ingredient = item == null ? null : new ItemStack(item, (int)itemEmiStack.getAmount());
                    if (ingredient instanceof ItemStack itemStack) {
                        itemStack.applyComponents(itemEmiStack.getComponentChanges());
                    }
                }
                if (LDLib2.isJeiLoaded() && ingredient instanceof ITypedIngredient<?> itemJeiStack) {
                    ItemStack itemStack = itemJeiStack.getItemStack().orElse(ItemStack.EMPTY);
                    if (!itemStack.isEmpty()) {
                        ingredient = itemStack;
                    }
                }
                if (slotReference != null && ingredient instanceof ItemStack stack) {
                    long id = Minecraft.getInstance().getWindow().getWindow();
                    boolean shiftDown = InputConstants.isKeyDown(id, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(id, GLFW.GLFW_KEY_LEFT_SHIFT);
                    ClickType clickType = shiftDown ? ClickType.QUICK_MOVE : ClickType.PICKUP;
                    slotClickPhantom(slotReference, 0, clickType, stack);
                    writeClientAction(1, buffer -> {
                        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, stack);
                        buffer.writeVarInt(0);
                        buffer.writeBoolean(shiftDown);
                    });
                }
            }
        });
    }

    @Override
    public void handleClientAction(int id, RegistryFriendlyByteBuf buffer) {
        if (slotReference != null && id == 1) {
            ItemStack stackHeld = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
            int mouseButton = buffer.readVarInt();
            boolean shiftKeyDown = buffer.readBoolean();
            ClickType clickType = shiftKeyDown ? ClickType.QUICK_MOVE : ClickType.PICKUP;
            slotClickPhantom(slotReference, mouseButton, clickType, stackHeld);
        } else if (slotReference != null && id == 2) {
            slotReference.set(ItemStack.EMPTY);
        }
    }

    public ItemStack slotClickPhantom(Slot slot, int mouseButton, ClickType clickTypeIn, ItemStack stackHeld) {
        ItemStack stack = ItemStack.EMPTY;

        ItemStack stackSlot = slot.getItem();
        if (!stackSlot.isEmpty()) {
            stack = stackSlot.copy();
        }

        if (mouseButton == 2) {
            fillPhantomSlot(slot, ItemStack.EMPTY, mouseButton);
        } else if (mouseButton == 0 || mouseButton == 1) {

            if (stackSlot.isEmpty()) {
                if (!stackHeld.isEmpty() ) {
                    fillPhantomSlot(slot, stackHeld, mouseButton);
                }
            } else if (stackHeld.isEmpty()) {
                adjustPhantomSlot(slot, mouseButton, clickTypeIn);
            } else  {
                if (!areItemsEqual(stackSlot, stackHeld)) {
                    adjustPhantomSlot(slot, mouseButton, clickTypeIn);
                }
                fillPhantomSlot(slot, stackHeld, mouseButton);
            }
        } else if (mouseButton == 5) {
            if (!slot.hasItem()) {
                fillPhantomSlot(slot, stackHeld, mouseButton);
            }
        }
        return stack;
    }

    private void adjustPhantomSlot(Slot slot, int mouseButton, ClickType clickTypeIn) {
        ItemStack stackSlot = slot.getItem();
        int stackSize;
        if (clickTypeIn == ClickType.QUICK_MOVE) {
            stackSize = mouseButton == 0 ? (stackSlot.getCount() + 1) / 2 : stackSlot.getCount() * 2;
        } else {
            stackSize = mouseButton == 0 ? stackSlot.getCount() - 1 : stackSlot.getCount() + 1;
        }

        if (stackSize > slot.getMaxStackSize()) {
            stackSize = slot.getMaxStackSize();
        }

        stackSlot.setCount(Math.min(maxStackSize, stackSize));

        slot.set(stackSlot);
    }

    private void fillPhantomSlot(Slot slot, ItemStack stackHeld, int mouseButton) {
        if (stackHeld.isEmpty()) {
            slot.set(ItemStack.EMPTY);
            return;
        }

        int stackSize = mouseButton == 0 ? stackHeld.getCount() : 1;
        if (stackSize > slot.getMaxStackSize()) {
            stackSize = slot.getMaxStackSize();
        }
        ItemStack phantomStack = stackHeld.copy();
        phantomStack.setCount(Math.min(maxStackSize, stackSize));
        slot.set(phantomStack);
    }

    public boolean areItemsEqual(ItemStack itemStack1, ItemStack itemStack2) {
        return ItemStack.matches(itemStack1, itemStack2);
    }
}
