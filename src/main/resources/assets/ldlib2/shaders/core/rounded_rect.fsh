#version 330

// Can't moj_import in things used during startup, when resource packs don't exist.
// This is a copy of dynamicimports.glsl
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec2 vLocalPos;
in vec4 vColor;
in vec2 vHalfSize;
in float vBorder;
in vec4 vRadius;  // tl, tr, br, bl

out vec4 fragColor;

float sdRoundedBox(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x  = (p.y > 0.0) ? r.x  : r.y;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

void main() {
    // Remap radius: vRadius = (tl, tr, br, bl) -> SDF wants (br, tr, bl, tl) for y-down
    vec4 rad = vec4(vRadius.z, vRadius.y, vRadius.w, vRadius.x);

    float d = sdRoundedBox(vLocalPos, vHalfSize, rad);
    float aa = fwidth(d) * 0.75;

    if (vBorder > 0.0) {
        vec2 innerHalf = max(vHalfSize - vec2(vBorder), vec2(0.0));
        vec4 innerRad = max(rad - vec4(vBorder), vec4(0.0));
        float dInner = sdRoundedBox(vLocalPos, innerHalf, innerRad);
        float aaInner = fwidth(dInner) * 0.75;

        float outerCov = 1.0 - smoothstep(-aa, aa, d);
        float innerCov = 1.0 - smoothstep(-aaInner, aaInner, dInner);
        float cov = clamp(outerCov - innerCov, 0.0, 1.0);
        fragColor = vec4(vColor.rgb, vColor.a * cov) * ColorModulator;
    } else {
        float cov = 1.0 - smoothstep(-aa, aa, d);
        fragColor = vec4(vColor.rgb, vColor.a * cov) * ColorModulator;
    }
}
