#version 330

uniform sampler2D Sampler0;

in vec2 texCoord;
out vec4 fragColor;

// Pick mask factor from either luminance (grayscale PNG) or alpha (alpha-only PNG).
// Heuristic: opaque alpha implies the user painted the shape in RGB → use luma.
// Otherwise alpha encodes the shape → use alpha. Plus a final OR by alpha so
// soft AA edges always contribute. Covers:
//   - white shape on opaque PNG (RGB=1, A=1)  → luma = 1                    ✓
//   - black shape on opaque PNG (RGB=0, A=1)  → luma = 0 (hidden)           ✓
//   - alpha-encoded mask (RGB=0, A=mask)      → falls to A branch → A       ✓
//   - transparent area outside shape (A=0)    → A → 0                        ✓
//   - mixed grayscale + alpha (rare)          → A branch picks A             ✓
// ITU-R BT.601 luma coefficients.
void main() {
    vec4 m = texture(Sampler0, texCoord);
    float luma = dot(m.rgb, vec3(0.299, 0.587, 0.114));
    float factor = (m.a >= 0.999) ? luma : m.a;
    // Blend func (ZERO, ONE, ZERO, SRC_ALPHA): dst.rgb stays, dst.alpha *= src.alpha.
    fragColor = vec4(0.0, 0.0, 0.0, factor);
}
