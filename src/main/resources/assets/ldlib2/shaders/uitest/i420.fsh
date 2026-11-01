#version 330
#extension GL_ARB_separate_shader_objects : require

// The UI recorder's frame conversion (uitest/record/YuvConverter): a region of Source, cropped and scaled to OutSize,
// written as the bytes of one I420 frame (BT.709, limited range), laid out row after row the way ffmpeg's yuv420p
// expects it in memory. Each fragment is one byte of the R8 target, LayoutWidth bytes to a row.

uniform sampler2D Source;

layout(std140) uniform I420Params {
    vec4 Region;
    ivec2 OutSize;
    int Identity;
    int LayoutWidth;
};

layout(location = 0) out vec4 fragColor;

vec3 texel(ivec2 p) {
    ivec2 size = textureSize(Source, 0);
    p = clamp(p, ivec2(0), size - 1);
    return texelFetch(Source, ivec2(p.x, size.y - 1 - p.y), 0).rgb;
}

vec3 at(vec2 o) {
    if (Identity != 0) return texel(ivec2(Region.xy) + ivec2(o));
    vec2 p = Region.xy + (o + 0.5) * Region.zw / vec2(OutSize) - 0.5;
    ivec2 i = ivec2(floor(p));
    vec2 f = p - vec2(i);
    return mix(mix(texel(i), texel(i + ivec2(1, 0)), f.x),
               mix(texel(i + ivec2(0, 1)), texel(i + ivec2(1, 1)), f.x), f.y);
}

void main() {
    int w = OutSize.x;
    int h = OutSize.y;
    int index = int(gl_FragCoord.y) * LayoutWidth + int(gl_FragCoord.x);
    int lumaSize = w * h;
    int cw = w / 2;
    int chromaSize = cw * (h / 2);
    float value;
    if (index < lumaSize) {
        vec3 c = at(vec2(index % w, index / w));
        value = 16.0 + 219.0 * dot(c, vec3(0.2126, 0.7152, 0.0722));
    } else if (index < lumaSize + 2 * chromaSize) {
        int j = index - lumaSize;
        bool isV = j >= chromaSize;
        if (isV) j -= chromaSize;
        vec2 o = vec2(j % cw, j / cw) * 2.0;
        vec3 c = (at(o) + at(o + vec2(1.0, 0.0)) + at(o + vec2(0.0, 1.0)) + at(o + vec2(1.0, 1.0))) * 0.25;
        value = isV ? 128.0 + 224.0 * dot(c, vec3(0.5, -0.45415, -0.04585))
                    : 128.0 + 224.0 * dot(c, vec3(-0.11457, -0.38543, 0.5));
    } else {
        value = 0.0;
    }
    fragColor = vec4(value / 255.0, 0.0, 0.0, 1.0);
}
