#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

uniform sampler2D Sampler2;

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
// Unused here, present only so the raster and distance field paths share one vertex format.
layout(location = 3) in ivec2 UV1;
layout(location = 4) in ivec2 UV2;

layout(location = 0) out vec2 texCoord0;
layout(location = 1) out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    texCoord0 = UV0;
    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
}
