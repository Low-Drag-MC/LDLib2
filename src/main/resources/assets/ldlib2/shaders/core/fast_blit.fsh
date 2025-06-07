#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;

out vec4 fragColor;

//vec3 textureUV(vec2 uv) {
//    vec3 color = vec3(0.);
//    color = vec3(uv.x, uv.y, 1.);
//    return color;
//}

void main(){
    fragColor = texture(DiffuseSampler, texCoord);
}
