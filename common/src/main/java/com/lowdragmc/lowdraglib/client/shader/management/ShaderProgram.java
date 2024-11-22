package com.lowdragmc.lowdraglib.client.shader.management;

import com.lowdragmc.lowdraglib.client.shader.uniform.IUniformCallback;
import com.lowdragmc.lowdraglib.client.shader.uniform.UniformCache;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class ShaderProgram {

	public final int programId;
	public final Set<Shader> shaders;
	public final UniformCache uniformCache;
	public final LinkedHashMap<String, Integer> samplers;
	private boolean unLinked;
	private IUniformCallback globalUniform;

	public ShaderProgram() {
		this.programId = GL20.glCreateProgram();
		this.shaders = new ReferenceOpenHashSet<>();
		this.samplers = new LinkedHashMap<>();
		if (this.programId == 0) {
			throw new IllegalStateException("Unable to create ShaderProgram.");
		}
		this.uniformCache = new UniformCache(this.programId);
	}

	public ShaderProgram attach(Shader loader) {
		if (loader == null) return this;
		if (this.shaders.contains(loader)) {
			throw new IllegalStateException(String.format("Unable to attach Shader as it is already attached:\n%s", loader.source));
		}
		this.shaders.add(loader);
		loader.attachShader(this);
		this.unLinked = true;
		return this;
	}

	public void use(IUniformCallback callback) {
		this.use();
		callback.apply(uniformCache);
	}

	public void setGlobalUniform(IUniformCallback globalUniform) {
		this.globalUniform = globalUniform;
	}

	public void bindTexture(String samplerName, int textureId) {
		if (textureId > 0) {
			samplers.put(samplerName, textureId);
		} else {
			samplers.remove(samplerName);
		}
	}

	public void bindTexture(String samplerName, ResourceLocation resourceLocation) {
		if (resourceLocation == null) {
			bindTexture(samplerName, 0);
			return;
		}
		AbstractTexture abstracttexture = Minecraft.getInstance().getTextureManager().getTexture(resourceLocation);
		int textureId = abstracttexture.getId();
		bindTexture(samplerName, textureId);
	}

	public void linkProgram() {
		if (unLinked) {
			this.uniformCache.invalidate();
			GL20.glLinkProgram(programId);
			if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
				throw new RuntimeException(String.format("ShaderProgram validation has failed!\n%s", GL20.glGetProgramInfoLog(programId, GL20.glGetProgrami(programId, 35716))));
			}
			this.unLinked = false;
		}
	}

	public void use() {
		if (unLinked) {
			linkProgram();
		}
		GL20.glUseProgram(programId);
		if (!samplers.isEmpty()) {
			int i = 0;
			for (Map.Entry<String, Integer> entry : samplers.entrySet()) {
				RenderSystem.activeTexture(GL13.GL_TEXTURE0 + i);
				RenderSystem.bindTexture(entry.getValue());
				uniformCache.glUniform1I(entry.getKey(), i);
				i++;
			}
		}
		if (globalUniform != null) {
			globalUniform.apply(uniformCache);
		}
	}

	public void release() {
		if (!samplers.isEmpty()) {
			for (int i = 0; i < samplers.size(); i++) {
				RenderSystem.activeTexture(GL13.GL_TEXTURE0 + i);
				RenderSystem.bindTexture(0);
			}
		}
		GL20.glUseProgram(0);
	}

	public void delete() {
		if (this.programId != 0) {
			GL20.glDeleteProgram(programId);
		}
	}

}
