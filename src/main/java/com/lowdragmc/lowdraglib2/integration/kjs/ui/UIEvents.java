package com.lowdragmc.lowdraglib2.integration.kjs.ui;

import com.lowdragmc.lowdraglib2.gui.factory.HeldItemUIMenuType;
import dev.latvian.mods.kubejs.event.*;
import dev.latvian.mods.kubejs.level.BlockContainerJS;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface UIEvents {
    EventGroup INSTANCE = EventGroup.of("LDLib2UI");
    TargetedEventHandler<ResourceLocation> BLOCK = INSTANCE.server("block", () -> BlockUIEventJS.class).requiredTarget(EventTargetType.ID).hasResult();
    TargetedEventHandler<ResourceLocation> ITEM = INSTANCE.server("item", () -> ItemUIEventJS.class).requiredTarget(EventTargetType.ID).hasResult();

    @AllArgsConstructor
    @Getter
    class BlockUIEventJS implements KubeEvent {
        public Level level;
        public BlockPos pos;
        public BlockContainerJS block;
        public Player player;
    }

    @AllArgsConstructor
    @Getter
    class ItemUIEventJS implements KubeEvent {
        public final HeldItemUIMenuType.HeldItemUIHolder heldItemUIHolder;
    }
}
