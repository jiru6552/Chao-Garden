#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform float GlintAlpha;

in float vertexDistance;
in vec2 sourceUv;
in vec2 reflectionUv;

out vec4 fragColor;

void main() {
    // The two sides of a zero-thickness wing can have different alpha masks.
    // Checking this face's original UV is necessary even with equal depth.
    if (texture(Sampler0, sourceUv).a < 0.1) discard;
    vec4 reflection = texture(Sampler1, reflectionUv) * ColorModulator;
    float fade = linear_fog_fade(vertexDistance, FogStart, FogEnd) * GlintAlpha;
    fragColor = vec4(reflection.rgb * fade, reflection.a);
}
