package com.lowdragmc.lowdraglib2.client.scene;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

/**
 * @author KilaBash
 * @date 2022/06/05
 * @implNote CameraEntity,
 */
public class CameraEntity extends Entity {

    public CameraEntity(Level pLevel) {
        super(EntityType.PLAYER, pLevel);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_326003_) {

    }

    @Override
    protected void readAdditionalSaveData(@Nonnull CompoundTag pCompound) {

    }

    @Override
    protected void addAdditionalSaveData(@Nonnull CompoundTag pCompound) {

    }

    @Override
    @Nonnull
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket(this, serverEntity);
    }
}
