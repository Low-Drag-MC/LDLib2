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

	/**
	 * Fixed point scale for the SDF tuning carried in {@link #SDF_TEXT_FORMAT}'s UV1 channel.
	 * <p>
	 * Sharpness tops out at 4.0 and weight at +-0.5 (see {@code LDLibClientConfig}), so 4096 keeps both well
	 * inside a signed short while leaving far more precision than either value is ever tuned to.
	 */
	public static final float SDF_PARAM_SCALE = 4096f;

	/**
	 * Vanilla's text format with the overlay channel repurposed to carry the SDF tuning (sharpness, weight).
	 * <p>
	 * 26.1 has no hook to bind a custom uniform buffer for a glyph draw: the GUI renderer owns the render pass
	 * and {@link net.minecraft.client.renderer.state.gui.GuiElementRenderState} only exposes the pipeline, the
	 * textures and the vertices. UV1 is used rather than a newly registered element because
	 * {@code VertexConsumer#setUv1} is part of the interface, so glyph quads keep working through wrapping
	 * consumers; a custom element can only be written by poking at a {@code BufferBuilder} directly, and a
	 * vertex that leaves one unfilled is rejected outright.
	 */
	public static final VertexFormat SDF_TEXT_FORMAT = VertexFormat.builder()
			.add("Position", POSITION)
			.add("Color", VertexFormatElement.COLOR)
			.add("UV0", VertexFormatElement.UV0)
			.add("UV1", VertexFormatElement.UV1)
			.add("UV2", VertexFormatElement.UV2)
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
