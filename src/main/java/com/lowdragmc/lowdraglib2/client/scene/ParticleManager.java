package com.lowdragmc.lowdraglib2.client.scene;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureManager;

import java.util.*;
import java.util.function.Predicate;

/**
 * @author KilaBash
 * @date 2022/06/05
 * @implNote ParticleManager, for LParticle
 */
@OnlyIn(Dist.CLIENT)
public class ParticleManager {
    private static final List<ParticleRenderType> RENDER_ORDER = ImmutableList.of(
            ParticleRenderType.SINGLE_QUADS, ParticleRenderType.ITEM_PICKUP, ParticleRenderType.ELDER_GUARDIANS
    );
    private final Queue<TrackingEmitter> trackingEmitters = Queues.newArrayDeque();
    protected final Queue<Particle> waitToAdded = Queues.newArrayDeque();
    protected final Map<ParticleRenderType, Queue<Particle>> particles = Maps.newTreeMap(makeParticleRenderTypeComparator(RENDER_ORDER));
    protected final TextureManager textureManager = Minecraft.getInstance().getTextureManager();

    public ClientLevel level;

    public void setLevel(ClientLevel level) {
        this.level = level;
    }

    public void createTrackingEmitter(Entity entity, ParticleOptions particle) {
        this.trackingEmitters.add(new TrackingEmitter(level, entity, particle));
    }

    public void createTrackingEmitter(Entity entity, ParticleOptions particle, int lifeTime) {
        this.trackingEmitters.add(new TrackingEmitter(level, entity, particle, lifeTime));
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
        this.particles.forEach((type, particleQueue) -> this.tickParticleList(particleQueue));
        if (!this.trackingEmitters.isEmpty()) {
            List<TrackingEmitter> removed = Lists.newArrayList();

            for (TrackingEmitter emitter : this.trackingEmitters) {
                emitter.tick();
                if (!emitter.isAlive()) {
                    removed.add(emitter);
                }
            }

            this.trackingEmitters.removeAll(removed);
        }

        if (!waitToAdded.isEmpty()) {
            synchronized (waitToAdded) {
                for (var particle : waitToAdded) {
                    particles.computeIfAbsent(particle.getGroup(), type -> Queues.newArrayDeque()).add(particle);
                }
                waitToAdded.clear();
            }
        }
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

    // todo particle extract
    public void render(PoseStack pMatrixStack, Camera pActiveRenderInfo, float pPartialTicks, Predicate<ParticleRenderType> renderTypePredicate) {
//        Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
//        RenderSystem.enableDepthTest();
//        RenderSystem.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE2);
//        RenderSystem.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
//        Matrix4fStack posestack = RenderSystem.getModelViewStack();
//        posestack.pushMatrix();
//        posestack.mul(pMatrixStack.last().pose());
//        RenderSystem.applyModelViewMatrix();
//
//        for(ParticleRenderType particlerendertype : this.particles.keySet()) {
//            if (particlerendertype == ParticleRenderType.NO_RENDER || !renderTypePredicate.test(particlerendertype)) continue;
//            var iterable = this.particles.get(particlerendertype);
//            if (iterable != null) {
//                RenderSystem.setShader(GameRenderer::getParticleShader);
//                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
//                var tesselator = Tesselator.getInstance();
//                var bufferBuilder = particlerendertype.begin(tesselator, this.textureManager);
//                if (bufferBuilder == null) continue;
//
//                for(var particle : iterable) {
//                    particle.render(bufferBuilder, pActiveRenderInfo, pPartialTicks);
//                }
//
//                var data = bufferBuilder.build();
//                if (data == null) continue;
//                BufferUploader.drawWithShader(data);
//            }
//        }
//
//        posestack.popMatrix();
//        RenderSystem.applyModelViewMatrix();
//        RenderSystem.depthMask(true);
//        RenderSystem.disableBlend();
//        Minecraft.getInstance().gameRenderer.lightTexture().turnOffLightLayer();
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
