package com.lowdragmc.lowdraglib.gui.modular;


import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

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

    interface Block extends IUIHolder {
        default BlockEntity self() {
            return (BlockEntity) this;
        }

        @Override
        default boolean isInvalid() {
            return self().isRemoved();
        }

        @Override
        default boolean isRemote() {
            return self().getLevel().isClientSide;
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
