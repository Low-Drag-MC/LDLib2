package com.lowdragmc.lowdraglib.integration.kjs.ui;

import dev.latvian.mods.kubejs.event.*;
import dev.latvian.mods.kubejs.level.BlockContainerJS;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface UIEvents {
    EventGroup INSTANCE = EventGroup.of("LDLibUI");
    TargetedEventHandler<String> BLOCK = INSTANCE.server("block", () -> BlockUIEventJS.class).requiredTarget(EventTargetType.STRING).hasResult();
    TargetedEventHandler<String> ITEM = INSTANCE.server("item", () -> ItemUIEventJS.class).requiredTarget(EventTargetType.STRING).hasResult();

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
        public Player player;
        public InteractionHand hand;
        public ItemStack held;
    }
}
