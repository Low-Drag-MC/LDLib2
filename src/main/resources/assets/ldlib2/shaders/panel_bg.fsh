#version 150

in vec2 screenPos;
in vec2 guiPos;
in vec2 uv;

out vec4 fragColor;

uniform float Density = 10.0;
uniform float SquareSize = 0.1;
uniform vec4 BgColor = vec4(30) / 255.;
uniform vec4 SquareColor = vec4(40) / 255.;
uniform vec2 offset = vec2(0.0);
uniform vec2 ScreenSize;

vec4 run(vec2 uv) {
    uv.x *= ScreenSize.x / ScreenSize.y;

    uv += offset;

    uv *= Density;
    uv = fract(uv);

    vec4 fragColor;
    if (abs(uv.x) < SquareSize && abs(uv.y) < SquareSize) {
        fragColor = SquareColor;
    } else {
        fragColor = BgColor;
    }
    return fragColor;
}

void main() {
    fragColor = run(uv);
}