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
in ivec4 RectParams;  // halfW*8, halfH*8, border*8, margin*8
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

    // Derive local position from vertex index within quad. The quad reaches past the shape by the margin,
    // so the anti-aliased edge has pixels to fade out on.
    vec2 quadHalf = halfSize + vec2(float(RectParams.w) / 8.0);
    int vid = gl_VertexID % 4;
    float lx = (vid < 2) ? -quadHalf.x : quadHalf.x;
    float ly = (vid == 0 || vid == 3) ? -quadHalf.y : quadHalf.y;
    vLocalPos = vec2(lx, ly);
}
