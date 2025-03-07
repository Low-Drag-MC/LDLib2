package com.lowdragmc.lowdraglib.gui.modular;


import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import net.minecraft.world.entity.player.Player;

public interface IUIHolder {
    IUIHolder EMPTY = new IUIHolder() {
        @Override
        public ModularUI createUI(Player entityPlayer) {
            return null;
        }

        @Override
        public boolean isInvalid() {
            return false;
        }

        @Override
        public boolean isRemote() {
            return true;
        }

        @Override
        public void markAsDirty() {

        }
    };

    interface BlockEntity extends IUIHolder {
        default net.minecraft.world.level.block.entity.BlockEntity self() {
            return (net.minecraft.world.level.block.entity.BlockEntity) this;
        }

        @Override
        default boolean isInvalid() {
            return self().isRemoved();
        }

        @Override
        default boolean isRemote() {
            var level = self().getLevel();
            return level == null ? LDLib.isRemote() : level.isClientSide;
        }

        @Override
        default void markAsDirty() {
            self().setChanged();
        }
    }

    interface Item {
        ModularUI createUI(Player entityPlayer, HeldItemUIFactory.HeldItemHolder holder);
    }

    ModularUI createUI(Player entityPlayer);

    boolean isInvalid();

    boolean isRemote();

    void markAsDirty();

}
