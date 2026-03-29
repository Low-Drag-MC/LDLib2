package com.lowdragmc.lowdraglib2.core.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * @author KilaBash
 * @date 2022/7/23
 * @implNote ParticleEngineMixin
 * @port ELB_GG 
 * @date_port 2026/03/29 
 * @port_to fabric
 */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {

    @Shadow @Final private Map<ParticleRenderType, Queue<Particle>> particles;
    @Shadow @Final private static List<ParticleRenderType> RENDER_ORDER;
    @Shadow @Final private TextureManager textureManager;

    @Inject(method = "tick", at = @At("TAIL"))
    public void injectTick(CallbackInfo ci) {
        particles.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    @Inject(method = "render", at = @At("RETURN"))
    public void ldlib2$injectCustomParticleRender(LightTexture lightTexture, Camera camera, float partialTick, CallbackInfo ci) {
        for (var entry : particles.entrySet()) {
            var renderType = entry.getKey();
            if (RENDER_ORDER.contains(renderType) || renderType == ParticleRenderType.NO_RENDER) continue;
            var queue = entry.getValue();
            if (queue == null || queue.isEmpty()) continue;
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            var tesselator = Tesselator.getInstance();
            var bufferBuilder = renderType.begin(tesselator, this.textureManager);
            if (bufferBuilder == null) continue;
            for (var particle : queue) {
                try {
                    particle.render(bufferBuilder, camera, partialTick);
                } catch (Throwable throwable) {
                    // skip bad particles
                }
            }
            var data = bufferBuilder.build();
            if (data == null) continue;
            BufferUploader.drawWithShader(data);
        }
    }

}
