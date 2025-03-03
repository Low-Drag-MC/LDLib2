package com.lowdragmc.lowdraglib.client.shader.management;

import com.google.common.base.Charsets;
import com.lowdragmc.lowdraglib.LDLib;
import com.mojang.blaze3d.platform.GlStateManager;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.io.IOUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class Shader {
    public final ShaderType shaderType;
    public final String source;
    private int shaderId;
    private boolean isCompiled;

    public Shader(ShaderType type, String source) {
        this.shaderType = type;
        this.source = source;
        this.shaderId = shaderType.createShader();
        if (this.shaderId == 0) {
            LDLib.LOGGER.error("GL Shader Allocation Fail!");
            throw new RuntimeException("GL Shader Allocation Fail!");
        }
    }

    public void attachShader(ShaderProgram program) {
        if (!isCompiled) compileShader();
        GlStateManager.glAttachShader(program.programId, this.shaderId);
    }

    public void deleteShader() {
        if (shaderId == 0) return;
        GlStateManager.glDeleteShader(this.shaderId);
        shaderId = 0;
    }

    public Shader compileShader() {
        if (!this.isCompiled && shaderId != 0) {
            Shader.setShaderSourceHackForAmd(this.shaderId, this.source);
            GL20.glCompileShader(this.shaderId);
            if (GL20.glGetShaderi(this.shaderId, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
                int maxLength = GL20.glGetShaderi(this.shaderId, GL20.GL_INFO_LOG_LENGTH);
                String error = String.format("Unable to compile %s shader object:\n%s", this.shaderType.name(), GL20.glGetShaderInfoLog(this.shaderId, maxLength));
                LDLib.LOGGER.error(error);
            }
            this.isCompiled = true;
        }
        return this;
    }

    // Evil hack for AMD
    private static void setShaderSourceHackForAmd(int shaderId, String source) {
        // Load shader source as bytes
        byte[] bs = source.getBytes(Charsets.UTF_8);
        ByteBuffer byteBuffer = MemoryUtil.memAlloc(bs.length + 1);
        byteBuffer.put(bs);
        byteBuffer.put((byte)0);
        byteBuffer.flip();

        try {
            MemoryStack memoryStack = MemoryStack.stackPush();
            try {
                // Use unsafe shader load method for loading because safe version fails on AMD GPUs.
                PointerBuffer pointerBuffer = memoryStack.mallocPointer(1);
                pointerBuffer.put(byteBuffer);
                GL20C.nglShaderSource(shaderId, 1, pointerBuffer.address0(), 0L);
            } catch (Throwable err1) {
                if (memoryStack != null) {
                    try {
                        memoryStack.close();
                    } catch (Throwable err2) {
                        err1.addSuppressed(err2);
                    }
                }
                throw err1;
            }

            if (memoryStack != null) {
                memoryStack.close();
            }
        } finally {
            MemoryUtil.memFree(byteBuffer);
        }
    }

    public static Shader loadShader(ShaderType type, String rawShader) {
        return new Shader(type, rawShader).compileShader();
    }

    public static Shader loadShader(ShaderType type, ResourceLocation resourceLocation) throws IOException {
        var maybeResource = Minecraft.getInstance().getResourceManager().getResource(resourceLocation);
        if (maybeResource.isPresent()) {
            var resource = maybeResource.get();
            InputStream stream = resource.open();
            StringBuilder stringBuilder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line;
            while((line = reader.readLine()) != null) {
                stringBuilder.append(line).append('\n');
            }
            stream.close();
            IOUtils.closeQuietly(stream);
            return loadShader(type, stringBuilder.toString());
        }
        throw new IOException("found no resource with ID " + resourceLocation);
    }

    public enum ShaderType {
        VERTEX("vertex", ".vsh", GL20.GL_VERTEX_SHADER, GL20::glCreateShader),
        FRAGMENT("fragment", ".fsh", GL20.GL_FRAGMENT_SHADER, GL20::glCreateShader),
        COMPUTE("compute", ".comp", 0x91B9, id -> GL43.glCreateShader(GL43.GL_COMPUTE_SHADER));

        public final String shaderName;
        public final String shaderExtension;
        public final int shaderMode;
        public final Int2IntFunction shaderCreator;

        ShaderType(String shaderNameIn, String shaderExtensionIn, int shaderModeIn, Int2IntFunction shaderCreatorIn) {
            this.shaderName = shaderNameIn;
            this.shaderExtension = shaderExtensionIn;
            this.shaderMode = shaderModeIn;
            this.shaderCreator = shaderCreatorIn;
        }

        public int createShader() {
            return shaderCreator.get(this.shaderMode);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) return true;
        if (obj instanceof Shader shader) {
            return Objects.equals(shader.source, this.source) && shader.shaderType == this.shaderType;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(shaderType, source);
    }
}
