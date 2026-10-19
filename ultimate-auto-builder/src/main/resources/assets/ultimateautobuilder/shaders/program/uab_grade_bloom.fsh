#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform sampler2D BloomSampler;
in vec2 texCoord;
in vec2 oneTexel;

uniform float Exposure;
uniform float Contrast;
uniform float Saturation;
uniform float Vibrance;
uniform float Gamma;
uniform float ShadowLift;
uniform float ShadowDepth;
uniform float Temperature;
uniform float TintR;
uniform float TintG;
uniform float TintB;
uniform float TintStrength;
uniform float Vignette;
uniform float Sharpen;
uniform float SkyEnabled;
uniform float SkyStrength;
uniform float ZenithR;
uniform float ZenithG;
uniform float ZenithB;
uniform float HorizonR;
uniform float HorizonG;
uniform float HorizonB;
uniform float Split;
uniform float BloomStrength;

out vec4 fragColor;

// Étalonnage Ultimate Auto Builder. Toutes les valeurs sont bornées côté Java (profil assaini).
float luma(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

void main() {
    vec3 src = texture(DiffuseSampler, texCoord).rgb;
    // Comparaison avant/après : partie gauche intacte.
    if (Split > 0.0 && texCoord.x < Split) {
        fragColor = vec4(src, 1.0);
        return;
    }
    vec3 c = src;
    // Netteté : masque flou à quatre voisins (coût : 4 lectures de texture).
    if (Sharpen > 0.001) {
        vec3 n = texture(DiffuseSampler, texCoord + vec2(oneTexel.x, 0.0)).rgb
               + texture(DiffuseSampler, texCoord - vec2(oneTexel.x, 0.0)).rgb
               + texture(DiffuseSampler, texCoord + vec2(0.0, oneTexel.y)).rgb
               + texture(DiffuseSampler, texCoord - vec2(0.0, oneTexel.y)).rgb;
        c = clamp(c + (c - n * 0.25) * Sharpen, 0.0, 1.0);
    }
    // Ciel : pixels sans géométrie (profondeur au plan lointain), dégradé horizon → zénith en conservant soleil et lune.
    if (SkyEnabled > 0.5) {
        float depth = texture(DepthSampler, texCoord).r;
        if (depth >= 0.99999) {
            vec3 grad = mix(vec3(HorizonR, HorizonG, HorizonB), vec3(ZenithR, ZenithG, ZenithB), smoothstep(0.15, 1.0, texCoord.y));
            float l = luma(c);
            vec3 tinted = grad * (0.55 + 0.9 * l);
            float keepBright = smoothstep(0.82, 0.95, l);
            c = mix(c, mix(tinted, c, keepBright), SkyStrength);
        }
    }
    c += texture(BloomSampler, texCoord).rgb * BloomStrength;
    c *= Exposure;
    float l = luma(c);
    // Éclaircissement des tons sombres (grottes, nuit) sans brûler les hautes lumières.
    c += ShadowLift * (1.0 - smoothstep(0.0, 0.55, l)) * (0.10 + 0.6 * c);
    // Intensité des ombres : approfondit les tons sombres.
    c *= mix(1.0, 0.45 + 0.55 * smoothstep(0.0, 0.4, l), ShadowDepth);
    c = pow(max(c, vec3(0.0)), vec3(1.0 / Gamma));
    c = (c - 0.5) * Contrast + 0.5;
    l = luma(c);
    float sat = max(max(c.r, c.g), c.b) - min(min(c.r, c.g), c.b);
    c = mix(vec3(l), c, Saturation * (1.0 + Vibrance * (1.0 - clamp(sat, 0.0, 1.0))));
    c *= vec3(1.0 + Temperature * 0.12, 1.0 + Temperature * 0.02, 1.0 - Temperature * 0.12);
    c = mix(c, c * vec3(TintR, TintG, TintB), TintStrength);
    vec2 d = texCoord - 0.5;
    c *= 1.0 - Vignette * smoothstep(0.30, 0.80, length(d) * 1.25);
    // Ligne de séparation de la comparaison.
    if (Split > 0.0 && abs(texCoord.x - Split) < oneTexel.x * 1.5) {
        c = vec3(1.0);
    }
    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
