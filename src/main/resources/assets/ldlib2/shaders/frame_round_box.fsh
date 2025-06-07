#version 150

in vec2 screenPos;
in vec2 guiPos;
in vec2 uv;

out vec4 fragColor;

// left-top's vertex's x and y than right-buttom's vertex's x and y
uniform vec4 SquareVertex;
// x = roundness top-right
// y = roundness boottom-right
// z = roundness top-left
// w = roundness bottom-left
uniform vec4 RoundRadius1;
uniform vec4 RoundRadius2;
uniform float Thickness = 10.;
uniform vec2 ScreenSize;
uniform vec4 Color = vec4(1.0);
uniform float Blur = 2.;
uniform float GuiScale;

float sdRoundedBox(in vec2 p, in vec2 b, in vec4 r)
{
    r.xy = (p.x>0.0)? r.xy : r.zw;
    r.x  = (p.y>0.0)? r.x  : r.y;
    vec2 q = abs(p)-b+r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

float calculate(vec2 uv, vec4 squareVertex, vec4 roundRadius){
    uv.x += ScreenSize.x / 2. - squareVertex.x - (squareVertex.z - squareVertex.x) / 2. - 2.;
    uv.y -= ScreenSize.y / 2. - squareVertex.y - (squareVertex.w - squareVertex.y) / 2. - 2.;

    float dis = sdRoundedBox(uv, vec2((squareVertex.z - squareVertex.x) / 2., (squareVertex.w - squareVertex.y) / 2.), roundRadius);

    return dis;
}

vec4 run(vec2 uv, vec4 squareVertex) {

    float offset = Thickness;

    float dis1 = calculate(uv, squareVertex * GuiScale, RoundRadius1 * GuiScale);
    float dis2 = calculate(uv, (squareVertex - vec4(offset, offset, -offset, -offset)) * GuiScale, RoundRadius2 * GuiScale);

    float dis;
    if (offset > 0.){
        dis =  smoothstep(0., Blur, dis1);
        dis *= smoothstep(Blur, 0., dis2);
    } else {
        dis = smoothstep(Blur, 0., dis1);
        dis *= smoothstep(0., Blur, dis2);
    }

    return Color * dis;

}

void main(){
    fragColor = run(uv * ScreenSize/ 2., SquareVertex);
}