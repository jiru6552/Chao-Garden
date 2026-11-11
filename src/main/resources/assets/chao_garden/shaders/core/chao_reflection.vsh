#version 150

#moj_import <fog.glsl>

in vec3 Position;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 TextureMat;
uniform int FogShape;

out float vertexDistance;
out vec2 sourceUv;
out vec2 reflectionUv;

void main() {
    vec4 viewPosition = ModelViewMat * vec4(Position, 1.0);
    // Keep the base entity shader's expression order for equal-depth testing.
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexDistance = fog_distance(Position, FogShape);
    sourceUv = UV0;
    // A reflection is one field over the surface, independent of its texture
    // atlas. Shared geometric points produce the same reflection coordinates
    // even when their body/egg faces use completely different atlas islands.
    // The small depth contribution gives the projection a three-dimensional
    // slant while keeping its coordinates continuous around the closed shell.
    vec2 surfaceProjection = viewPosition.xy * 0.25
        + viewPosition.z * vec2(0.06, 0.035);
    reflectionUv = (TextureMat * vec4(surfaceProjection, 0.0, 1.0)).xy;
}
