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
in ivec4 RectParams;  // halfW*8, halfH*8, border*8, cornerId(0..3)
in ivec4 Radius;      // rTL*8, rTR*8, rBR*8, rBL*8

out vec2 vLocalPos;
out vec4 vColor;
out vec2 vHalfSize;
out float vBorder;
out vec4 vRadius;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vec2 halfSize = vec2(float(RectParams.x), float(RectParams.y)) / 8.0;
    vHalfSize = halfSize;
    vBorder = float(RectParams.z) / 8.0;
    vRadius = vec4(Radius) / 8.0;
    vColor = Color;

    // Derive local position from the per-vertex corner id (RectParams.w). We can't use
    // gl_VertexID % 4: the 26.2 GUI packs all draws into one shared buffer and draws each with a
    // baseVertex offset, so gl_VertexID is shifted by a per-frame-varying baseVertex.
    int vid = RectParams.w;
    float lx = (vid < 2) ? -halfSize.x : halfSize.x;
    float ly = (vid == 0 || vid == 3) ? -halfSize.y : halfSize.y;
    vLocalPos = vec2(lx, ly);
}
