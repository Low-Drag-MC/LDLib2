package com.lowdragmc.lowdraglib2.client.shader.management;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.shader.Shaders;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * Shader Storage Buffer Object
 */
@OnlyIn(Dist.CLIENT)
public class ShaderSSBO {

	public final int id;
	private boolean inValid = false;

	private static final int BUFFER_TYPE = GL43.GL_SHADER_STORAGE_BUFFER;

	public ShaderSSBO() {
		if (!Shaders.supportSSBO()) {
			String errorMessage = "need support for GL_ARB_shader_storage_buffer_object";
			LDLib2.LOGGER.error(errorMessage);
			Minecraft.getInstance().delayCrash(new CrashReport(errorMessage, new IllegalStateException(errorMessage)));
		}
		this.id = GL43.glGenBuffers();
	}

	public void close() {
		if (!inValid) {
			GL30.glDeleteBuffers(id);
			inValid = true;
		}else {
			LDLib2.LOGGER.error("try closing an already closed ShaderStorageBufferObject");
		}
	}

	public void bindBuffer() {
		if (!inValid){
			GL30.glBindBuffer(BUFFER_TYPE, this.id);
		} else {
			LDLib2.LOGGER.error("try to use an already close ShaderStorageBufferObject");
		}
	}

	public void unBindBuffer() {
		GL30.glBindBuffer(BUFFER_TYPE, 0);
	}

	/**
	 *
	 * @param programIndex the name of the program
	 * @param storageBlockIndex the storage index in the shader
	 * @param index the globally shared binding index
	 */
	public void bindToShader(int programIndex, int storageBlockIndex, int index) {
		bindBuffer();
		GL43.glShaderStorageBlockBinding(programIndex, storageBlockIndex, index);
		unBindBuffer();
	}

	/**
	 * @param index the globally shared binding index
	 */
	public void bindIndex(int index) {
		bindBuffer();
		GL43.glBindBufferBase(BUFFER_TYPE, index, this.id);
		unBindBuffer();
	}

	public void getSubData(long offset, float[] data) {
		bindBuffer();
		GL43.glGetBufferSubData(BUFFER_TYPE, offset, data);
		unBindBuffer();
	}


	public void createBufferData(long size, int mode) {
		bindBuffer();
		GL30.glBufferData(BUFFER_TYPE, size, mode);
		unBindBuffer();
	}

	public void createBufferData(FloatBuffer data, int mode) {
		bindBuffer();
		GL30.glBufferData(BUFFER_TYPE, data, mode);
		unBindBuffer();
	}

	public void bufferSubData(long offset, FloatBuffer data) {
		bindBuffer();
		GL30.glBufferSubData(BUFFER_TYPE, offset, data);
		unBindBuffer();
	}

	public void bufferSubData(long offset, ByteBuffer data) {
		bindBuffer();
		GL30.glBufferSubData(BUFFER_TYPE, offset, data);
		unBindBuffer();
	}

	public void bufferSubData(long offset, float[] data) {
		bindBuffer();
		GL30.glBufferSubData(BUFFER_TYPE, offset, data);
		unBindBuffer();
	}

	public void bufferSubData(long offset, int[] data) {
		bindBuffer();
		GL30.glBufferSubData(BUFFER_TYPE, offset, data);
		unBindBuffer();
	}

}
