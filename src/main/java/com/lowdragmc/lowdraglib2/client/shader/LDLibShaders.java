package com.lowdragmc.lowdraglib2.client.shader;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.shader.management.Shader;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import lombok.Getter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.lwjgl.opengl.GL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.mojang.blaze3d.vertex.VertexFormatElement.POSITION;

@OnlyIn(Dist.CLIENT)
public class LDLibShaders {
	public static final Pattern REGEX_VERSION = Pattern.compile(
			"(#(?:/\\*(?:[^*]|\\*+[^*/])*\\*+/|\\h)*version(?:/\\*(?:[^*]|\\*+[^*/])*\\*+/|\\h)*(\\d+))\\b"
	);
	public static Program.Type GEOMETRY_TYPE;

	private static final List<Runnable> reloadListeners = new ArrayList<>();
	public static Shader IMAGE_F;
	public static Shader IMAGE_V;
	public static Shader GUI_IMAGE_V;
	public static Shader SCREEN_V;
	public static Shader ROUND_F;
	public static Shader PANEL_BG_F;
	public static Shader ROUND_BOX_F;
	public static Shader PROGRESS_ROUND_BOX_F;
	public static Shader FRAME_ROUND_BOX_F;
	public static Shader ROUND_LINE_F;

	public static void init() {
		IMAGE_F = load(Shader.ShaderType.FRAGMENT, LDLib2.id("image"));
		IMAGE_V = load(Shader.ShaderType.VERTEX, LDLib2.id("image"));
		GUI_IMAGE_V = load(Shader.ShaderType.VERTEX, LDLib2.id("gui_image"));
		SCREEN_V = load(Shader.ShaderType.VERTEX, LDLib2.id("screen"));
		ROUND_F = load(Shader.ShaderType.FRAGMENT, LDLib2.id("round"));
		PANEL_BG_F = load(Shader.ShaderType.FRAGMENT, LDLib2.id("panel_bg"));
		ROUND_BOX_F = load(Shader.ShaderType.FRAGMENT, LDLib2.id("round_box"));
		PROGRESS_ROUND_BOX_F = load(Shader.ShaderType.FRAGMENT, LDLib2.id("progress_round_box"));
		FRAME_ROUND_BOX_F = load(Shader.ShaderType.FRAGMENT, LDLib2.id("frame_round_box"));
		ROUND_LINE_F = load(Shader.ShaderType.FRAGMENT, LDLib2.id("round_line"));
	}

	public static Map<ResourceLocation, Shader> CACHE = new HashMap<>();

	public static void addReloadListener(Runnable runnable) {
		reloadListeners.add(runnable);
	}

	public static void reload() {
		for (Shader shader : CACHE.values()) {
			if (shader != null) {
				shader.deleteShader();
			}
		}
		CACHE.clear();
		init();
		reloadListeners.forEach(Runnable::run);
	}

	public static Shader load(Shader.ShaderType shaderType, ResourceLocation resourceLocation) {
		return CACHE.computeIfAbsent(ResourceLocation.fromNamespaceAndPath(resourceLocation.getNamespace(), "shaders/" + resourceLocation.getPath() + shaderType.shaderExtension), key -> {
			try {
				Shader shader = Shader.loadShader(shaderType, key);
				LDLib2.LOGGER.debug("load shader {} resource {} success", shaderType, resourceLocation);
				return shader;
			} catch (IOException e) {
				LDLib2.LOGGER.error("load shader {} resource {} failed", shaderType, resourceLocation);
				LDLib2.LOGGER.error("caused by ", e);
				return IMAGE_F;
			}
		});
	}

	// *** vanilla **//

	@Getter
	private static ShaderInstance particleShader;
	@Getter
	private static ShaderInstance blitShader;
	@Getter
	private static ShaderInstance spriteBlitShader;
	@Getter
	private static ShaderInstance hsbShader;
	@Getter
	private static ShaderInstance graphWireShader;
    @Getter
    private static ShaderInstance visualLayerShader;
    @Getter
    private static ShaderInstance SDFRect;
    @Getter
    private static ShaderInstance guiTexture;

	/**
	 * the vertex format for HSB color, three four of float
	 */
	public static final VertexFormatElement HSB_Alpha = VertexFormatElement.register(VertexFormatElement.findNextId(), 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.COLOR, 4);
	public static final VertexFormatElement AA = VertexFormatElement.register(VertexFormatElement.findNextId(), 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.COLOR, 1);

	public static VertexFormat HSB_VERTEX_FORMAT = VertexFormat.builder()
			.add("Position", POSITION)
			.add("HSB_ALPHA", HSB_Alpha)
			.build();

    public static void registerShaders(RegisterShadersEvent registerShadersEvent) {
		var resourceProvider = registerShadersEvent.getResourceProvider();
		try {
			registerShadersEvent.registerShader(new ShaderInstance(resourceProvider,
							LDLib2.id("particle"), DefaultVertexFormat.PARTICLE),
					shaderInstance -> particleShader = shaderInstance);
			registerShadersEvent.registerShader(new ShaderInstance(resourceProvider,
							LDLib2.id("fast_blit"), DefaultVertexFormat.POSITION),
					shaderInstance -> blitShader = shaderInstance);
            registerShadersEvent.registerShader(new ShaderInstance(resourceProvider,
                            LDLib2.id("visual_layer"), DefaultVertexFormat.POSITION_TEX),
                    shaderInstance -> visualLayerShader = shaderInstance);
			registerShadersEvent.registerShader(new ShaderInstance(resourceProvider,
							LDLib2.id("sprite_blit"), DefaultVertexFormat.POSITION_TEX_COLOR),
					shaderInstance -> spriteBlitShader = shaderInstance);
			registerShadersEvent.registerShader(new ShaderInstance(resourceProvider,
							LDLib2.id("hsb_block"), HSB_VERTEX_FORMAT),
					shaderInstance -> hsbShader = shaderInstance);
			registerShadersEvent.registerShader(new ShaderInstance(resourceProvider,
							LDLib2.id("graph_wire"), DefaultVertexFormat.POSITION_TEX_COLOR),
					shaderInstance -> graphWireShader = shaderInstance);
            registerShadersEvent.registerShader(new ShaderInstance(resourceProvider,
                            LDLib2.id("sdf_rect"), DefaultVertexFormat.POSITION),
                    shaderInstance -> SDFRect = shaderInstance);
            registerShadersEvent.registerShader(new ShaderInstance(resourceProvider,
                            LDLib2.id("gui_texture"), DefaultVertexFormat.POSITION_TEX_COLOR),
                    shaderInstance -> guiTexture = shaderInstance);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    }

	public static boolean supportComputeShader() {
		return GL.getCapabilities().GL_ARB_compute_shader;
	}

	public static boolean supportSSBO() {
		return GL.getCapabilities().GL_ARB_shader_storage_buffer_object;
	}

}
