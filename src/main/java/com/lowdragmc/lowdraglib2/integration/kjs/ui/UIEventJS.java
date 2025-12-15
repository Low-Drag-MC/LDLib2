package com.lowdragmc.lowdraglib2.integration.kjs.ui;

import com.google.common.base.Predicates;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.factory.IContainerUIHolder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class UIEventJS implements KubeEvent, MenuProvider, IContainerUIHolder {
    public final Player player;
    public final String id;
    public final Level level;

    // runtime
    @Nullable
    public ModularUI modularUI;
    public Predicate<Player> validator;
    public Component displayName;

    public UIEventJS(Player player, String id) {
        this.player = player;
        this.id = id;
        this.level = player.level();
        // default validator
        this.validator = Predicates.alwaysTrue();
        this.displayName = Component.translatable(id);
    }

    @Override
    @HideFromJS
    public boolean isStillValid(Player player) {
        return validator.test(player);
    }

    @Override
    @HideFromJS
    public Component getDisplayName() {
        return displayName;
    }

    public abstract MenuType<ModularUIContainerMenu> getMenuType();

    @Override
    @Nullable
    @HideFromJS
    public ModularUIContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        if (modularUI == null) return null;
        try {
            return new ModularUIContainerMenu(getMenuType(), containerId, playerInventory, this);
        } catch (Exception e) {
            LDLib2.LOGGER.error("Failed to create UI {} for {}", id, player, e);
            return null;
        }
    }

    @Override
    @HideFromJS
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(id);
    }

    @Override
    @HideFromJS
    public ModularUI createUI(Player player) {
        if (modularUI != null) {
            return modularUI;
        }
        throw new IllegalStateException("No Modular UI found");
    }
}
