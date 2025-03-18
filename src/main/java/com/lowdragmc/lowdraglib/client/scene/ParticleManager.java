package com.lowdragmc.lowdraglib.client.scene;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.level.Level;
import org.joml.Matrix4fStack;

import java.util.*;

/**
 * @author KilaBash
 * @date 2022/06/05
 * @implNote ParticleManager, for LParticle
 */
@OnlyIn(Dist.CLIENT)
public class ParticleManager {
    private static final List<ParticleRenderType> RENDER_ORDER = ImmutableList.of(ParticleRenderType.TERRAIN_SHEET, ParticleRenderType.PARTICLE_SHEET_OPAQUE, ParticleRenderType.PARTICLE_SHEET_LIT, ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT, ParticleRenderType.CUSTOM);
    private final Queue<Particle> waitToAdded = Queues.newArrayDeque();
    private final Map<ParticleRenderType, Queue<Particle>> particles = Maps.newTreeMap(makeParticleRenderTypeComparator(RENDER_ORDER));
    private final TextureManager textureManager = Minecraft.getInstance().getTextureManager();

    public Level level;

    public void setLevel(Level level) {
        this.level = level;
    }

    public void clearAllParticles() {
        synchronized (waitToAdded) {
            waitToAdded.clear();
            particles.clear();
        }
    }

    public void addParticle(Particle particle) {
        synchronized (waitToAdded) {
            waitToAdded.add(particle);
        }
    }

    public int getParticleAmount() {
        int amount = waitToAdded.size();
        amount += particles.values().stream().mapToInt(Collection::size).sum();
        return amount;
    }

    public void tick() {
        if (!waitToAdded.isEmpty()) {
            synchronized (waitToAdded) {
                for (var particle : waitToAdded) {
                    particles.computeIfAbsent(particle.getRenderType(), type -> Queues.newArrayDeque()).add(particle);
                }
                waitToAdded.clear();
            }
        }
        this.particles.forEach((particleRenderType, particleQueue) -> this.tickParticleList(particleQueue));
    }

    private void tickParticleList(Collection<Particle> pParticles) {
        if (!pParticles.isEmpty()) {
            var iterator = pParticles.iterator();
            while(iterator.hasNext()) {
                var particle = iterator.next();
                particle.tick();
                if (!particle.isAlive()) {
                    iterator.remove();
                }
            }
        }

    }

    public void render(PoseStack pMatrixStack, Camera pActiveRenderInfo, float pPartialTicks) {
        Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
        RenderSystem.enableDepthTest();
        RenderSystem.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE2);
        RenderSystem.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        Matrix4fStack posestack = RenderSystem.getModelViewStack();
        posestack.pushMatrix();
        posestack.mul(pMatrixStack.last().pose());
        RenderSystem.applyModelViewMatrix();

        for(ParticleRenderType particlerendertype : this.particles.keySet()) {
            if (particlerendertype == ParticleRenderType.NO_RENDER) continue;
            var iterable = this.particles.get(particlerendertype);
            if (iterable != null) {
                RenderSystem.setShader(GameRenderer::getParticleShader);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                Tesselator tesselator = Tesselator.getInstance();
                BufferBuilder bufferBuilder = particlerendertype.begin(tesselator, this.textureManager);

                for(var particle : iterable) {
                    particle.render(bufferBuilder, pActiveRenderInfo, pPartialTicks);
                }

                BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
            }
        }

        posestack.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        Minecraft.getInstance().gameRenderer.lightTexture().turnOffLightLayer();
    }

    public static Comparator<ParticleRenderType> makeParticleRenderTypeComparator(List<ParticleRenderType> renderOrder) {
        Comparator<ParticleRenderType> vanillaComparator = Comparator.comparingInt(renderOrder::indexOf);
        return (typeOne, typeTwo) ->
        {
            boolean vanillaOne = renderOrder.contains(typeOne);
            boolean vanillaTwo = renderOrder.contains(typeTwo);

            if (vanillaOne && vanillaTwo)
            {
                return vanillaComparator.compare(typeOne, typeTwo);
            }
            if (!vanillaOne && !vanillaTwo)
            {
                return Integer.compare(System.identityHashCode(typeOne), System.identityHashCode(typeTwo));
            }
            return vanillaOne ? -1 : 1;
        };
    }

}
