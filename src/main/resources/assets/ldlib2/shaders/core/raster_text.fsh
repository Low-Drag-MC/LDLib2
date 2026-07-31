#version 330

// Can't moj_import in things used during startup, when resource packs don't exist.
// This is a copy of dynamicimports.glsl
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

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
    fragColor = color;
}
