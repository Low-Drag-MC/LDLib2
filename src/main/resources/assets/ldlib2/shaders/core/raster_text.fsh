#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:oit.glsl>

uniform sampler2D Sampler0;

layout(location = 0) in vec2 texCoord0;
layout(location = 1) in vec4 vertexColor;

// The alpha-only phases of order-independent transparency write no colour at all.
#ifndef OIT_ALPHA_ONLY
layout(location = 0) out vec4 fragColor;
#endif

void main() {
    // The atlas already holds the glyph rasterized at the size it is being drawn at, so the coverage is used
    // straight as alpha. No distance field, no derivatives, nothing to reconstruct.
    //
    // Deliberately not vanilla's rendertype_text_intensity, which multiplies rgb by the coverage as well and
    // therefore darkens edges. Matching the distance field shader's plain alpha keeps the two paths
    // comparable, so a glyph looks the same whichever atlas it came from.
    float coverage = texture(Sampler0, texCoord0).r;

    vec4 color = vec4(vertexColor.rgb, vertexColor.a * coverage) * ColorModulator;
    if (color.a < 0.01) {
        discard;
    }

    // Order-independent transparency, when the game's improved transparency is on and this text is
    // drawn in the world: see the OIT_* pipeline sets in LDLibRenderPipelines.
    #ifdef OIT_ALPHA_ONLY
    executeAlphaOnlyPhase(gl_FragCoord.z, color.a);
    #else
    #ifdef OIT_ACCUMULATE
    color = sampleColorForAccumulation(color);
    #endif
    fragColor = color;
    #endif
}
