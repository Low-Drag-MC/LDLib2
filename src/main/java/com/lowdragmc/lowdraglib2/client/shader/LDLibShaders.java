package com.lowdragmc.lowdraglib2.client.shader;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import org.lwjgl.opengl.GL;

import static com.mojang.blaze3d.vertex.VertexFormatElement.POSITION;

public class LDLibShaders {

	/**
	 * the vertex format for HSB color, three four of float
	 */
	public static final VertexFormatElement HSB_Alpha = VertexFormatElement.register(VertexFormatElement.findNextId(), 0, VertexFormatElement.Type.FLOAT, false, 4);

	public static VertexFormat HSB_VERTEX_FORMAT = VertexFormat.builder()
			.add("Position", POSITION)
			.add("HSB_ALPHA", HSB_Alpha)
			.build();

	/**
	 * Vertex format elements for analytic rounded-rect rendering.
	 * RectParams: (halfW*8, halfH*8, border*8, 0) as SHORT×4
	 * Radius: (rTL*8, rTR*8, rBR*8, rBL*8) as SHORT×4
	 */
	public static final VertexFormatElement RECT_PARAMS = VertexFormatElement.register(
			VertexFormatElement.findNextId(), 0, VertexFormatElement.Type.SHORT, false, 4);

	public static final VertexFormatElement RECT_RADIUS = VertexFormatElement.register(
			VertexFormatElement.findNextId(), 0, VertexFormatElement.Type.SHORT, false, 4);

	public static final VertexFormat ROUNDED_RECT_FORMAT = VertexFormat.builder()
			.add("Position", POSITION)
			.add("Color", VertexFormatElement.COLOR)
			.add("RectParams", RECT_PARAMS)
			.add("Radius", RECT_RADIUS)
			.build();

	@Deprecated
	public static boolean supportComputeShader() {
		return GL.getCapabilities().GL_ARB_compute_shader;
	}

	@Deprecated
	public static boolean supportSSBO() {
		return GL.getCapabilities().GL_ARB_shader_storage_buffer_object;
	}

}
