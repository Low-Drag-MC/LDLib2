#version 150

uniform sampler2D Sampler0;
uniform vec4 UVBounds;  // [uStart, vStart, uEnd, vEnd]
uniform int WrapMode;   // 0=CLAMP, 1=REPEAT, 2=MIRRORED_REPEAT

uniform vec4 ColorModulator;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

vec2 computeUV(vec2 uv, vec4 bounds, int mode) {
    float u = (uv.x - bounds.x) / (bounds.z - bounds.x);
    float v = (uv.y - bounds.y) / (bounds.w - bounds.y);

    // wrap UV coordinates based on the wrap mode
    if (mode == 1) {         // REPEAT
        u = fract(u);
        v = fract(v);
    } else if (mode == 2) {  // MIRRORED_REPEAT
        u = fract(u * 0.5) * 2.0;
        v = fract(v * 0.5) * 2.0;
        if (u > 1.0) u = 2.0 - u;
        if (v > 1.0) v = 2.0 - v;
    } else {                 // CLAMP
        u = clamp(u, 0.0, 1.0);
        v = clamp(v, 0.0, 1.0);
    }

    // map UV coordinates to the texture bounds
    return vec2(
    bounds.x + u * (bounds.z - bounds.x),
    bounds.y + v * (bounds.w - bounds.y)
    );
}

void main() {
    vec2 finalUV = computeUV(texCoord0, UVBounds, WrapMode);

    vec4 color = texture(Sampler0, finalUV) * vertexColor;
    if (color.a < 0.1) {
        discard;
    }
    fragColor = color * ColorModulator;
}
