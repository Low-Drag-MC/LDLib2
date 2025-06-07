#version 150

#define SUPPORT_POSE_STACK

in vec3 Position;// base on normalized screen postion

out vec2 screenPos;// before modified by gui scale
out vec2 guiPos;// modified by gui scale
out vec2 uv;// normalized into [-1,1]

uniform float GuiScale;
uniform vec2 ScreenSize;

#ifdef SUPPORT_POSE_STACK
uniform mat4 PoseStack;
uniform mat4 ProjMat;
#endif

void main() {
    gl_Position = vec4(Position.xy, 0.0, 1.0);
    vec2 normalizedPos = gl_Position.xy * 0.5 + 0.5;
    screenPos = ScreenSize * vec2(normalizedPos.x, 1-normalizedPos.y);
    guiPos = screenPos / GuiScale;
    uv = gl_Position.xy;
    #ifdef SUPPORT_POSE_STACK
    gl_Position = vec4(((ProjMat) * PoseStack  * vec4(guiPos, 0.0, 1.0)).xy, 0.0, 1.0);
    #endif
}