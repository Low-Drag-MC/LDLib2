#version 150

in vec3 Position;
in vec4 HSB_ALPHA;

out vec4 hsb_alpha;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

void main() {
//    gl_Position = vec4(Postion.x * 2 -1, -(Postion.y * 2 - 1), 1.0, 1.0);
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    hsb_alpha = vec4(HSB_ALPHA.r / 360.0,HSB_ALPHA.gba);
}
