#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in ivec4 RectParams;  // halfW*8, halfH*8, border*8, cornerId(0..3)
layout(location = 3) in ivec4 Radius;      // rTL*8, rTR*8, rBR*8, rBL*8

layout(location = 0) out vec2 vLocalPos;
layout(location = 1) out vec4 vColor;
layout(location = 2) out vec2 vHalfSize;
layout(location = 3) out float vBorder;
layout(location = 4) out vec4 vRadius;

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
