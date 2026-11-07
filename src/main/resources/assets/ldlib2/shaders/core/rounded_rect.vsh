#version 330

// Can't moj_import in things used during startup, when resource packs don't exist.
// This is a copy of dynamicimports.glsl and projection.glsl
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec3 Position;
in vec4 Color;
in vec2 UV0;          // the vertex's position relative to the shape's centre
in ivec4 RectParams;  // halfW*8, halfH*8, border*8, 0
in ivec4 Radius;      // rTL*8, rTR*8, rBR*8, rBL*8

out vec2 vLocalPos;
out vec4 vColor;
out vec2 vHalfSize;
out float vBorder;
out vec4 vRadius;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vHalfSize = vec2(float(RectParams.x), float(RectParams.y)) / 8.0;
    vBorder = float(RectParams.z) / 8.0;
    vRadius = vec4(Radius) / 8.0;
    vColor = Color;
    // Carried per vertex rather than derived from gl_VertexID % 4: the 26.2 GUI packs all draws into one shared
    // buffer and draws each with a baseVertex offset, so gl_VertexID is shifted by a per-frame-varying amount.
    // It also reaches past the shape by the quad's margin, so the anti-aliased edge has pixels to fade out on.
    vLocalPos = UV0;
}
