#version 150

in vec3 Position;
in vec2 UV;

out vec2 texCoord;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

void main(){
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texCoord = UV;
}
