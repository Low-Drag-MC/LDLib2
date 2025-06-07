#version 150

in vec3 Position;
in vec2 UV;
in vec4 c;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vertexColor;
out vec2 texCoord;

void main(){
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexColor = c;
    texCoord = UV;
}