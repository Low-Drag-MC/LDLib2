#version 150

in vec2 vLocalPos;
out vec4 fragColor;

uniform vec2 HalfSize;
uniform vec4 Radius;

uniform float Border;

uniform vec4 FillColor;
uniform vec4 BorderColor;

float pickCornerRadius(vec2 p, vec4 r){
    if (p.x >= 0.0 && p.y >= 0.0) return r.y;
    if (p.x >= 0.0 && p.y <  0.0) return r.z;
    if (p.x <  0.0 && p.y <  0.0) return r.w;
    return r.x;
}

float sdRRect(vec2 p, vec2 halfSize, vec4 r){
    float rc = pickCornerRadius(p, r);
    vec2 q = abs(p) - (halfSize - vec2(rc));
    return length(max(q, 0.0)) - rc;
}

void main(){
    vec2 p = vLocalPos;

    // --- 外壳 & 内壳距离 ---
    float dOuter = sdRRect(p, HalfSize, Radius);

    vec2 innerHalf = max(HalfSize - vec2(Border), 0.0);
    vec4 innerRad  = max(Radius   - vec4(Border), 0.0);
    float dInner = sdRRect(p, innerHalf, innerRad);

    // --- 各自的 AA 宽度（更稳） ---
    float aaO = max(fwidth(dOuter), 1e-3);
    float aaI = max(fwidth(dInner), 1e-3);

    // --- 覆盖度 ---
    float covOuter = 1.0 - smoothstep(0.0, aaO, dOuter); // 外轮廓以内
    float fillA    = 1.0 - smoothstep(0.0, aaI, dInner); // 内轮廓以内
    float borderA  = clamp(covOuter - fillA, 0.0, 1.0);  // 壳 = 外 − 内（避免重叠）

    // --- 预乘后合成（配合 ONE, ONE_MINUS_SRC_ALPHA）---
    vec4 fillPM   = vec4(FillColor.rgb   * FillColor.a,   FillColor.a);
    vec4 borderPM = vec4(BorderColor.rgb * BorderColor.a, BorderColor.a);

    fragColor = fillPM * fillA+ borderPM * borderA;
}