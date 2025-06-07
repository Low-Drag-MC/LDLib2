#version 150

in vec2 screenPos;
in vec2 guiPos;
in vec2 uv;

out vec4 fragColor;

uniform vec2 Point1;
uniform vec2 Point2;
uniform float Width;
uniform float Blur = 2.;
uniform vec4 Color1;
uniform vec4 Color2;

vec4 run() {

    vec2 vectorUV = guiPos - Point1;
    vec2 vectorPoint = Point2 - Point1;

    float vectorPointLength = length(vectorPoint);

    float axialDis = dot(vectorUV, vectorPoint) / dot(vectorPoint, vectorPoint);//normalized

    float radialDis = length(vectorPoint * axialDis - vectorUV);

    float dis = length(vectorUV - vectorPoint * clamp(axialDis, 0., 1.));
    float percent = smoothstep(Width + Blur, Width, dis);
    return vec4(1.0) * percent * mix(Color1, Color2, axialDis);
}

void main() {
    fragColor = run();
}