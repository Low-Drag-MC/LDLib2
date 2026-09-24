#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:oit.glsl>

uniform sampler2D Sampler0;

layout(location = 0) in vec2 texCoord0;
layout(location = 1) in vec4 vertexColor;
// AA width multiplier, 1.0 gives a roughly one pixel wide edge transition
layout(location = 2) in float vSharpness;
// Stem thickening in pixels, useful to compensate for small font sizes looking washed out
layout(location = 3) in float vWeight;

// The alpha-only phases of order-independent transparency write no colour at all.
#ifndef OIT_ALPHA_ONLY
layout(location = 0) out vec4 fragColor;
#endif

// The glyph atlas stores distances with onedge_value = 128, so this is where the outline sits.
const float EDGE = 128.0 / 255.0;

void main() {
    float sd = texture(Sampler0, texCoord0).r;

    // Screen space rate of change of the distance field, which keeps the edge one pixel wide no matter how
    // the text is scaled or transformed.
    //
    // This has to be the true gradient magnitude, not fwidth. fwidth is |dFdx| + |dFdy|, which is correct
    // only for axis aligned edges; on a 45 degree edge it overestimates by a factor of sqrt(2) and smears the
    // transition over 1.4 pixels instead of 1. Straight stems would look sharp while every curve and diagonal
    // looked soft, which reads as the whole glyph being blurry.
    vec2 gradient = vec2(dFdx(sd), dFdy(sd));
    float aa = max(length(gradient), 1e-5);
    float coverage = clamp((sd - EDGE) / aa * vSharpness + 0.5 + vWeight, 0.0, 1.0);

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
