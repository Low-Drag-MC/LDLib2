#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;          // the vertex's position relative to the shape's centre
layout(location = 3) in ivec4 RectParams;  // halfW*8, halfH*8, border*8, 0
layout(location = 4) in ivec4 Radius;      // rTL*8, rTR*8, rBR*8, rBL*8

layout(location = 0) out vec2 vLocalPos;
layout(location = 1) out vec4 vColor;
layout(location = 2) out vec2 vHalfSize;
layout(location = 3) out float vBorder;
layout(location = 4) out vec4 vRadius;

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
