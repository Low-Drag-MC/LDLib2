package com.lowdragmc.lowdraglib2.client.scene;

import com.google.common.collect.Queues;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.Queue;

/**
 * Thin wrapper around vanilla {@link ParticleEngine} for scene-local particle simulation —
 * reuses every ParticleGroup / mod factory / item-pickup / elder-guardian path rather than
 * re-implementing a SingleQuadParticle-only fork. Engine init is deferred until a
 * {@link ClientLevel} is wired in via {@link #setLevel}; particles added before then queue
 * up in {@link #pendingAdds} and flush on first init.
 */
@OnlyIn(Dist.CLIENT)
public class ParticleManager {

    /** Lazy-init: needs a {@link ClientLevel} which the dummy world provides asynchronously. */
    @Nullable
    private ParticleEngine engine;
    @Nullable
    @lombok.Getter
    private ClientLevel level;

    /** Particles added before {@link #engine} exists; flushed on first {@link #ensureEngine()}. */
    private final Queue<Particle> pendingAdds = Queues.newArrayDeque();

    /** Reused across frames; {@link ParticlesRenderState#reset()} clears its internal list. */
    @Nullable
    private ParticlesRenderState renderState;

    public void setLevel(@Nullable ClientLevel level) {
        this.level = level;
        if (engine != null) engine.setLevel(level);
        else ensureEngine(); // eager — avoids any early addParticle / tick falling into the pending-queue path
    }

    public void addParticle(Particle particle) {
        if (engine != null) {
            engine.add(particle);
        } else {
            synchronized (pendingAdds) {
                pendingAdds.add(particle);
            }
        }
    }

    public void createTrackingEmitter(Entity entity, ParticleOptions options) {
        ensureEngine();
        if (engine != null) engine.createTrackingEmitter(entity, options);
    }

    public void createTrackingEmitter(Entity entity, ParticleOptions options, int lifeTime) {
        ensureEngine();
        if (engine != null) engine.createTrackingEmitter(entity, options, lifeTime);
    }

    public void clearAllParticles() {
        if (engine != null) engine.clearParticles();
        synchronized (pendingAdds) {
            pendingAdds.clear();
        }
    }

    public int getParticleAmount() {
        if (engine == null) return pendingAdds.size();
        // ParticleEngine.countParticles() returns the count as a string ("12345").
        try {
            return Integer.parseInt(engine.countParticles());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void tick() {
        ensureEngine();
        if (engine != null) engine.tick();
    }

    private void ensureEngine() {
        if (engine != null || level == null) return;
        var vanillaParticleEngine = Minecraft.getInstance().particleEngine;
        if (vanillaParticleEngine == null) return; // Minecraft still initialising
        engine = new ParticleEngine(level, vanillaParticleEngine.resourceManager);
        synchronized (pendingAdds) {
            while (!pendingAdds.isEmpty()) {
                engine.add(pendingAdds.poll());
            }
        }
    }

    /**
     * Extract live particles and submit into {@code storage}. Caller must drain via
     * {@code FeatureRenderDispatcher.renderSolidFeatures() / renderTranslucentParticles()}
     * <strong>before</strong> calling {@link #afterRender()} — the submitted state objects
     * are reused across frames and their {@code particleCount} is what the dispatcher sees.
     */
    public void render(SubmitNodeStorage storage,
                       CameraRenderState cameraRenderState,
                       Camera camera,
                       Frustum frustum,
                       float partialTicks) {
        if (engine == null) return;
        if (renderState == null) renderState = new ParticlesRenderState();
        engine.extract(renderState, frustum, camera, partialTicks);
        renderState.submit(storage, cameraRenderState);
    }

    /** Release per-frame submit state. Run AFTER the dispatcher has drained the storage. */
    public void afterRender() {
        if (renderState != null) renderState.reset();
    }
}
