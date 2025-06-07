#version 150

uniform vec2 iResolution;
uniform float iTime;

in vec4 vertexColor;
in vec2 texCoord;

out vec4 fragColor;

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
//    vec2 F = fragCoord.xy;
//    vec2 r = iResolution.xy, u = (F + F - r)/r.y/1.888;
//    fragColor.rgba = vec4(vec3(0.012,1.000,0.648), 0.);
//
//    float a = .004/(abs(length(u*u*u*u)-.04)+.005);
//    fragColor.a = a;
//    fragColor.rgb += .004/(abs(length(u*u*u*u)-.04)+.005) * (cos(vec3(1.))+1.);

    vec2 p1 = vec2(.01, .01);
    vec2 p2 = vec2(.99, .99);
    vec2 p3 = vec2(.01, .99);
    vec2 p4 = vec2(.99, .01);

    vec2 uv = fragCoord.xy / iResolution.xy;

    float d1 = step(p1.x,uv.x)*step(uv.x,p4.x)*abs(uv.y-p1.y)+
    step(uv.x,p1.x)*distance(uv,p1)+step(p4.x,uv.x)*distance(uv,p4);
    d1 = min(step(p3.x,uv.x)*step(uv.x,p2.x)*abs(uv.y-p2.y)+
    step(uv.x,p3.x)*distance(uv,p3)+step(p2.x,uv.x)*distance(uv,p2),d1);
    d1 = min(step(p1.y,uv.y)*step(uv.y,p3.y)*abs(uv.x-p1.x)+
    step(uv.y,p1.y)*distance(uv,p1)+step(p3.y,uv.y)*distance(uv,p3),d1);
    d1 = min(step(p4.y,uv.y)*step(uv.y,p2.y)*abs(uv.x-p2.x)+
    step(uv.y,p4.y)*distance(uv,p4)+step(p2.y,uv.y)*distance(uv,p2),d1);

    float f1 = .04 / (abs(d1 - 0.));

    // Time varying pixel color
    vec3 col = .5 + 0.5*cos(iTime+uv.xyx+vec3(4.000,0.040,0.078));

    fragColor = vec4(f1 * col, length(f1 * col));
}

void main() {
    mainImage(fragColor, vec2(texCoord.x * iResolution.x, texCoord.y * iResolution.y));
}