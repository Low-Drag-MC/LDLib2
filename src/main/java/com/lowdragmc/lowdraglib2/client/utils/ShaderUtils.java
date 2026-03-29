package com.lowdragmc.lowdraglib2.client.utils;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.client.shader.LDLibShaders;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL43;

/**
 * @author KilaBash
 * @date 2022/12/11
 * @implNote ShaderUtils
 * @port ELB_GG 
 * @date_port 2026/03/29 
 * @port_to fabric
 */
public class ShaderUtils {

    /**
     * fast blit - fast copy a fbo to another one (color component)
     * @param from fbo
     * @param to fbo
     */
    public static void fastBlit(RenderTarget from, RenderTarget to) {
        RenderSystem.assertOnRenderThread();
        GlStateManager._colorMask(true, true, true, true);
        GlStateManager._disableDepthTest();
        GlStateManager._depthMask(false);

        to.bindWrite(true);

        LDLibShaders.getBlitShader().setSampler("DiffuseSampler", from.getColorTextureId());

        LDLibShaders.getBlitShader().apply();
        GlStateManager._enableBlend();
        RenderSystem.defaultBlendFunc();

        Tesselator tesselator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        bufferbuilder.addVertex(-1, 1, 0);
        bufferbuilder.addVertex(-1, -1, 0);
        bufferbuilder.addVertex(1, -1, 0);
        bufferbuilder.addVertex(1, 1, 0);
        BufferUploader.draw(bufferbuilder.buildOrThrow());
        LDLibShaders.getBlitShader().clear();

        GlStateManager._depthMask(true);
        GlStateManager._colorMask(true, true, true, true);
        GlStateManager._enableDepthTest();
    }

    private static final boolean DEBUG_LABEL_AVAILABLE = GL.getCapabilities().GL_KHR_debug;

    public static void warpGLDebugLabel(String message, Runnable block) {
        if (DEBUG_LABEL_AVAILABLE && Platform.isDevEnv()) {
            GL43.glPushDebugGroup(GL43.GL_DEBUG_SOURCE_APPLICATION, 0, message);
            block.run();
            GL43.glPopDebugGroup();
        } else {
            block.run();
        }
    }
}
