#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D Mask;

uniform float Opacity;   // 0..1
uniform float HasMask;   // 0 or 1

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);

    // 读取蒙版（优先用 alpha，或者用 r 通道）
    float m = 1.0;
    if (HasMask > 0.5) {
        m = texture(Mask, texCoord).a; // 或 .r
    }

    // 非预乘：只缩 alpha
    // color.rgb 可选乘以 Opacity（通常只缩 alpha）
    color.a *= (m * Opacity);

    fragColor = color;
}