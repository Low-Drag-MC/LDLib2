#version 150

in vec2 screenPos;
in vec2 guiPos;
in vec2 uv;

out vec4 fragColor;

uniform vec4 SquareVertex;// left-top's vertex's x and y than right-buttom's vertex's x and y
// x = roundness top-right
// y = roundness boottom-right
// z = roundness top-left
// w = roundness bottom-left
uniform vec4 RoundRadius;
uniform vec2 ScreenSize;
uniform float Blur = 2.;
uniform float GuiScale;
uniform float progressHalfBlur = 0.005;
uniform vec4 Color1;
uniform vec4 Color2;
uniform float Progress;


//change from https://www.shadertoy.com/view/4llXD7
float sdRoundedBox(in vec2 p, in vec2 b, in vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x = (p.y > 0.0) ? r.x : r.y;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

vec4 run(vec2 uv, vec4 squareVertex, vec4 roundRadius){
    uv *= ScreenSize / 2.;

    uv.x += ScreenSize.x / 2. - squareVertex.x - (squareVertex.z - squareVertex.x) / 2. - 2.;
    uv.y -= ScreenSize.y / 2. - squareVertex.y - (squareVertex.w - squareVertex.y) / 2. - 2.;

    float dis = sdRoundedBox(uv, vec2((squareVertex.z - squareVertex.x) / 2., (squareVertex.w - squareVertex.y) / 2.), roundRadius);
    float percent = smoothstep(Blur, .0, dis);
    float vertex_percent = (screenPos.x - squareVertex.x) / (squareVertex.z - squareVertex.x);
    return percent * mix(Color1, Color2, smoothstep(Progress - progressHalfBlur, Progress + progressHalfBlur, vertex_percent));
}

void main() {
    fragColor = run(uv, SquareVertex * GuiScale, RoundRadius * GuiScale);
}
