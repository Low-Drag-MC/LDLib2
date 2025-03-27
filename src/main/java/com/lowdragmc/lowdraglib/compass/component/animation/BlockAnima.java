package com.lowdragmc.lowdraglib.compass.component.animation;

import net.minecraft.core.BlockPos;
import org.joml.Vector3f;

/**
 * @author KilaBash
 * @date 2023/7/29
 * @implNote BlockAnima
 */
public record BlockAnima(BlockPos pos, Vector3f offset, int duration) {
    public BlockAnima(BlockPos pos, Vector3f animaPos) {
        this(pos, animaPos, 15);
    }

    public BlockAnima(BlockPos pos) {
        this(pos, new Vector3f(0, 0.7f, 0));
    }
}
