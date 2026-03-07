#version 330

// Can't moj_import in things used during startup, when resource packs don't exist.
// This is a copy of dynamicimports.glsl
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    //get the colour
    vec2 uv = texCoord0.xy;
    uv = uv * 0.5 + 0.25;
    uv = uv * 0.5 + 0.25;

    float v = min(texCoord0.y, 1.0 - texCoord0.y);
    v = pow(v / 0.3, 2.0) * 0.3;

    //main beam
    uv = (2.0 * uv) - 1.0;
    float beamWidth = abs(1.5 / (10.0 * uv.y)) * 1. / 4.;
    vec3 horBeam = vec3(beamWidth);

    fragColor = vec4((horBeam * vertexColor.rgb), length(horBeam) * v * vertexColor.a) * ColorModulator;
}
