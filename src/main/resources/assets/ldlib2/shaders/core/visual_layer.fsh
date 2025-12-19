#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D Mask;

uniform float Opacity;   // 0..1
uniform float HasMask;   // 0 or 1

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord) * Opacity;

    if (HasMask > 0.5) {
        color *=  texture(Mask, texCoord).r; // 或 .r
    }

    fragColor = color;
}