#version 150

#moj_import <fog.glsl>

uniform sampler2D textureA;

uniform vec4 textureColor;
uniform vec4 textureColorHDR;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform float DiscardThreshold;

in float vertexDistance;
in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 color = texture(textureA, texCoord0) * vertexColor * textureColor;
    if (color.a < DiscardThreshold) {
        discard;
    }
    color = vec4(color.rgb + textureColorHDR.rgb * textureColorHDR.a, color.a);
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
