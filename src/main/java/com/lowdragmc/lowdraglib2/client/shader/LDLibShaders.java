package com.lowdragmc.lowdraglib2.client.shader;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.lwjgl.opengl.GL;

public class LDLibShaders {

	/**
	 * the vertex format for HSB color, three four of float
	 */
//	public static final VertexFormatElement HSB_Alpha = VertexFormatElement.register(VertexFormatElement.findNextId(), 0, VertexFormatElement.Type.FLOAT, false, 4);

	public static VertexFormat HSB_VERTEX_FORMAT = VertexFormat.builder(0)
			.addAttribute("Position", GpuFormat.RGB32_FLOAT)
			.addAttribute("HSB_ALPHA", GpuFormat.RGBA32_FLOAT)
			.build();

	/**
	 * Byte offset of the custom {@code HSB_ALPHA} attribute within {@link #HSB_VERTEX_FORMAT}.
	 * In 26.2 custom vertex attributes are written directly at {@code vertexPointer + offset}
	 * (see {@code BufferBuilderAccessor#getVertexPointer}), since {@code BufferBuilder} only
	 * knows the 7 standard element names.
	 */
	public static final int HSB_ALPHA_OFFSET = HSB_VERTEX_FORMAT.getElement("HSB_ALPHA").offset();

	/**
	 * Vertex format elements for analytic rounded-rect rendering.
	 * RectParams: (halfW*8, halfH*8, border*8, 0) as SHORT×4
	 * Radius: (rTL*8, rTR*8, rBR*8, rBL*8) as SHORT×4
	 */
//	public static final VertexFormatElement RECT_PARAMS = VertexFormatElement.register(
//			VertexFormatElement.findNextId(), 0, VertexFormatElement.Type.SHORT, false, 4);
//
//	public static final VertexFormatElement RECT_RADIUS = VertexFormatElement.register(
//			VertexFormatElement.findNextId(), 0, VertexFormatElement.Type.SHORT, false, 4);

	public static final VertexFormat ROUNDED_RECT_FORMAT = VertexFormat.builder(0)
			.addAttribute("Position", GpuFormat.RGB32_FLOAT)
			.addAttribute("Color", GpuFormat.RGBA8_UNORM)
			.addAttribute("RectParams", GpuFormat.RGBA16_SINT)
			.addAttribute("Radius", GpuFormat.RGBA16_SINT)
			.build();

	/** Byte offsets of the custom rounded-rect attributes within {@link #ROUNDED_RECT_FORMAT}. */
	public static final int RECT_PARAMS_OFFSET = ROUNDED_RECT_FORMAT.getElement("RectParams").offset();
	public static final int RECT_RADIUS_OFFSET = ROUNDED_RECT_FORMAT.getElement("Radius").offset();

	/**
	 * Fixed point scale for the SDF tuning carried in {@link #SDF_TEXT_FORMAT}'s UV1 channel.
	 * <p>
	 * Sharpness tops out at 4.0 and weight at +-0.5 (see {@code LDLibClientConfig}), so 4096 keeps both well
	 * inside a signed short while leaving far more precision than either value is ever tuned to.
	 * <p>
	 * Kept in sync by hand with the matching literal in {@code assets/ldlib2/shaders/core/sdf_text.vsh}.
	 */
	public static final float SDF_PARAM_SCALE = 4096f;

	/**
	 * The vertex format behind every LDLib text pipeline, SDF and raster alike (the raster shader ignores UV1;
	 * one shared format keeps {@code LDBakedGlyph} single-path). Byte for byte it is
	 * {@link net.minecraft.client.renderer.RenderPipelines}' entity format without {@code Normal} - vanilla's own
	 * text format has no UV1 at all - with the overlay channel carrying the SDF tuning (sharpness, weight).
	 * <p>
	 * There is no hook to bind a custom uniform buffer for a glyph draw: the GUI renderer owns the render pass
	 * and {@link net.minecraft.client.renderer.state.gui.GuiElementRenderState} only exposes the pipeline, the
	 * textures and the vertices. UV1 is used rather than a custom attribute (the {@code HSB_ALPHA} /
	 * {@code RectParams} pattern above) because a custom attribute can only be written by poking at a
	 * {@code BufferBuilder}'s vertex pointer, and {@code LDBakedGlyph} also renders through vanilla {@code Font}
	 * into a {@code MultiBufferSource}, where the consumer may be a wrapper ({@code VertexMultiConsumer},
	 * {@code SheetedDecalTextureGenerator}). There the cast silently no-ops, the attribute is never written and
	 * the vertex is rejected for leaving a declared element unfilled. {@code VertexConsumer#setUv1} is on the
	 * interface, so it survives wrapping.
	 */
	public static final VertexFormat SDF_TEXT_FORMAT = VertexFormat.builder(0)
			.addAttribute(DefaultVertexFormat.POSITION_SEMANTIC_NAME, GpuFormat.RGB32_FLOAT)
			.addAttribute(DefaultVertexFormat.COLOR_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM)
			.addAttribute(DefaultVertexFormat.UV0_SEMANTIC_NAME, GpuFormat.RG32_FLOAT)
			.addAttribute(DefaultVertexFormat.UV1_SEMANTIC_NAME, GpuFormat.RG16_SINT)
			.addAttribute(DefaultVertexFormat.UV2_SEMANTIC_NAME, GpuFormat.RG16_SINT)
			.build();

	/**
	 * @deprecated OpenGL-only, and not merely in the sense of reporting {@code false} elsewhere:
	 *             {@code GL.getCapabilities()} throws outright when the game is running on Vulkan,
	 *             which 26.2 allows. There is no backend-neutral equivalent — capabilities are
	 *             expressed through {@code RenderSystem.getDevice().getDeviceInfo()} now — and the
	 *             compute/SSBO machinery this guarded was removed, so nothing calls it.
	 */
	@Deprecated(since = "26.2.2.35", forRemoval = true)
	public static boolean supportComputeShader() {
		return GL.getCapabilities().GL_ARB_compute_shader;
	}

	/**
	 * @deprecated See {@link #supportComputeShader()} — same reason, same fate.
	 */
	@Deprecated(since = "26.2.2.35", forRemoval = true)
	public static boolean supportSSBO() {
		return GL.getCapabilities().GL_ARB_shader_storage_buffer_object;
	}

}
