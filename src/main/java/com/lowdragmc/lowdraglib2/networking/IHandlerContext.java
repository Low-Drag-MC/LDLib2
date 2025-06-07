package com.lowdragmc.lowdraglib2.networking;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;

/**
 * @author KilaBash
 * @date 2023/2/9
 * @implNote IHandlerContext
 */
public interface IHandlerContext {
    Object getContext();
    boolean isClient();
    @Nullable ServerPlayer getPlayer();
    @Nullable MinecraftServer getServer();
    Level getLevel();

}
