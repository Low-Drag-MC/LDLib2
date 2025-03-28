package com.lowdragmc.lowdraglib.utils.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

@ToString
@EqualsAndHashCode
@Getter
@Setter
public final class EntityInfo {
    private int id;
    @Nullable
    private CompoundTag tag;
    @Nullable
    private EntityType<?> entityType;

    private EntityInfo(int id, @Nullable EntityType<?> entityType, @Nullable CompoundTag tag) {
        this.id = id;
        this.entityType = entityType;
        this.tag = tag;
    }

    public static EntityInfo of(int id) {
        return of(id, null, null);
    }

    public static EntityInfo of(int id, @Nullable EntityType<?> entityType) {
        return of(id, entityType, null);
    }

    public static EntityInfo of(int id, @Nullable EntityType<?> entityType, @Nullable CompoundTag tag) {
        return new EntityInfo(id, entityType, tag);
    }

}
