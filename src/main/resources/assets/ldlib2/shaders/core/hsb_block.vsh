#version 330

// Can't moj_import in things used during startup, when resource packs don't exist.
// This is a copy of dynamicimports.glsl and projection.glsl
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec3 Position;
in vec4 HSB_ALPHA;

out vec4 hsb_alpha;

void main() {
//    gl_Position = vec4(Postion.x * 2 -1, -(Postion.y * 2 - 1), 1.0, 1.0);
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    hsb_alpha = vec4(HSB_ALPHA.r / 360.0,HSB_ALPHA.gba);
}
