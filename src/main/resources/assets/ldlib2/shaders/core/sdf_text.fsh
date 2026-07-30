#version 150

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
// AA width multiplier, 1.0 gives a roughly one pixel wide edge transition
uniform float Sharpness;
// Stem thickening in pixels, useful to compensate for small font sizes looking washed out
uniform float Weight;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

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
    float coverage = clamp((sd - EDGE) / aa * Sharpness + 0.5 + Weight, 0.0, 1.0);

    vec4 color = vec4(vertexColor.rgb, vertexColor.a * coverage) * ColorModulator;
    if (color.a < 0.01) {
        discard;
    }
    fragColor = color;
}
