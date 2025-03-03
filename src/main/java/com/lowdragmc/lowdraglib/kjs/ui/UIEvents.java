package com.lowdragmc.lowdraglib.kjs.ui;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.event.Extra;
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
    EventHandler BLOCK = INSTANCE.server("block", () -> BlockUIEventJS.class).extra(Extra.STRING).hasResult();
    EventHandler ITEM = INSTANCE.server("item", () -> ItemUIEventJS.class).extra(Extra.STRING).hasResult();

    @AllArgsConstructor
    @Getter
    class BlockUIEventJS extends EventJS {
        public Level level;
        public BlockPos pos;
        public BlockContainerJS block;
        public Player player;
    }

    @AllArgsConstructor
    @Getter
    class ItemUIEventJS extends EventJS {
        public Player player;
        public InteractionHand hand;
        public ItemStack held;
    }
}
