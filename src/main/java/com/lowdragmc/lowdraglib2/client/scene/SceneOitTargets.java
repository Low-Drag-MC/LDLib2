package com.lowdragmc.lowdraglib2.client.scene;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.oit.OitRenderPassProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;
import org.joml.Vector4fc;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * A scene's targets for improved (order-independent) transparency, sized to the scene, with the passes
 * {@code LevelRenderer.executeOit} runs around the three phases: the depth-bounds cull after the first, and the
 * composite onto the scene. Render thread only.
 */
public final class SceneOitTargets implements AutoCloseable {
    private static final Vector4fc DEPTH_BOUNDS_CLEAR = new Vector4f(-Float.MAX_VALUE, 0, 0, 0);
    private static final Vector4fc ZERO = new Vector4f(0, 0, 0, 0);
    private static final int USAGE = GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING
            | GpuTexture.USAGE_COPY_DST;
    /** The composite for a scene whose colour is not the level's format. */
    private static final Map<GpuFormat, RenderPipeline> COMPOSITES = new EnumMap<>(GpuFormat.class);

    private final int width;
    private final int height;
    private final GpuTexture depthBounds;
    private final GpuTexture depthBoundsCulled;
    private final GpuTexture accumulate;
    private final GpuTexture[] transmittance = new GpuTexture[LevelRenderer.OIT_TRANSMITTANCE_TARGET_COUNT];
    private final GpuTextureView depthBoundsView;
    private final GpuTextureView depthBoundsCulledView;
    private final GpuTextureView accumulateView;
    private final GpuTextureView[] transmittanceViews = new GpuTextureView[LevelRenderer.OIT_TRANSMITTANCE_TARGET_COUNT];

    private SceneOitTargets(int width, int height) {
        this.width = width;
        this.height = height;
        var device = RenderSystem.getDevice();
        depthBounds = device.createTexture(() -> "Scene OIT depth bounds", USAGE, GpuFormat.RGBA32_FLOAT, width, height, 1, 1);
        depthBoundsCulled = device.createTexture(() -> "Scene OIT depth bounds culled", USAGE, GpuFormat.RGBA32_FLOAT,
                width, height, 1, 1);
        accumulate = device.createTexture(() -> "Scene OIT accumulate", USAGE, GpuFormat.RGBA16_FLOAT, width, height, 1, 1);
        for (int i = 0; i < transmittance.length; i++) {
            transmittance[i] = device.createTexture(() -> "Scene OIT transmittance", USAGE, GpuFormat.RGBA16_FLOAT,
                    width, height, 1, 1);
            transmittanceViews[i] = device.createTextureView(transmittance[i]);
        }
        depthBoundsView = device.createTextureView(depthBounds);
        depthBoundsCulledView = device.createTextureView(depthBoundsCulled);
        accumulateView = device.createTextureView(accumulate);
    }

    /** {@code current} when it is {@code width} x {@code height}, otherwise new targets of that size (closing it). */
    public static SceneOitTargets ensure(@Nullable SceneOitTargets current, int width, int height) {
        if (current != null && current.width == width && current.height == height) {
            return current;
        }
        if (current != null) {
            current.close();
        }
        return new SceneOitTargets(width, height);
    }

    /** Clears what the phases accumulate into. No pass is open. */
    public void clear() {
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.clearColorTexture(depthBounds, DEPTH_BOUNDS_CLEAR);
        encoder.clearColorTexture(accumulate, ZERO);
        for (GpuTexture texture : transmittance) {
            encoder.clearColorTexture(texture, ZERO);
        }
    }

    /** The phases' parameters, testing against {@code depth}. */
    public OitRenderPassProvider.Parameters parameters(GpuTextureView depth) {
        return new OitRenderPassProvider.Parameters(depthBoundsView, transmittanceViews, accumulateView, depth);
    }

    /**
     * After the depth-bounds phase: the bounds without what the opaque scene hides, which the later phases read
     * ({@code params} is switched to them). No pass is open.
     */
    public void cullDepthBounds(GpuTextureView depth, OitRenderPassProvider.Parameters params) {
        var nearest = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Scene OIT cull depth bounds",
                depthBoundsCulledView, Optional.empty(), depth, OptionalDouble.empty())) {
            RenderSystem.bindDefaultUniforms(pass);
            pass.setPipeline(RenderSystem.getCompiledPipeline(RenderPipelines.BLIT_DEPTH_BOUNDS));
            pass.setUniform("InSampler", depthBoundsView, nearest);
            pass.draw(3, 1, 0, 0);
            pass.setUniform("DepthBoundsSampler", depthBoundsView, nearest);
            pass.setPipeline(RenderSystem.getCompiledPipeline(RenderPipelines.OIT_DEPTH_BOUNDS_CULL));
            pass.draw(3, 1, 0, 0);
        }
        params.setDepthBoundsTargetView(depthBoundsCulledView);
    }

    /** Resolves the phases onto {@code color}, writing the nearest translucent depth into {@code depth}. No pass is open. */
    public void composite(GpuTextureView color, GpuTextureView depth) {
        var nearest = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Scene OIT composite",
                color, Optional.empty(), depth, OptionalDouble.empty())) {
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("Sampler0", accumulateView, nearest);
            for (int i = 0; i < transmittanceViews.length; i++) {
                pass.setUniform("Coeff" + i, transmittanceViews[i], nearest);
            }
            pass.setUniform("DepthBoundsSampler", depthBoundsView, nearest);
            pass.setPipeline(RenderSystem.getCompiledPipeline(compositeFor(color.texture().getFormat())));
            pass.draw(3, 1, 0, 0);
        }
    }


    private static RenderPipeline compositeFor(GpuFormat format) {
        var vanilla = RenderPipelines.OIT_COMPOSITE;
        var target = vanilla.getColorTargetStates().getFirst();
        if (target == null || target.format() == format) {
            return vanilla;
        }
        return COMPOSITES.computeIfAbsent(format, f -> vanilla.toBuilder()
                .withLocation(Identifier.fromNamespaceAndPath("ldlib2", "pipeline/scene_oit_composite_" + f.name().toLowerCase()))
                .withColorTargetState(0, new ColorTargetState(target.blendFunction(), f, target.writeMask()))
                .build());
    }

    public GpuTextureView accumulate() {
        return accumulateView;
    }

    /** The bounds before the cull, as the composite reads them. */
    public GpuTextureView depthBounds() {
        return depthBoundsView;
    }

    public GpuTextureView[] transmittance() {
        return transmittanceViews.clone();
    }

    @Override
    public void close() {
        depthBoundsView.close();
        depthBoundsCulledView.close();
        accumulateView.close();
        for (GpuTextureView view : transmittanceViews) {
            view.close();
        }
        depthBounds.close();
        depthBoundsCulled.close();
        accumulate.close();
        for (GpuTexture texture : transmittance) {
            texture.close();
        }
    }
}
