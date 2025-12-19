#version 150

in vec3 Position;
//in vec2 UV;

out vec2 texCoord;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

void main(){
    vec4 pos = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texCoord = pos.xy * 0.5 + 0.5;
    gl_Position = pos;
}
