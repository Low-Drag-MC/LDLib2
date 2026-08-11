package com.lowdragmc.lowdraglib2.client.window;

import com.lowdragmc.lowdraglib2.utils.Scope;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GLCapabilities;

/**
 * Makes another GL context current for the length of a try-with-resources block, and puts the
 * previous one back.
 *
 * <p><b>This is the only place in the library that may call {@code glfwMakeContextCurrent}</b>, and
 * the rule that goes with it is absolute: no {@code RenderSystem} or {@code GlStateManager} call may
 * happen inside the scope. Those classes keep static Java-side mirrors of GL state, and those
 * mirrors describe Minecraft's context. GL state is per-context, so touching the other context
 * directly leaves Minecraft's mirrors correct — but routing a call through {@code RenderSystem}
 * while the other context is current corrupts them, and nothing detects it:
 * {@code RenderSystem.assertOnRenderThread} only checks the thread, and the thread is right.
 *
 * <p>Two subtleties that look like they can be skipped and cannot:
 * <ul>
 *   <li>LWJGL caches {@link GLCapabilities} per thread, not per context. Both contexts are current
 *       on the same thread at different times, so the cache does not follow the switch and has to be
 *       swapped by hand. With a single graphics driver the two capability objects happen to be
 *       interchangeable and skipping this appears to work; on a machine with two it can crash.</li>
 *   <li>Objects written in one context are only guaranteed visible to another after the writer
 *       flushes, hence the {@link GL11C#glFlush()} before the switch.</li>
 * </ul>
 */
public final class GlContextScope implements Scope {
    private final long previousHandle;
    private final GLCapabilities previousCapabilities;

    private GlContextScope(long previousHandle, GLCapabilities previousCapabilities) {
        this.previousHandle = previousHandle;
        this.previousCapabilities = previousCapabilities;
    }

    /**
     * Switches to {@code handle}'s context.
     *
     * @param capabilities the capabilities created for that context, see
     *                     {@link #createCapabilitiesFor(long)}
     */
    public static GlContextScope enter(long handle, GLCapabilities capabilities) {
        RenderSystem.assertOnRenderThread();
        var previousHandle = GLFW.glfwGetCurrentContext();
        var previousCapabilities = GL.getCapabilities();
        // Publish this context's writes before the other one reads the shared textures.
        GL11C.glFlush();
        GLFW.glfwMakeContextCurrent(handle);
        GL.setCapabilities(capabilities);
        return new GlContextScope(previousHandle, previousCapabilities);
    }

    /**
     * Makes {@code handle} current once to build its {@link GLCapabilities}, then restores the
     * caller's context. Call exactly once per window, before the first {@link #enter}.
     *
     * <p>Also turns vsync off for the new context: swap interval is per-context, and leaving it at
     * the driver default means the game waits for two vertical blanks per frame instead of one and
     * its framerate halves.
     */
    public static GLCapabilities createCapabilitiesFor(long handle) {
        RenderSystem.assertOnRenderThread();
        var previousHandle = GLFW.glfwGetCurrentContext();
        var previousCapabilities = GL.getCapabilities();
        GLFW.glfwMakeContextCurrent(handle);
        try {
            var capabilities = GL.createCapabilities();
            GLFW.glfwSwapInterval(0);
            return capabilities;
        } finally {
            GLFW.glfwMakeContextCurrent(previousHandle);
            GL.setCapabilities(previousCapabilities);
        }
    }

    @Override
    public void close() {
        GLFW.glfwMakeContextCurrent(previousHandle);
        GL.setCapabilities(previousCapabilities);
    }
}
