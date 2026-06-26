package com.lowdragmc.lowdraglib2.client.shader;

import com.mojang.blaze3d.GpuFormat;
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

	@Deprecated
	public static boolean supportComputeShader() {
		return GL.getCapabilities().GL_ARB_compute_shader;
	}

	@Deprecated
	public static boolean supportSSBO() {
		return GL.getCapabilities().GL_ARB_shader_storage_buffer_object;
	}

}
