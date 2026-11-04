package com.lowdragmc.lowdraglib2.client.scene;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.DynamicGpuData;
import net.minecraft.client.renderer.DynamicGpuDataStorageMapped;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.FlipFrameEvent;
import net.neoforged.neoforge.client.event.lifecycle.ClientStoppingEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The scenes' own ChunkSection UBOs. {@link DynamicGpuData} keeps a single chunk section storage and recreates it
 * whenever the per-draw UBO and the multi-draw-indirect kind alternate, so sharing it with a world drawn indirectly
 * would rebuild both every frame.
 */
@EventBusSubscriber(modid = LDLib2.MOD_ID, value = Dist.CLIENT)
public final class SceneChunkSections {
    private static final int INITIAL_CAPACITY = 64;

    @Nullable
    private static DynamicGpuDataStorageMapped<DynamicGpuData.ChunkSectionInfo> storage;
    private static boolean writtenThisFrame;

    private SceneChunkSections() {
    }

    static GpuBufferSlice[] write(DynamicGpuData.ChunkSectionInfo[] infos) {
        if (storage == null) {
            storage = new DynamicGpuDataStorageMapped<>("LDLib2 Scene Chunk Sections UBO",
                    DynamicGpuData.CHUNK_SECTION_UBO_SIZE, GpuBuffer.USAGE_UNIFORM, INITIAL_CAPACITY);
        }
        writtenThisFrame = true;
        return storage.writeData(infos);
    }

    @SubscribeEvent
    public static void onFlipFrame(FlipFrameEvent event) {
        if (storage != null && writtenThisFrame) {
            writtenThisFrame = false;
            storage.endFrame();
        }
    }

    @SubscribeEvent
    public static void onClientStopping(ClientStoppingEvent event) {
        if (storage != null) {
            storage.close();
            storage = null;
        }
        writtenThisFrame = false;
    }
}
