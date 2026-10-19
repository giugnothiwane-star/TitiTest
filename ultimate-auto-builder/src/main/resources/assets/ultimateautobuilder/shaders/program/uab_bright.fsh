#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;

uniform float Threshold;

out vec4 fragColor;

// Extraction des zones lumineuses pour le halo (bloom).
void main() {
    vec3 c = texture(DiffuseSampler, texCoord).rgb;
    float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
    float k = max(l - Threshold, 0.0) / max(1.0 - Threshold, 0.001);
    fragColor = vec4(c * k, 1.0);
}
