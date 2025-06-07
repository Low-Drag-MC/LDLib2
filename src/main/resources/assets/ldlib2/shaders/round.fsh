#version 150

in vec2 guiPos;
in vec2 uv;

out vec4 fragColor;

uniform float Radius;
uniform vec2 CenterPos;
uniform vec4 Color;
uniform float StepLength;
// all pos are modified by gui scale

void main() {
    fragColor = smoothstep(Radius + StepLength, Radius, length(guiPos - CenterPos)) * Color;
}