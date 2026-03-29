package com.lowdragmc.lowdraglib2.client.utils;

import org.lwjgl.opengl.ARBInstancedArrays;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL33;

public class GLUtil {

    /**
     * GL 3.3 core has glVertexAttribDivisor; GL 3.2 exposes the same via GL_ARB_instanced_arrays.
     * Use whichever is available so this works on exact-3.2 contexts (LWJGL debug will crash on a
     * null function pointer otherwise).
     */
    public static void vertexAttribDivisor(int index, int divisor) {
        var caps = GL.getCapabilities();
        if (caps.OpenGL33) {
            GL33.glVertexAttribDivisor(index, divisor);
        } else if (caps.GL_ARB_instanced_arrays) {
            ARBInstancedArrays.glVertexAttribDivisorARB(index, divisor);
        }
    }
}
