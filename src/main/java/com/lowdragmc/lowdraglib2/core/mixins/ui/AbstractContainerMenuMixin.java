package com.lowdragmc.lowdraglib2.core.mixins.ui;

import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolderMenu;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin implements IModularUIHolderMenu {
    @Unique
    @Nullable
    public ModularUI ldlib2$modularUI;
    @Unique
    public final Map<Slot, ItemSlot> ldlib2$itemSlotMap = new HashMap<>();

    @Override
    public @Nullable ItemSlot ldlib2$getItemSlot(Slot slot) {
        return ldlib2$itemSlotMap.get(slot);
    }

    @Override
    public void ldlib2$addSlot(ItemSlot itemSlot) {
        ldlib2$itemSlotMap.put(itemSlot.getSlot(), itemSlot);
    }

    @Override
    public @Nullable ModularUI ldlib2$getModularUI() {
        return ldlib2$modularUI;
    }

    @Override
    public void ldlib2$setModularUI(ModularUI modularUI) {
        ldlib2$modularUI = modularUI;
        modularUI.setMenu((AbstractContainerMenu)(Object)this);
    }

    @Inject(method = "broadcastChanges", at = @At(value = "RETURN"))
    private void ldlib2$broadcastChanges(CallbackInfo ci) {
        var mui = getModularUI();
        if (mui != null) {
            mui.tickServer();
        }
    }
}
