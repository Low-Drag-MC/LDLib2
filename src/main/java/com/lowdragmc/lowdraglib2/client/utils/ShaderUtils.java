package com.lowdragmc.lowdraglib2.client.utils;

import com.lowdragmc.lowdraglib2.Platform;
import com.mojang.blaze3d.vertex.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL43;

/**
 * @author KilaBash
 * @date 2022/12/11
 * @implNote ShaderUtils
 */
@OnlyIn(Dist.CLIENT)
public class ShaderUtils {

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
